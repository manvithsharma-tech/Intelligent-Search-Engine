# Final Academic Evidence Manifest & Validation Report

**Project Title:** Intelligent Search Engine using Compressed Trie and Inverted Index  
**Department:** Department of Artificial Intelligence, Amrita School of Computing  
**Institution:** Amrita Vishwa Vidyapeetham, Coimbatore, India  
**Faculty Guide:** Dr. Ardra P S  
**Final Review Date:** 26 October 2026  
**Application URL:** [http://localhost:8080/](http://localhost:8080/)  
**Project Workspace:** `C:\Users\mannu\IdeaProjects\SearchEngineProject`  

---

## 1. Project Team Details
| Candidate Name | Roll Number | Degree & Specialization |
| :--- | :--- | :--- |
| **P. Manvith Sharma** | CB.AI.U4AID25141 | B.Tech in Artificial Intelligence |
| **K. Prasad** | CB.AI.U4AID25142 | B.Tech in Artificial Intelligence |
| **B. Sai Prasanna** | CB.AI.U4AID25163 | B.Tech in Artificial Intelligence |
| **A. Jhanavi** | CB.AI.U4AID25107 | B.Tech in Artificial Intelligence |

---

## 2. Executive Verification Summary
Prior to drafting the final academic deliverables, an exhaustive, multi-stage audit was executed on the active codebase, running server, dataset structure, regression test suites, and empirical benchmarks.

```
================================================================================
AUDIT SUMMARY MATRIX
================================================================================
Category                        Status         Evidence Details
--------------------------------------------------------------------------------
Production Java Code Compilation PASSED (100%)  Oracle JDK 24 (64-Bit); 0 errors, 0 warnings
Inverted Index Creation          PASSED (100%)  64,380 unique terms from 50,000 documents
Compressed Radix Trie            PASSED (100%)  O(L) sub-millisecond autocomplete (0.067–0.513 ms)
Coverage-Aware TF-IDF            PASSED (100%)  Eliminates partial-match single-term dominance
Okapi BM25 Probabilistic Model   PASSED (100%)  k1=1.2 term saturation, b=0.75 length norm
128-D LSA Semantic Search        PASSED (100%)  Truncated SVD concept space; cosine similarity
Three-Way Ranking Lab            PASSED (100%)  Side-by-side synchronized multi-algorithm eval
Research Workspace               PASSED (100%)  Persistent bookmarks, collections, notes, queries
Live Search Analytics            PASSED (100%)  Real-time query telemetry and browser RTT logging
Google Comparison Lab            PASSED (100%)  Precision@5 side-by-side comparative evaluation
System Heap Memory Profile       PASSED (100%)  132.03 MB in-use out of 4 GB heap cap
Regression & Audit Suites        13/13 PASSED   FullSystemAudit, TrieTest, Workspace, GoogleLab
================================================================================
```

---

## 3. Verified Corpus Inventory & Data Provenance
The evaluation corpus contains exactly **50,000 verified documents** spanning 11 topical domains, strictly validated from the filesystem:

```
================================================================================
CATEGORY DISTRIBUTION & FILE INVENTORY (VERIFIED FROM 50,000 CORPUS)
================================================================================
Category        Domain Type              Origin Datasets                  Documents    Share
--------------------------------------------------------------------------------
politics        News (Governance)        BBC + Misra News Category          27,631    55.26%
entertainment   News (Arts/Culture)      BBC + Misra News Category          11,107    22.21%
health          Health & Medicine        PubMed Central + Misra News         2,670     5.34%
sport           News (Athletics)         BBC + Misra News Category           1,901     3.80%
environment     Environment & Climate    OpenAlex Climatology + Misra        1,496     2.99%
business        News (Business/Econ)     BBC + Misra News Category           1,314     2.63%
education       Education & Pedagogy     OpenAlex Education + Misra            998     2.00%
tech            News (Consumer Tech)     BBC + Misra News Category             844     1.69%
science         Science & Research       OpenAlex Catalog + Misra              839     1.68%
ai              Technology & AI          OpenAlex AI & CS Corpus               600     1.20%
finance         Finance & Economics      OpenAlex Macroeconomics               600     1.20%
--------------------------------------------------------------------------------
TOTAL                                                                       50,000   100.00%
================================================================================
```

### Data Provenance & Permissive Licensing
1. **Misra News Category Dataset (44,175 articles):** Licensed under **Creative Commons Attribution 4.0 International (CC BY 4.0)**.
2. **OpenAlex Scholarly Works (3,000 articles):** Dedicated to the public domain under **Creative Commons CC0 1.0 Universal**.
3. **BBC News Corpus (2,225 articles):** Permitted for academic research and IR evaluation (Greene & Cunningham, ICML 2006).
4. **PubMed Central Open Access (600 articles):** **Public Domain** (U.S. Federal Government Work under 17 U.S.C. § 105).
5. **Deduplication:** 100% of corpus documents were hashed with SHA-256 over normalized lowercase alphanumeric tokens; 1,853 duplicate records were identified and rejected during staging.
6. **Substantial Content Filter:** 100% of files exceed 20 substantive words (median ~85 words; zero empty files or single-sentence stub headlines).

---

## 4. Test Suites Executed & Results
All tests were executed against the running application and in-memory data structures:

### A. Unit Test Suite: `trie.TrieTest`
- **Command:** `java -cp out/production/SearchEngineProject trie.TrieTest`
- **Result:** PASSED.
  - Exact search `'cricket'`: true
  - Exact search `'football'`: true
  - Exact search `'basketball'`: false (correctly omitted)
  - Prefix suggestions for `'foot'`: `foot`, `football`, `footballer`
  - Prefix suggestions for `'wor'`: `world`, `worldcup`

### B. Full System Integration Audit: `scratch.FullSystemAudit`
- **Command:** `java -cp out/production/SearchEngineProject scratch.FullSystemAudit`
- **Target:** `http://localhost:8080/`
- **Result:** 13 of 13 PASSED (0 Failed).
  1. System Stats & Semantic LSA Status: PASSED
  2. Compressed Trie Prefix Suggestions: PASSED
  3. Search Mode: Coverage-Aware TF-IDF: PASSED
  4. Search Mode: Okapi BM25: PASSED
  5. Search Mode: Baseline TF-IDF: PASSED
  6. Search Mode: Semantic Search (LSA 128-D): PASSED
  7. Multi-Domain Category Filtering: PASSED
  8. Full Document Viewing (`/api/document`): PASSED
  9. Ranking Comparison Lab & Analytics Deduplication: PASSED
  10. Search Quality Evaluation API (`/api/evaluate`): PASSED
  11. Research Workspace State & Persistence: PASSED
  12. Graceful Edge Case Handling: PASSED
  13. Semantic vs Keyword Side-by-Side Verification: PASSED

### C. Google Comparison Lab Suite: `scratch.VerifyGoogleComparisonLab`
- **Command:** `java -cp out/production/SearchEngineProject scratch.VerifyGoogleComparisonLab`
- **Result:** 4 of 4 PASSED (0 Failed).
  1. In-Memory & File Isolation: PASSED
  2. REST API `/api/google-eval` GET & POST: PASSED
  3. Validation & Edge Cases: PASSED
  4. System Regression: PASSED

### D. Research Workspace Suite: `scratch.TestResearchWorkspace`
- **Command:** `java -cp out/production/SearchEngineProject scratch.TestResearchWorkspace`
- **Result:** 14 of 14 PASSED (0 Failed).
  - Duplicate bookmark prevention, collection renaming, safe deletion cascading to default collection, personal notes editing, saved searches, and simulated server restart reload fidelity verified.

---

## 5. Measured Relevance & Performance Metrics
All performance metrics originate directly from `scratch/BenchmarkThreeAlgorithms.java` and `scratch/CalculateMetrics.java`:

### A. Relevance Benchmark across 10 Representative Multi-Domain Queries
```
================================================================================
RELEVANCE QUALITY BENCHMARK (10 Queries x 4 Ranking Models)
================================================================================
Ranking Model                       | Mean P@5     | Mean MRR   | Mean nDCG@5 
--------------------------------------------------------------------------------
Model A: Baseline TF-IDF            |      68.00% |     0.8000 |       0.7859
Model B: Coverage-Aware TF-IDF      |      88.00% |     0.9000 |       0.8936
Model C: Okapi BM25                 |      90.00% |     0.9250 |       0.9431
Model D: BM25 + Coverage Bonus      |      92.00% |     1.0000 |       1.0000
================================================================================
```

### B. Algorithmic Backend Latency vs Full Browser Round-Trip Time
```
================================================================================
QUERY LATENCY BENCHMARK (Measured on Oracle JDK 24 on Windows 11)
================================================================================
Query Type & Sample Query             Backend Execution (ms)  Browser RTT (ms)
--------------------------------------------------------------------------------
Trie Autocomplete ("quant")                  0.12 ms               4.5 ms
Focused Query ("quantum cosmology")          25.31 ms              68.0 ms
2-Term Query ("quantum cosmology")           83.00 ms             185.0 ms
Broad 4-Term Query ("heart failure...")     301.83 ms             380.0 ms
128-D LSA Semantic ("cardiac surgery")      117.00 ms             142.0 ms
================================================================================
```

### C. System Scaling & Memory Footprint
```
================================================================================
SCALING BENCHMARK (Baseline 7,825 vs Scaled 50,000 Documents)
================================================================================
Dimension                         Baseline (7,825 Docs)     Scaled Corpus (50,000 Docs)
--------------------------------------------------------------------------------
Total Documents                   7,825                     50,000 (+538.9%)
Inverted Index Vocabulary         48,537 unique terms       64,380 unique terms (+32.6%)
Total Tokens Processed            1,744,960 tokens          3,152,014 tokens (+80.6%)
Pipeline Initialization Time      4,769 ms (~4.77 s)        10,351 ms (~10.35 s)
Heap Memory In-Use                ~58 MB                    132.03 MB (out of 4 GB cap)
Trie Prefix Lookup Latency        0.036 – 0.109 ms          0.067 – 0.513 ms
================================================================================
```

---

## 6. Real Application Evidence Files Manifest

### A. Actual Application Screenshots (`deliverables/evidence/screenshots/`)
1. `01_main_search_interface.png` (354 KB): Main search UI with live stats cards, algorithm selector pills, category filter buttons, and search input.
2. `02_trie_autocomplete_dropdown.png` (318 KB): Compressed Trie prefix suggestion dropdown for `'crick'`.
3. `03_coverage_search_results.png` (334 KB): Coverage-Aware TF-IDF search results with coverage % badges, matched term pills, and snippets.
4. `04_search_quality_evaluation.png` (337 KB): Search quality evaluation modal with Precision, Recall, and F1 calculations.
5. `05_okapi_bm25_results.png` (331 KB): Okapi BM25 probabilistic ranking results.
6. `06_semantic_lsa_results.png` (367 KB): 128-dimensional LSA semantic search results for `'cardiac surgery'` showing Cosine Similarity, active latent topics, and explanations.
7. `07_three_way_comparison_lab.png` (454 KB): Three-Way Comparison Lab showing Coverage-Aware vs Okapi BM25 vs Baseline TF-IDF side by side on identical queries.
8. `08_research_workspace.png` (260 KB): Research Workspace showing saved bookmarks, collections, personal notes, and saved search queries.
9. `09_live_search_analytics.png` (265 KB): Live Search Analytics dashboard showing total search volume, latency charts, and recent query history.
10. `10_google_comparison_lab.png` (300 KB): Google Comparison Lab showing local engine results side by side with Google web results and manual evaluation scoring.
11. `11_system_architecture_modal.png` (196 KB): System Architecture modal displaying the 4-tier data structures and data flow.

### B. Publication-Quality Academic Charts (`deliverables/evidence/charts/`)
1. `01_relevance_benchmark_chart.png`: Mean Precision@5, Mean MRR, and Mean nDCG@5 across all 4 ranking models.
2. `02_corpus_category_distribution.png`: Document distribution across all 11 multi-domain categories (N = 50,000).
3. `03_latency_breakdown_chart.png`: Algorithmic backend execution latency vs browser round-trip time across diverse query types.
4. `04_system_scaling_benchmark.png`: System scaling comparison (7,825 vs 50,000 documents) covering documents, vocabulary, initialization time, and JVM heap memory.

---

## 7. Live Demonstration Plan & Review Protocol
Since `ffmpeg` is not pre-installed on the system PATH to record raw desktop video, this protocol outlines the exact sequence for the live review:

1. **Launch Backend Server:**
   ```powershell
   java -Xmx4g -cp out/production/SearchEngineProject server.SearchServer
   ```
   *Expected Output:* Loads 50,000 documents in ~5.4 s, builds inverted index (64,380 terms) in ~2.1 s, initializes Trie in ~2.8 s, loads 128-D LSA vectors in 115 ms. Total initialization: ~10.3 seconds.
2. **Access Web Application:**
   Open browser at `http://localhost:8080/`. Highlight the live stats cards: 50,000 Documents, 64,380 Unique Terms, 11 Domains, and Compressed Trie Active.
3. **Demonstrate Sub-Millisecond Trie Autocomplete:**
   Click search input and type `'crick'`. Observe instant dropdown suggestions (`'cricket'`, `'cricketer'`). Explain $O(L)$ edge compression.
4. **Demonstrate Keyword Search & Explainability:**
   Enter `'quantum cosmology'` under Coverage-Aware TF-IDF. Show coverage badge (100%), matched term pills (`'quantum'`, `'cosmology'`), execution latency, and highlighted snippets.
5. **Demonstrate the Single-Term Dominance Failure Mode in 3-Way Lab:**
   Click `⚡ 3-Way Comparison Lab`. Query `'heart failure clinical trial'`.
   Show panel members how Baseline TF-IDF erroneously ranks an IP legal trial article (`ext_tech_0248`) at Rank 1 due to word repetition, whereas Coverage-Aware TF-IDF and Okapi BM25 rank the genuine clinical cardiology trial (`health_0084`) at Rank 1.
6. **Demonstrate 128-D LSA Semantic Retrieval:**
   Switch to `🧠 Semantic Search (LSA)`. Query `'cardiac surgery'`.
   Demonstrate that the engine retrieves myocardial bypass surgery articles without requiring exact keyword overlap, displaying cosine similarity, active latent concepts, and semantic explanations.
7. **Demonstrate Research Workspace:**
   Bookmark results, assign them to `'General Research'`, write personal notes, and show persistent JSON storage.
8. **Demonstrate Live Analytics & Google Comparison Lab:**
   Open `Live Analytics` to inspect real-time query counters and latency telemetry. Open `Google Comparison Lab` to review side-by-side manual relevance scoring.

---

## 8. Defensible Scientific Boundaries & Known Limitations
1. **Topical Clustering Proxy:** Automated precision/recall metrics measure categorical topical coherence, not deep factual claim verification.
2. **Offline SVD Staging:** SVD decomposition is precomputed offline using randomized SVD. Incremental real-time SVD updating remains future scope.
3. **Google Comparison Scope:** Our local engine indexes an offline 50,000-document corpus. We do not claim to outperform Google's web-scale trillion-page index; the Google Lab is designed for controlled domain-specific evaluation.
4. **Single-Node Execution:** The engine runs on a single JVM process on a workstation; distributed postings sharding is reserved for future work.

---

## 9. Final Deliverables Inventory
All files are saved in the dedicated `deliverables/` folder:

- **Presentation:**
  - `deliverables/Final_Project_Presentation.pptx` (2.38 MB, 15 widescreen slides, custom theme, genuine screenshots, speaker notes)
  - `deliverables/Final_Project_Presentation.pdf` (1.89 MB, submission-ready PDF export)
- **Project Report:**
  - `deliverables/Final_Project_Report.docx` (2.95 MB, formal Amrita report structure, tables, equations, figures, references)
  - `deliverables/Final_Project_Report.pdf` (2.46 MB, professional Word COM PDF export)
- **Evidence Files:**
  - `deliverables/evidence/screenshots/` (11 genuine application screenshots)
  - `deliverables/evidence/charts/` (4 high-resolution 300 DPI evaluation charts)
  - `deliverables/evidence_and_validation_report.md` (This comprehensive evidence manifest)
