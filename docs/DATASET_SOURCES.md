# Dataset Provenance, Sources & Ingestion Specification

**Project Title:** Intelligent Search Engine using Compressed Trie and Inverted Index  
**Corpus Size:** 50,000 Unique Documents  
**Target Category Count:** 11 Topical Domains  
**Date of Expansion:** October 2026  

---

## 1. Overview of Primary Sources

The complete 50,000-document collection comprises four verified, legally permissive data sources. No documents were synthetically fabricated or artificially duplicated to reach the target volume.

```
====================================================================================================
PRIMARY DATA SOURCES INVENTORY
====================================================================================================
Source Dataset Name            Provider / Author        License         Permitted Uses     Corpus Docs
----------------------------------------------------------------------------------------------------
1. News Category Dataset (v3)  Rishabh Misra (arXiv)    CC BY 4.0       Academic & Comm.   44,175
2. BBC News Dataset            Greene & Cunningham      Academic Eval   Research & Study    2,225
3. OpenAlex Scholarly Works    OurResearch              CC0 1.0         Public Domain       3,000
4. PubMed Central Open Access  NLM / NIH                Public Domain   Open Access (Gov)     600
----------------------------------------------------------------------------------------------------
TOTAL VERIFIED DOCUMENTS                                                                    50,000
====================================================================================================
```

---

## 2. Source-by-Source Details

### Source 1: News Category Dataset (v3)

- **Dataset Name:** News Category Dataset
- **Version:** Version 3 (2012–2022 dataset archive)
- **Source Link:** https://www.kaggle.com/datasets/rmisra/news-category-dataset or [arXiv:2209.11429](https://arxiv.org/abs/2209.11429)
- **Curator:** Rishabh Misra (Machine Learning Research, Twitter / Kaggle)
- **Download / Ingestion Date:** October 2026
- **License:** **Creative Commons Attribution 4.0 International (CC BY 4.0)**
- **Permitted Uses:** Sharing, adaptation, transformation, commercial, and academic evaluation with appropriate credit and indication of modifications.
- **Citation & Attribution:**
  > Misra, Rishabh. "News Category Dataset." arXiv preprint arXiv:2209.11429 (2022).  
  > Misra, Rishabh and Jigyasa Grover. "Sculpting Data for ML: The first act of Machine Learning." (2021). ISBN: 978-0-578-83125-1.
- **Original Record Count:** 209,527 raw news records from HuffPost.
- **Imported Record Count:**
  - Initial baseline import: 2,000 articles
  - Scaled expansion batch: 42,175 articles
  - Total imported into 50k corpus: 44,175 articles
- **Category Mapping:**

| Raw Dataset Category | Mapped Engine Category | Justification |
| :--- | :--- | :--- |
| `POLITICS` | `politics` | Direct topical alignment with governance and elections |
| `ENTERTAINMENT` | `entertainment` | Direct alignment with culture, cinema, and arts |
| `HEALTHY LIVING`, `WELLNESS` | `health` | Direct alignment with medicine, disease, and wellness |
| `SPORTS` | `sport` | Direct alignment with athletics and competitions |
| `ENVIRONMENT` | `environment` | Direct alignment with climate and ecological sciences |
| `BUSINESS` | `business` | Direct alignment with commerce and markets |
| `EDUCATION` | `education` | Direct alignment with pedagogy, academia, and schools |
| `SCIENCE` | `science` | Direct alignment with physical and natural sciences |
| `TECH` | `tech` | Direct alignment with consumer tech and computing |
| *Other categories (e.g. `FOOD & DRINK`, `STYLE`, `PARENTING`, `TRAVEL`, `WEDDINGS`)* | *(Skipped)* | Omitted to prevent topic pollution outside the 11 engine domains |

---

### Source 2: BBC News Classification Dataset

- **Dataset Name:** BBC News Dataset
- **Version:** ICML 2006 Benchmark
- **Source Link:** http://mlg.ucd.ie/datasets/bbc.html
- **Curators:** Derek Greene and Pádraig Cunningham (University College Dublin)
- **License:** Permitted for academic research, education, and evaluation purposes.
- **Permitted Uses:** Educational demonstration, algorithmic benchmarking, information retrieval evaluation.
- **Citation & Attribution:**
  > D. Greene and P. Cunningham. "Practical Solutions to the Problem of Diagonal Dominance in Kernel Document Clustering", Proc. ICML 2006.
- **Original Record Count:** 2,225 articles.
- **Imported Record Count:** 2,225 articles across `business` (510), `entertainment` (386), `politics` (417), `sport` (511), and `tech` (401).
- **Category Mapping:** Direct 1-to-1 match with the 5 foundational news categories.

---

### Source 3: OpenAlex Scholarly Works Catalog

- **Dataset Name:** OpenAlex Scholarly Works
- **Provider:** OurResearch (Non-profit scholarly infrastructure)
- **Source Link:** https://openalex.org / API: `https://api.openalex.org/works`
- **Download / Ingestion Date:** October 2026
- **License:** **Creative Commons CC0 1.0 Universal (Public Domain Dedication)**
- **Permitted Uses:** Unrestricted worldwide use, commercial and non-commercial reproduction, modification, and integration without copyright barriers.
- **Citation & Attribution:**
  > Priem, J., Piwowar, H., & Orr, R. (2022). OpenAlex: A fully-open index of scholarly works, researchers, venues, institutions, and concepts. arXiv preprint arXiv:2205.01833.
- **Original Pool:** Over 250 million scholarly publications.
- **Imported Record Count:** 3,000 articles across 5 academic domains (600 each for `ai`, `science`, `education`, `environment`, and `finance`).
- **Category Mapping:** Sourced using OpenAlex primary concept IDs and sub-field filters directly matching target academic domains.

---

### Source 4: PubMed Central Open Access Subset

- **Dataset Name:** PubMed Central (PMC) Open Access Biomedical Literature
- **Provider:** National Library of Medicine (NLM) / National Institutes of Health (NIH)
- **Source Link:** https://eutils.ncbi.nlm.nih.gov/entrez/eutils/
- **Download / Ingestion Date:** October 2026
- **License:** **Public Domain** (U.S. Federal Government Work under 17 U.S.C. § 105) / PMC Open Access subset.
- **Permitted Uses:** Unrestricted reproduction, search indexing, and computational text analysis.
- **Citation & Attribution:**
  > National Center for Biotechnology Information (NCBI), U.S. National Library of Medicine (NLM), National Institutes of Health (NIH).
- **Imported Record Count:** 600 peer-reviewed medical and clinical abstracts into the `health` category.
- **Category Mapping:** Curated under clinical, cardiology, oncology, and therapeutic terms into the `health` category.

---

## 3. Deduplication Methodology & Pipeline Safeguards

To ensure that the 50,000 documents are genuinely unique and free of duplicate content or ID conflicts:

1. **Cryptographic SHA-256 Content Fingerprinting:**
   - Every candidate article underwent text normalization (lowercased, punctuation stripped, whitespace collapsed).
   - A SHA-256 cryptographic hash was computed over the normalized text.
   - Every hash was cross-referenced against a master hash set containing all existing live documents as well as all records already accepted in the current batch.
2. **Zero ID Collisions:**
   - Pre-existing files retained their IDs.
   - All newly imported documents were assigned globally unique, sequential IDs formatted as `exp_00001` through `exp_42175`.
   - Before merging, an automated integrity test verified that zero ID collisions exist in the filesystem.
3. **Substantive Length Filtering:**
   - Records with fewer than 20 words or consisting of single headline fragments were rejected.
4. **Isolated Raw Archive Processing:**
   - All raw data files (`.parquet`, `.csv`, `.jsonl`) were staged in `data_import/raw/` completely outside `dataset/` so `DocumentReader.java` could not accidentally parse binary archives.

---

## 4. Ingestion Reconciliation & Rejected Records Log

The expansion from 7,825 to 50,000 documents required importing **42,175** new unique documents from the raw pool of 209,527 records. The rejection breakdown was logged deterministically in `data_import/reports/import_log.csv` and `data_import/reports/import_summary.json`:

```
================================================================================
IMPORT & DEDUPLICATION RECONCILIATION SUMMARY
================================================================================
Metric / Stage                                                 Count
--------------------------------------------------------------------------------
Initial Baseline Corpus Count                                  7,825
Target Final Corpus Volume                                    50,000
Net Additions Required (50,000 - 7,825)                       42,175
Total Raw Source Records Inspected                           209,527
--------------------------------------------------------------------------------
REJECTED RECORDS:
  - Unmapped Category (topics outside the 11 engine domains): 15,348
  - Corpus Duplicate (exact match with existing 7,825 docs):   1,840
  - Too Short / Stub Record (< 20 substantive words):            858
  - Batch Duplicate (duplicate within incoming raw source):       13
  - Remainder not needed once 42,175 quota reached:          149,293
--------------------------------------------------------------------------------
ACCEPTED & STAGED UNIQUE RECORDS                              42,175
PRE-MERGE STAGING VALIDATION ERRORS                                0
POST-MERGE CORPUS READABLE UNIQUE DOCUMENTS                   50,000
================================================================================
```

---

## 5. Final Verified Corpus Counts

```
================================================================================
FINAL VERIFIED CORPUS COUNTS ACROSS ALL 11 DOMAINS
================================================================================
Category        Baseline (7,825)    Imported (42,175)    Final Count (50,000)
--------------------------------------------------------------------------------
politics                     817               26,814                  27,631
entertainment                786               10,321                  11,107
health                       600                2,070                   2,670
sport                        911                  990                   1,901
environment                  600                  896                   1,496
business                     910                  404                   1,314
education                    600                  398                     998
tech                         801                   43                     844
science                      600                  239                     839
ai                           600                    0                     600
finance                      600                    0                     600
--------------------------------------------------------------------------------
TOTAL                      7,825               42,175                  50,000
================================================================================
```

All 50,000 documents are verified readable, UTF-8 compliant, deduplicated, and fully indexed in memory by the production Java search engine.
