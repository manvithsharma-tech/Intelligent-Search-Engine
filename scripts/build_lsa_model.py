import os
import sys
import time
import json
import struct
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.decomposition import TruncatedSVD

def main():
    print("================================================================================")
    print("BUILDING LATENT SEMANTIC ANALYSIS (LSA) MODEL FOR 50,000-DOCUMENT CORPUS")
    print("================================================================================")

    start_time = time.time()
    dataset_dir = "dataset"
    out_dir = os.path.join("data", "semantic")
    os.makedirs(out_dir, exist_ok=True)

    # 1. Read all documents
    print("Step 1: Reading 50,000 documents from dataset/...")
    doc_ids = []
    doc_categories = []
    doc_texts = []

    # Sort categories to ensure determinism
    categories = sorted([d for d in os.listdir(dataset_dir) if os.path.isdir(os.path.join(dataset_dir, d))])
    for category in categories:
        cat_path = os.path.join(dataset_dir, category)
        files = sorted([f for f in os.listdir(cat_path) if f.lower().endswith(".txt")])
        for fname in files:
            doc_id = os.path.splitext(fname)[0]
            file_path = os.path.join(cat_path, fname)
            try:
                with open(file_path, "r", encoding="utf-8", errors="replace") as f:
                    content = f.read()
            except Exception:
                with open(file_path, "r", encoding="iso-8859-1", errors="replace") as f:
                    content = f.read()

            doc_ids.append(doc_id)
            doc_categories.append(category)
            doc_texts.append(content)

    num_docs = len(doc_ids)
    print(f"Loaded {num_docs} documents across {len(categories)} categories in {time.time() - start_time:.2f}s")

    # 2. Fit TF-IDF Vectorizer
    t0 = time.time()
    print("Step 2: Fitting TF-IDF Vectorizer (sublinear TF, stop words removed, max_features=25000)...")
    vectorizer = TfidfVectorizer(
        max_features=25000,
        min_df=3,
        max_df=0.6,
        sublinear_tf=True,
        stop_words="english",
        token_pattern=r"(?u)\b[a-z0-9]{2,}\b"
    )
    X = vectorizer.fit_transform(doc_texts)
    vocab = vectorizer.vocabulary_
    idfs = vectorizer.idf_
    feature_names = vectorizer.get_feature_names_out()
    vocab_size = len(feature_names)
    print(f"TF-IDF matrix computed: shape {X.shape}, non-zero entries: {X.nnz:,} in {time.time() - t0:.2f}s")

    # 3. Fit TruncatedSVD
    t0 = time.time()
    n_components = 128
    print(f"Step 3: Fitting TruncatedSVD with {n_components} latent semantic dimensions...")
    svd = TruncatedSVD(n_components=n_components, algorithm="randomized", n_iter=7, random_state=42)
    doc_vectors = svd.fit_transform(X) # Shape: (50000, 128)
    explained_variance = float(np.sum(svd.explained_variance_ratio_))
    print(f"SVD computed in {time.time() - t0:.2f}s. Total explained variance ratio: {explained_variance:.4f}")

    # 4. L2 Normalize document vectors
    print("Step 4: Normalizing document vectors to unit L2 norm for cosine similarity dot-products...")
    doc_norms = np.linalg.norm(doc_vectors, axis=1, keepdims=True)
    doc_norms[doc_norms == 0] = 1.0
    doc_vectors = doc_vectors / doc_norms
    doc_vectors = doc_vectors.astype(np.float32)

    # 5. Compute term concept coordinates: components_ is (128, vocab_size)
    # Term vector for term j in concept space is components_[:, j]
    term_vectors = svd.components_.T.astype(np.float32) # Shape: (vocab_size, 128)
    # L2 normalize term vectors
    term_norms = np.linalg.norm(term_vectors, axis=1, keepdims=True)
    term_norms[term_norms == 0] = 1.0
    term_vectors = term_vectors / term_norms
    term_vectors = term_vectors.astype(np.float32)

    # 6. Extract top descriptive terms per latent topic for explainability
    print("Step 5: Extracting topic keywords for all 128 latent semantic dimensions...")
    topics = {}
    components = svd.components_ # (128, vocab_size)
    for dim in range(n_components):
        row = components[dim]
        top_pos_indices = np.argsort(row)[::-1][:8]
        top_neg_indices = np.argsort(row)[:8]

        top_pos_words = [feature_names[i] for i in top_pos_indices if row[i] > 0]
        top_neg_words = [feature_names[i] for i in top_neg_indices if row[i] < 0]

        topics[str(dim)] = {
            "topPositive": top_pos_words,
            "topNegative": top_neg_words
        }

    # 7. Write binary vectors and JSON metadata
    print("Step 6: Writing binary vector files and metadata...")
    doc_vec_path = os.path.join(out_dir, "doc_vectors.bin")
    term_vec_path = os.path.join(out_dir, "term_vectors.bin")
    meta_path = os.path.join(out_dir, "semantic_meta.json")

    # Write Big-Endian float32 for seamless Java DataInputStream / ByteBuffer reading
    with open(doc_vec_path, "wb") as f:
        # doc_vectors is shape (50000, 128)
        doc_vectors.astype(">f4").tofile(f)

    with open(term_vec_path, "wb") as f:
        # term_vectors is shape (vocab_size, 128)
        term_vectors.astype(">f4").tofile(f)

    # Prepare vocabulary metadata (term -> {index, idf})
    vocab_meta = {}
    for term, idx in vocab.items():
        vocab_meta[term] = [int(idx), round(float(idfs[idx]), 5)]

    meta_obj = {
        "dimensions": n_components,
        "totalDocuments": num_docs,
        "totalVocabulary": vocab_size,
        "explainedVarianceRatio": round(explained_variance, 4),
        "docIds": doc_ids,
        "docCategories": doc_categories,
        "vocab": vocab_meta,
        "topics": topics
    }

    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta_obj, f)

    doc_file_size_mb = os.path.getsize(doc_vec_path) / (1024 * 1024)
    term_file_size_mb = os.path.getsize(term_vec_path) / (1024 * 1024)
    meta_file_size_mb = os.path.getsize(meta_path) / (1024 * 1024)

    total_time = time.time() - start_time
    print("================================================================================")
    print("LSA MODEL GENERATION COMPLETE!")
    print(f"Total processing time: {total_time:.2f} seconds")
    print(f"Output Directory:      {os.path.abspath(out_dir)}")
    print(f"Document Vectors:      {doc_vec_path} ({doc_file_size_mb:.2f} MB)")
    print(f"Term Vectors:          {term_vec_path} ({term_file_size_mb:.2f} MB)")
    print(f"Metadata & Topics:     {meta_path} ({meta_file_size_mb:.2f} MB)")
    print(f"Total Model Storage:   {doc_file_size_mb + term_file_size_mb + meta_file_size_mb:.2f} MB")
    print("================================================================================")

if __name__ == "__main__":
    main()
