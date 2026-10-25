package semantic;

import model.Document;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * High-performance, zero-dependency Latent Semantic Analysis (LSA) search engine.
 * Employs 128-dimensional truncated singular value decomposition (SVD) concept space
 * for genuine meaning-based retrieval across the 50,000-document multi-domain corpus.
 */
public class SemanticSearchEngine {

    private static final String DEFAULT_MODEL_DIR = "data/semantic";

    private final Map<String, Document> documentMap;
    private int dimensions = 128;
    private int totalDocuments = 0;
    private int totalVocabulary = 0;

    // Flattened dense float arrays for maximum CPU cache locality & SIMD auto-vectorization
    private float[] docVectors;   // shape: totalDocuments * dimensions
    private float[] termVectors;  // shape: totalVocabulary * dimensions

    private String[] docIds;
    private String[] docCategories;
    private Map<String, Integer> docIdToIndex;

    private Map<String, Integer> termToIndex;
    private float[] termIdfs;

    // Thematic concept explanations per latent dimension
    private Map<Integer, List<String>> topicPositiveWords;
    private Map<Integer, List<String>> topicNegativeWords;

    private boolean available = false;
    private String initError = null;

    public SemanticSearchEngine(List<Document> documents, Map<String, Document> documentMap) {
        this(documents, documentMap, DEFAULT_MODEL_DIR);
    }

    public SemanticSearchEngine(List<Document> documents, Map<String, Document> documentMap, String modelDir) {
        this.documentMap = documentMap != null ? documentMap : new HashMap<>();
        initModel(modelDir);
    }

    /**
     * Initializes the LSA model from precomputed dense binary matrices and metadata.
     */
    private void initModel(String modelDir) {
        long startTime = System.currentTimeMillis();
        Path dirPath = Paths.get(modelDir);

        Path metaPath = dirPath.resolve("semantic_meta.json");
        Path docVecPath = dirPath.resolve("doc_vectors.bin");
        Path termVecPath = dirPath.resolve("term_vectors.bin");

        if (!Files.exists(metaPath) || !Files.exists(docVecPath) || !Files.exists(termVecPath)) {
            this.available = false;
            this.initError = "Semantic model files missing in " + modelDir;
            System.err.println("[SemanticSearchEngine] Warning: " + initError);
            return;
        }

        try {
            // 1. Parse JSON metadata
            String metaJson = Files.readString(metaPath, StandardCharsets.UTF_8);
            parseMetadata(metaJson);

            // 2. Load Document Vectors (Big-Endian float32) via 64KB streaming buffer
            int expectedDocFloats = totalDocuments * dimensions;
            this.docVectors = loadDenseFloats(docVecPath, expectedDocFloats);

            // 3. Load Term Vectors (Big-Endian float32) via 64KB streaming buffer
            int expectedTermFloats = totalVocabulary * dimensions;
            this.termVectors = loadDenseFloats(termVecPath, expectedTermFloats);

            this.available = true;
            long loadDuration = System.currentTimeMillis() - startTime;
            System.out.println("[SemanticSearchEngine] Successfully loaded " + totalDocuments
                    + " document vectors (" + dimensions + "-D LSA) in " + loadDuration + " ms.");
        } catch (Exception e) {
            this.available = false;
            this.initError = e.getMessage();
            System.err.println("[SemanticSearchEngine] Initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Streams binary float vectors directly into flat float array using 64KB buffer,
     * avoiding multi-megabyte temporary byte[] allocations on constrained heaps.
     */
    private float[] loadDenseFloats(Path path, int expectedFloats) throws IOException {
        long fileSize = Files.size(path);
        if (fileSize != (long) expectedFloats * 4) {
            throw new IllegalStateException(path.getFileName() + " size mismatch: expected "
                    + ((long) expectedFloats * 4) + " bytes, found " + fileSize);
        }

        float[] floats = new float[expectedFloats];
        try (InputStream is = new BufferedInputStream(Files.newInputStream(path), 64 * 1024)) {
            byte[] chunk = new byte[64 * 1024];
            ByteBuffer bb = ByteBuffer.wrap(chunk).order(ByteOrder.BIG_ENDIAN);
            int floatIdx = 0;
            int bytesRead;
            while ((bytesRead = is.read(chunk)) != -1) {
                bb.position(0);
                bb.limit(bytesRead);
                int floatCount = bytesRead / 4;
                for (int i = 0; i < floatCount; i++) {
                    floats[floatIdx++] = bb.getFloat();
                }
            }
        }
        return floats;
    }

    /**
     * Fast, zero-dependency streaming parser for semantic_meta.json.
     */
    private void parseMetadata(String json) {
        this.docIdToIndex = new HashMap<>(55000);
        this.termToIndex = new HashMap<>(30000);
        this.topicPositiveWords = new HashMap<>(130);
        this.topicNegativeWords = new HashMap<>(130);

        // Extract scalar numbers
        this.dimensions = extractInt(json, "\"dimensions\"");
        this.totalDocuments = extractInt(json, "\"totalDocuments\"");
        this.totalVocabulary = extractInt(json, "\"totalVocabulary\"");

        if (this.dimensions <= 0) this.dimensions = 128;
        if (this.totalDocuments <= 0) this.totalDocuments = 50000;
        if (this.totalVocabulary <= 0) this.totalVocabulary = 25000;

        this.docIds = new String[totalDocuments];
        this.docCategories = new String[totalDocuments];
        this.termIdfs = new float[totalVocabulary];

        // Parse docIds array
        int docIdsStart = json.indexOf("\"docIds\":");
        if (docIdsStart != -1) {
            int arrStart = json.indexOf('[', docIdsStart);
            int arrEnd = json.indexOf(']', arrStart);
            if (arrStart != -1 && arrEnd != -1) {
                String arrContent = json.substring(arrStart + 1, arrEnd);
                int idx = 0;
                int p = 0;
                while (idx < totalDocuments && (p = arrContent.indexOf('"', p)) != -1) {
                    int nextQuote = arrContent.indexOf('"', p + 1);
                    if (nextQuote == -1) break;
                    String id = arrContent.substring(p + 1, nextQuote);
                    Document existingDoc = documentMap.get(id);
                    if (existingDoc != null) {
                        id = existingDoc.getId();
                    }
                    docIds[idx] = id;
                    docIdToIndex.put(id, idx);
                    idx++;
                    p = nextQuote + 1;
                }
            }
        }

        // Parse docCategories array
        int docCatsStart = json.indexOf("\"docCategories\":");
        if (docCatsStart != -1) {
            int arrStart = json.indexOf('[', docCatsStart);
            int arrEnd = json.indexOf(']', arrStart);
            if (arrStart != -1 && arrEnd != -1) {
                String arrContent = json.substring(arrStart + 1, arrEnd);
                int idx = 0;
                int p = 0;
                while (idx < totalDocuments && (p = arrContent.indexOf('"', p)) != -1) {
                    int nextQuote = arrContent.indexOf('"', p + 1);
                    if (nextQuote == -1) break;
                    docCategories[idx] = arrContent.substring(p + 1, nextQuote).intern();
                    idx++;
                    p = nextQuote + 1;
                }
            }
        }

        // Parse vocab mapping: "word": [index, idf]
        int vocabStart = json.indexOf("\"vocab\":");
        if (vocabStart != -1) {
            int objStart = json.indexOf('{', vocabStart);
            int topicsStart = json.lastIndexOf("\"topics\":");
            int objEnd = (topicsStart != -1) ? json.lastIndexOf('}', topicsStart) : -1;

            if (objStart != -1 && objEnd != -1 && objEnd > objStart) {
                String vocabBlock = json.substring(objStart + 1, objEnd);
                int p = 0;
                while ((p = vocabBlock.indexOf('"', p)) != -1) {
                    int quoteEnd = vocabBlock.indexOf('"', p + 1);
                    if (quoteEnd == -1) break;
                    String term = vocabBlock.substring(p + 1, quoteEnd);

                    int colon = vocabBlock.indexOf(':', quoteEnd);
                    int openBracket = vocabBlock.indexOf('[', colon);
                    int comma = vocabBlock.indexOf(',', openBracket);
                    int closeBracket = vocabBlock.indexOf(']', comma);

                    if (openBracket != -1 && comma != -1 && closeBracket != -1) {
                        try {
                            int tIdx = Integer.parseInt(vocabBlock.substring(openBracket + 1, comma).trim());
                            float idf = Float.parseFloat(vocabBlock.substring(comma + 1, closeBracket).trim());
                            termToIndex.put(term, tIdx);
                            if (tIdx < termIdfs.length) {
                                termIdfs[tIdx] = idf;
                            }
                        } catch (NumberFormatException ignored) {}
                        p = closeBracket + 1;
                    } else {
                        p = quoteEnd + 1;
                    }
                }
            }
        }

        // Parse topics metadata
        int topicsStart = json.lastIndexOf("\"topics\":");
        if (topicsStart != -1) {
            for (int dim = 0; dim < dimensions; dim++) {
                String dimKey = "\"" + dim + "\":";
                int dPos = json.indexOf(dimKey, topicsStart);
                if (dPos != -1) {
                    int dObjStart = json.indexOf('{', dPos);
                    int dObjEnd = json.indexOf('}', dObjStart);
                    if (dObjStart != -1 && dObjEnd != -1) {
                        String block = json.substring(dObjStart, dObjEnd);
                        topicPositiveWords.put(dim, parseStringList(block, "\"topPositive\":"));
                        topicNegativeWords.put(dim, parseStringList(block, "\"topNegative\":"));
                    }
                }
            }
        }
    }

    private static int extractInt(String json, String key) {
        int pos = json.indexOf(key);
        if (pos == -1) return -1;
        int colon = json.indexOf(':', pos);
        if (colon == -1) return -1;
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static List<String> parseStringList(String json, String key) {
        int pos = json.indexOf(key);
        if (pos == -1) return Collections.emptyList();
        int open = json.indexOf('[', pos);
        int close = json.indexOf(']', open);
        if (open == -1 || close == -1) return Collections.emptyList();
        String content = json.substring(open + 1, close);
        List<String> list = new ArrayList<>();
        int p = 0;
        while ((p = content.indexOf('"', p)) != -1) {
            int next = content.indexOf('"', p + 1);
            if (next == -1) break;
            list.add(content.substring(p + 1, next));
            p = next + 1;
        }
        return list;
    }

    public boolean isAvailable() {
        return available;
    }

    public int getDimensions() {
        return dimensions;
    }

    public int getTotalDocuments() {
        return totalDocuments;
    }

    public int getTotalVocabulary() {
        return totalVocabulary;
    }

    public String getInitError() {
        return initError;
    }

    /**
     * Executes genuine semantic retrieval using 128-D concept vector projection and
     * cosine similarity dot products across the 50,000-document collection.
     *
     * @param query Raw query string
     * @param categoryFilter Category filter ("all" or specific domain)
     * @param limit Maximum number of documents to return
     * @return Ranked list of SemanticResult instances
     */
    public List<SemanticResult> search(String query, String categoryFilter, int limit) {
        if (!available || query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String catFilter = (categoryFilter == null || categoryFilter.trim().isEmpty())
                ? "all" : categoryFilter.trim().toLowerCase();

        // 1. Tokenize query
        List<String> rawTokens = tokenize(query);
        if (rawTokens.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Identify recognized terms in vocabulary & build query vector
        List<String> recognizedWords = new ArrayList<>();
        float[] qVector = new float[dimensions];
        for (String token : rawTokens) {
            Integer termIdx = termToIndex.get(token);
            if (termIdx != null) {
                recognizedWords.add(token);
                float idf = (termIdx < termIdfs.length) ? termIdfs[termIdx] : 1.0f;
                int offset = termIdx * dimensions;
                for (int d = 0; d < dimensions; d++) {
                    qVector[d] += idf * termVectors[offset + d];
                }
            }
        }

        // If no query terms are recognized in vocabulary
        if (recognizedWords.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Normalize query vector to unit L2 norm
        float qNormSq = 0.0f;
        for (int d = 0; d < dimensions; d++) {
            qNormSq += qVector[d] * qVector[d];
        }
        if (qNormSq <= 0.000001f) {
            return Collections.emptyList();
        }
        float invQNorm = (float) (1.0 / Math.sqrt(qNormSq));
        for (int d = 0; d < dimensions; d++) {
            qVector[d] *= invQNorm;
        }

        // 4. Compute cosine similarity against candidate documents
        // Since both docVectors and qVector are unit-normalized, dot product == cosine similarity.
        List<ScoredDoc> candidates = new ArrayList<>();
        boolean filterAll = "all".equals(catFilter);

        for (int i = 0; i < totalDocuments; i++) {
            if (!filterAll && !docCategories[i].equalsIgnoreCase(catFilter)) {
                continue;
            }

            int offset = i * dimensions;
            float dotProduct = 0.0f;
            // Vectorized dot product
            for (int d = 0; d < dimensions; d++) {
                dotProduct += docVectors[offset + d] * qVector[d];
            }

            // Keep candidates with positive semantic alignment
            if (dotProduct > 0.001f) {
                candidates.add(new ScoredDoc(i, dotProduct));
            }
        }

        // 5. Sort candidates descending by score with deterministic tie-breaking on docId
        candidates.sort((a, b) -> {
            int scoreCmp = Float.compare(b.score, a.score);
            if (scoreCmp != 0) return scoreCmp;
            return docIds[a.docIndex].compareTo(docIds[b.docIndex]);
        });

        int returnCount = Math.min(limit, candidates.size());
        List<SemanticResult> results = new ArrayList<>(returnCount);

        // Precompute top semantic query terms coordinates for related-keyword alignment
        List<float[]> qTermVecs = new ArrayList<>();
        for (String w : recognizedWords) {
            Integer idx = termToIndex.get(w);
            if (idx != null) {
                float[] v = new float[dimensions];
                System.arraycopy(termVectors, idx * dimensions, v, 0, dimensions);
                qTermVecs.add(v);
            }
        }

        for (int rank = 0; rank < returnCount; rank++) {
            ScoredDoc sd = candidates.get(rank);
            int docIdx = sd.docIndex;
            String docId = docIds[docIdx];
            Document doc = documentMap.get(docId);
            if (doc == null) {
                doc = documentMap.get(docCategories[docIdx].toLowerCase() + ":" + docId);
            }
            if (doc == null) {
                doc = new Document(docId, docCategories[docIdx], docId + ".txt", "");
            }

            // 6. Generate Semantic Explainability
            // Find the 2 latent dimensions with largest positive coordinate product
            int docOffset = docIdx * dimensions;
            int bestDim1 = -1, bestDim2 = -1;
            float maxProd1 = -1.0f, maxProd2 = -1.0f;

            for (int d = 0; d < dimensions; d++) {
                float prod = qVector[d] * docVectors[docOffset + d];
                if (prod > maxProd1) {
                    maxProd2 = maxProd1;
                    bestDim2 = bestDim1;
                    maxProd1 = prod;
                    bestDim1 = d;
                } else if (prod > maxProd2) {
                    maxProd2 = prod;
                    bestDim2 = d;
                }
            }

            List<String> topConcepts = new ArrayList<>();
            if (bestDim1 != -1) {
                boolean positiveSide = qVector[bestDim1] >= 0;
                List<String> words = positiveSide ? topicPositiveWords.get(bestDim1) : topicNegativeWords.get(bestDim1);
                if (words != null && !words.isEmpty()) {
                    topConcepts.add(formatTopicWords(words, 4));
                }
            }
            if (bestDim2 != -1 && bestDim2 != bestDim1) {
                boolean positiveSide = qVector[bestDim2] >= 0;
                List<String> words = positiveSide ? topicPositiveWords.get(bestDim2) : topicNegativeWords.get(bestDim2);
                if (words != null && !words.isEmpty()) {
                    topConcepts.add(formatTopicWords(words, 4));
                }
            }

            // Find semantically related words present in the document
            List<String> relatedWords = findRelatedDocWords(doc.getContent(), qTermVecs, recognizedWords);

            // Construct explanation string
            String explanation = buildExplanation(doc, recognizedWords, relatedWords, topConcepts);

            // Generate semantic context snippet
            String snippet = generateSemanticSnippet(doc.getContent(), rawTokens, relatedWords);

            results.add(new SemanticResult(
                    rank + 1,
                    doc,
                    sd.score,
                    topConcepts,
                    relatedWords,
                    explanation,
                    snippet
            ));
        }

        return results;
    }

    /**
     * Finds words in the document that have strong semantic cosine similarity
     * to the query concept vector, even if they don't exactly match the query words.
     */
    private List<String> findRelatedDocWords(String docContent, List<float[]> qTermVecs, List<String> queryWords) {
        if (docContent == null || docContent.isEmpty() || qTermVecs.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> querySet = new HashSet<>(queryWords);
        List<String> docTokens = tokenize(docContent);
        Set<String> uniqueDocWords = new LinkedHashSet<>(docTokens);

        // Score words by cosine similarity to any query word vector
        Map<String, Float> wordScores = new HashMap<>();
        for (String w : uniqueDocWords) {
            if (querySet.contains(w)) continue; // Focus on words with DIFFERENT wording
            Integer idx = termToIndex.get(w);
            if (idx == null) continue;

            int offset = idx * dimensions;
            float maxSim = 0.0f;
            for (float[] qv : qTermVecs) {
                float sim = 0.0f;
                for (int d = 0; d < dimensions; d++) {
                    sim += termVectors[offset + d] * qv[d];
                }
                if (sim > maxSim) maxSim = sim;
            }

            if (maxSim >= 0.50f) { // Strong conceptual association threshold
                wordScores.put(w, maxSim);
            }
        }

        List<Map.Entry<String, Float>> sorted = new ArrayList<>(wordScores.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            result.add(sorted.get(i).getKey());
        }
        return result;
    }

    private String buildExplanation(Document doc,
                                    List<String> queryWords,
                                    List<String> relatedWords,
                                    List<String> topConcepts) {
        StringBuilder sb = new StringBuilder();
        sb.append("LSA Semantic match across 128-D concept space");
        if (!topConcepts.isEmpty()) {
            sb.append(" [Topic: ").append(topConcepts.get(0)).append("]");
        }
        if (!relatedWords.isEmpty()) {
            sb.append(" · Related vocabulary in article: ");
            for (int i = 0; i < relatedWords.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(relatedWords.get(i));
            }
        } else {
            sb.append(" · Global contextual alignment");
        }
        return sb.toString();
    }

    private String generateSemanticSnippet(String content, List<String> queryTokens, List<String> relatedWords) {
        if (content == null || content.isEmpty()) return "";

        String lower = content.toLowerCase();
        int bestPos = -1;

        // Try to find exact query token first
        for (String qt : queryTokens) {
            int pos = lower.indexOf(qt);
            if (pos != -1) {
                bestPos = pos;
                break;
            }
        }

        // If no query tokens present in document, find the first semantically related word!
        if (bestPos == -1 && relatedWords != null) {
            for (String rw : relatedWords) {
                int pos = lower.indexOf(rw);
                if (pos != -1) {
                    bestPos = pos;
                    break;
                }
            }
        }

        if (bestPos == -1) {
            return content.length() > 180 ? content.substring(0, 180) + "..." : content;
        }

        int start = Math.max(0, bestPos - 60);
        int end = Math.min(content.length(), bestPos + 120);

        String snippet = content.substring(start, end).replaceAll("\\s+", " ").trim();
        if (start > 0) snippet = "... " + snippet;
        if (end < content.length()) snippet = snippet + " ...";
        return snippet;
    }

    private static String formatTopicWords(List<String> words, int max) {
        if (words == null || words.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int count = Math.min(max, words.size());
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(", ");
            sb.append(words.get(i));
        }
        return sb.toString();
    }

    private static List<String> tokenize(String text) {
        if (text == null) return Collections.emptyList();
        String clean = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] parts = clean.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String p : parts) {
            if (p.length() >= 2) {
                tokens.add(p);
            }
        }
        return tokens;
    }

    /**
     * Helper to compute semantic score between query and a single document.
     */
    public double getScore(String query, Document document) {
        if (!available || query == null || document == null) return 0.0;
        Integer docIdx = docIdToIndex.get(document.getId());
        if (docIdx == null) return 0.0;

        List<String> rawTokens = tokenize(query);
        if (rawTokens.isEmpty()) return 0.0;

        float[] qVector = new float[dimensions];
        boolean hasToken = false;
        for (String t : rawTokens) {
            Integer termIdx = termToIndex.get(t);
            if (termIdx != null) {
                hasToken = true;
                float idf = (termIdx < termIdfs.length) ? termIdfs[termIdx] : 1.0f;
                int offset = termIdx * dimensions;
                for (int d = 0; d < dimensions; d++) {
                    qVector[d] += idf * termVectors[offset + d];
                }
            }
        }

        if (!hasToken) return 0.0;

        float qNormSq = 0.0f;
        for (int d = 0; d < dimensions; d++) {
            qNormSq += qVector[d] * qVector[d];
        }
        if (qNormSq <= 0.000001f) return 0.0;
        float invQNorm = (float) (1.0 / Math.sqrt(qNormSq));
        for (int d = 0; d < dimensions; d++) {
            qVector[d] *= invQNorm;
        }

        int docOffset = docIdx * dimensions;
        float dot = 0.0f;
        for (int d = 0; d < dimensions; d++) {
            dot += docVectors[docOffset + d] * qVector[d];
        }

        return Math.max(0.0, dot);
    }

    private static class ScoredDoc {
        final int docIndex;
        final float score;

        ScoredDoc(int docIndex, float score) {
            this.docIndex = docIndex;
            this.score = score;
        }
    }
}
