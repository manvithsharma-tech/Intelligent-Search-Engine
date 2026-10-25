# Intelligent Search Engine

> **B.Tech Capstone Project — Amrita Vishwa Vidyapeetham**  
> **Amrita School of Computing, Department of Artificial Intelligence**  
> *P. Manvith Sharma, K. Prasad, B. Sai Prasanna, A. Jhanavi*

---

## 🌟 Overview

The **Intelligent Search Engine** is a high-performance, zero-dependency information retrieval system engineered from the ground up in pure Java and modern web technologies. Operating across a multi-domain corpus of **50,000 documents** spanning 11 categories (AI, Business, Education, Entertainment, Environment, Finance, Health, Politics, Science, Sport, and Technology), the system combines classical lexical indexing with advanced semantic retrieval.

### Key Capabilities
- **Sub-millisecond Prefix Search**: Compressed Radix Trie enabling instantaneous auto-complete and vocabulary lookup.
- **Inverted Index with Positional Postings**: Efficient term-to-document mapping with term frequency and document frequency indices.
- **Tri-Engine Ranking Architecture**:
  - **Coverage-Aware TF-IDF**: Term Frequency–Inverse Document Frequency weighted by query coverage ratio.
  - **Okapi BM25**: Probabilistic relevance framework with query term saturation ($k_1 = 1.2$) and document length normalization ($b = 0.75$).
  - **128-Dimensional Latent Semantic Analysis (LSA)**: Truncated Singular Value Decomposition (SVD) concept space enabling true conceptual matching even when query terms and document vocabulary do not share literal keywords.
- **Google Comparison Lab**: Side-by-side comparative evaluation benchmarking local corpus retrieval against Google Search rankings with NDCG, Precision@K, and Rank Correlation metrics.
- **Research Workspace & Analytics**: Bookmark collections, saved queries, query history, and real-time query latency analytics.

---

## 🏗️ Architecture

```
                                  ┌───────────────────────────────┐
                                  │      Web Client (Frontend)    │
                                  │   HTML5 / Modern CSS / JS     │
                                  └──────────────┬────────────────┘
                                                 │ HTTP / REST
                                                 ▼
                                  ┌───────────────────────────────┐
                                  │   SearchServer (Java JDK)     │
                                  │ com.sun.net.httpserver.HttpServer│
                                  └──────┬───────────────┬────────┘
                                         │               │
                 ┌───────────────────────┴──────┐ ┌──────┴──────────────────────┐
                 │        Lexical Engine        │ │       Semantic Engine        │
                 ├──────────────────────────────┤ ├──────────────────────────────┤
                 │ • Compressed Radix Trie      │ │ • 128-D Latent Semantic SVD  │
                 │ • Inverted Index             │ │ • Cosine Similarity Vector   │
                 │ • Coverage-Aware TF-IDF      │ │ • Topic Word Explanations    │
                 │ • Okapi BM25                 │ │ • Dense Binary Matrices      │
                 └──────────────────────────────┘ └──────────────────────────────┘
```

---

## 📂 Repository Structure

```
.
├── Dockerfile                  # Multi-stage production container for Render / Cloud deployment
├── render.yaml                 # Render Blueprint configuration
├── README.md                   # Project documentation & execution guide
├── analytics.json              # Persistent search telemetry & query analytics
├── google_evals.json           # Historical Google comparison evaluation records
├── workspace.json              # User bookmark collections & saved searches
├── dataset/                    # 50,000 multi-domain text articles
│   ├── ai/
│   ├── business/
│   ├── education/
│   ├── entertainment/
│   ├── environment/
│   ├── finance/
│   ├── health/
│   ├── politics/
│   ├── science/
│   ├── sport/
│   └── tech/
├── data/
│   └── semantic/               # Precomputed 128-D LSA binary matrices
│       ├── doc_vectors.bin     # Document concept vectors (25.6 MB)
│       ├── term_vectors.bin    # Vocabulary concept vectors (12.8 MB)
│       └── semantic_meta.json  # Vocabulary mapping, IDFs, and thematic topics
├── deliverables/               # Capstone project report, slides, and evidence
│   ├── Final_Project_Report.pdf
│   ├── Final_Project_Presentation.pdf
│   └── evidence/
├── src/                        # Zero-dependency Java backend source code
│   ├── analytics/              # Analytics logging and metric aggregation
│   ├── comparison/             # Google Search comparison and evaluation
│   ├── evaluation/             # Precision@K, Recall, NDCG evaluators
│   ├── index/                  # Inverted Index implementation
│   ├── main/                   # Standalone console driver
│   ├── model/                  # Document and result models
│   ├── preprocessing/          # Tokenization, stopword filtering, stemming
│   ├── ranking/                # TF-IDF and Okapi BM25 scoring engines
│   ├── semantic/               # High-performance LSA vector retrieval
│   ├── server/                 # Lightweight HTTP REST API server
│   ├── trie/                   # Compressed Radix Trie data structure
│   └── workspace/              # Bookmark and collection manager
└── web/                        # Modern responsive web frontend
    ├── index.html
    ├── style.css
    └── app.js
```

---

## 🚀 Deployment on Render

This repository is pre-configured with a **multi-stage Dockerfile** and a `render.yaml` Blueprint for 1-click zero-downtime deployment on [Render](https://render.com).

### Automatic Deployment Steps:
1. Connect this GitHub repository to your [Render Dashboard](https://dashboard.render.com).
2. Click **New +** > **Web Service**.
3. Select this repository.
4. Render will automatically detect the `Dockerfile`.
   - **Environment**: `Docker`
   - **Plan**: `Free`
   - **Health Check Path**: `/api/stats`
5. The application dynamically binds to `$PORT` provided by Render.
6. Once deployed, the service will be live at:
   ```
   https://intelligent-search-engine.onrender.com
   ```

---

## 💻 Running Locally

### Prerequisites
- **Java Development Kit (JDK) 17+** (JDK 21 or 24 recommended)
- Zero external libraries or package managers required!

### 1. Compile
```bash
# Compile all Java sources
javac -encoding UTF-8 -sourcepath src -d out src/server/SearchServer.java
```

### 2. Run Server
```bash
# Launch server on default port 8080 (or specify a custom port: java -cp out server.SearchServer 9000)
java -cp out server.SearchServer
```

### 3. Open Web Client
Navigate to [http://localhost:8080](http://localhost:8080) in any web browser.

---

## 📊 REST API Reference

| Endpoint | Method | Parameters | Description |
|---|---|---|---|
| `/api/stats` | `GET` | — | Total indexed documents, vocabulary size, categories |
| `/api/suggest` | `GET` | `q=<prefix>` | Instant prefix autocomplete suggestions from Trie |
| `/api/search` | `GET` | `q=<query>&algo=<tfidf\|bm25\|semantic>&category=<cat>&limit=<n>` | Multi-algorithm search results with relevance scores |
| `/api/document` | `GET` | `id=<docId>` | Fetch full document content and metadata |
| `/api/evaluate` | `GET` | `q=<query>&algo=<algo>` | Precision, Recall, and NDCG benchmark calculations |
| `/api/analytics` | `GET` | — | Aggregated search latency, query distribution, and telemetry |
| `/api/workspace` | `GET/POST` | `action=<list\|bookmark\|collection>` | Manage bookmarks, folders, and saved searches |
| `/api/google-eval` | `GET/POST` | — | Google comparison lab records and ranking metrics |

---

## 👥 Authors
- **P. Manvith Sharma** (CB.AI.U4AID25141)
- **K. Prasad** (CB.AI.U4AID25142)
- **B. Sai Prasanna** (CB.AI.U4AID25163)
- **A. Jhanavi** (CB.AI.U4AID25107)

*Department of Artificial Intelligence, Amrita Vishwa Vidyapeetham, Coimbatore*
