import os
import sys
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE

def build_presentation():
    prs = Presentation()
    prs.slide_width = Inches(13.333)
    prs.slide_height = Inches(7.5)

    blank_layout = prs.slide_layouts[6] # Blank slide layout

    # Color Palette: Academic Deep Navy, Slate, Violet, Cyan, Pure White
    NAVY_DARK = RGBColor(10, 17, 40)       # #0A1128
    NAVY_HEADER = RGBColor(15, 23, 42)     # #0F172A
    NAVY_CARD = RGBColor(24, 33, 64)       # #182140
    CARD_BG = RGBColor(248, 250, 252)      # #F8FAFC
    CARD_BORDER = RGBColor(226, 232, 240)  # #E2E8F0
    TEXT_MAIN = RGBColor(15, 23, 42)       # #0F172A
    TEXT_MUTED = RGBColor(71, 85, 105)     # #475569
    TEXT_LIGHT = RGBColor(226, 232, 240)   # #E2E8F0
    VIOLET = RGBColor(124, 58, 237)        # #7C3AED
    VIOLET_LIGHT = RGBColor(167, 139, 250) # #A78BFA
    BLUE_ACCENT = RGBColor(37, 99, 235)    # #2563EB
    CYAN_ACCENT = RGBColor(6, 182, 212)    # #06B6D4
    EMERALD = RGBColor(16, 185, 129)       # #10B981
    WHITE = RGBColor(255, 255, 255)

    deliverables_dir = os.path.abspath("deliverables")
    screenshots_dir = os.path.join(deliverables_dir, "evidence", "screenshots")
    charts_dir = os.path.join(deliverables_dir, "evidence", "charts")

    def add_header(slide, title_text, category_text="B.TECH CAPSTONE PROJECT REVIEW"):
        # Header banner container
        header_box = slide.shapes.add_textbox(Inches(0.8), Inches(0.4), Inches(11.733), Inches(0.9))
        tf = header_box.text_frame
        tf.word_wrap = True
        tf.margin_left = tf.margin_top = tf.margin_right = tf.margin_bottom = 0

        p_cat = tf.paragraphs[0]
        p_cat.text = category_text.upper()
        p_cat.font.name = "Segoe UI"
        p_cat.font.size = Pt(10)
        p_cat.font.bold = True
        p_cat.font.color.rgb = VIOLET

        p_title = tf.add_paragraph()
        p_title.text = title_text
        p_title.font.name = "Segoe UI"
        p_title.font.size = Pt(22)
        p_title.font.bold = True
        p_title.font.color.rgb = NAVY_HEADER

    def add_card(slide, left, top, width, height, bg_color=CARD_BG, border_color=CARD_BORDER):
        shape = slide.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, left, top, width, height)
        shape.fill.solid()
        shape.fill.fore_color.rgb = bg_color
        shape.line.color.rgb = border_color
        shape.line.width = Pt(1.2)
        return shape

    # =========================================================================
    # SLIDE 1: Title & Team Slide (Dark Premium Theme)
    # =========================================================================
    slide1 = prs.slides.add_slide(blank_layout)
    bg1 = slide1.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, Inches(13.333), Inches(7.5))
    bg1.fill.solid()
    bg1.fill.fore_color.rgb = NAVY_DARK
    bg1.line.color.rgb = NAVY_DARK

    # Title Card
    tbox = slide1.shapes.add_textbox(Inches(1.0), Inches(0.8), Inches(11.333), Inches(2.2))
    tf1 = tbox.text_frame
    tf1.word_wrap = True

    p0 = tf1.paragraphs[0]
    p0.text = "AMRITA VISHWA VIDYAPEETHAM · DEPARTMENT OF ARTIFICIAL INTELLIGENCE"
    p0.font.name = "Segoe UI"
    p0.font.size = Pt(11)
    p0.font.bold = True
    p0.font.color.rgb = CYAN_ACCENT

    p1 = tf1.add_paragraph()
    p1.text = "Intelligent Search Engine using\nCompressed Trie and Inverted Index"
    p1.font.name = "Segoe UI"
    p1.font.size = Pt(32)
    p1.font.bold = True
    p1.font.color.rgb = WHITE

    p2 = tf1.add_paragraph()
    p2.text = "Multi-Domain Retrieval · Coverage-Aware TF-IDF · Okapi BM25 · 128-D LSA Semantic Search"
    p2.font.name = "Segoe UI"
    p2.font.size = Pt(14)
    p2.font.color.rgb = VIOLET_LIGHT

    # Team Card
    add_card(slide1, Inches(1.0), Inches(3.3), Inches(7.2), Inches(3.4), bg_color=NAVY_CARD, border_color=RGBColor(45, 55, 95))
    team_box = slide1.shapes.add_textbox(Inches(1.3), Inches(3.5), Inches(6.6), Inches(3.0))
    tt_f = team_box.text_frame
    tt_f.word_wrap = True

    tp0 = tt_f.paragraphs[0]
    tp0.text = "PROJECT TEAM (B.Tech AIE 2022–2026)"
    tp0.font.name = "Segoe UI"
    tp0.font.size = Pt(12)
    tp0.font.bold = True
    tp0.font.color.rgb = CYAN_ACCENT

    team_members = [
        ("P. Manvith Sharma", "CB.AI.U4AID25141"),
        ("K. Prasad", "CB.AI.U4AID25142"),
        ("B. Sai Prasanna", "CB.AI.U4AID25163"),
        ("A. Jhanavi", "CB.AI.U4AID25107"),
    ]
    for name, roll in team_members:
        p = tt_f.add_paragraph()
        p.text = f"•  {name}  —  {roll}"
        p.font.name = "Segoe UI"
        p.font.size = Pt(12.5)
        p.font.color.rgb = TEXT_LIGHT

    # Guide & Date Card
    add_card(slide1, Inches(8.5), Inches(3.3), Inches(3.833), Inches(3.4), bg_color=NAVY_CARD, border_color=RGBColor(45, 55, 95))
    guide_box = slide1.shapes.add_textbox(Inches(8.8), Inches(3.5), Inches(3.2), Inches(3.0))
    gt_f = guide_box.text_frame
    gt_f.word_wrap = True

    gp0 = gt_f.paragraphs[0]
    gp0.text = "FACULTY GUIDE"
    gp0.font.name = "Segoe UI"
    gp0.font.size = Pt(12)
    gp0.font.bold = True
    gp0.font.color.rgb = CYAN_ACCENT

    gp1 = gt_f.add_paragraph()
    gp1.text = "Dr. Ardra P S"
    gp1.font.name = "Segoe UI"
    gp1.font.size = Pt(16)
    gp1.font.bold = True
    gp1.font.color.rgb = WHITE

    gp2 = gt_f.add_paragraph()
    gp2.text = "Department of Artificial Intelligence\nAmrita School of Computing"
    gp2.font.name = "Segoe UI"
    gp2.font.size = Pt(11)
    gp2.font.color.rgb = TEXT_LIGHT

    gp3 = gt_f.add_paragraph()
    gp3.text = "\nFINAL REVIEW DATE"
    gp3.font.name = "Segoe UI"
    gp3.font.size = Pt(11)
    gp3.font.bold = True
    gp3.font.color.rgb = CYAN_ACCENT

    gp4 = gt_f.add_paragraph()
    gp4.text = "26 October 2026"
    gp4.font.name = "Segoe UI"
    gp4.font.size = Pt(13)
    gp4.font.bold = True
    gp4.font.color.rgb = WHITE

    slide1.notes_slide.notes_text_frame.text = (
        "Good morning respected guide Dr. Ardra P S and panel members. "
        "Today our team—Manvith Sharma, Prasad, Sai Prasanna, and Jhanavi—presents our B.Tech final capstone project: "
        "'Intelligent Search Engine using Compressed Trie and Inverted Index'. "
        "This project introduces a zero-dependency, high-performance information retrieval architecture combining an O(L) Radix Trie, "
        "inverted indexing with Coverage-Aware TF-IDF, Okapi BM25, and 128-dimensional Latent Semantic Analysis across a verified 50,000-document multi-domain corpus."
    )

    # =========================================================================
    # SLIDE 2: Abstract & Executive Overview
    # =========================================================================
    slide2 = prs.slides.add_slide(blank_layout)
    add_header(slide2, "Abstract & Executive Overview")

    # 3 Summary Cards
    cards_data = [
        ("The Challenge", 
         "Standard keyword retrieval models suffer from partial-match query inflation: a short document repeating a single rare term can outrank documents matching all search terms. Furthermore, disjoint IR architectures lack real-time sub-millisecond prefix completion, semantic synonym handling, and explainable ranking diagnostics.",
         NAVY_HEADER, VIOLET),
        ("Integrated Architecture", 
         "We engineer a unified, zero-dependency search engine integrating: (1) an edge-compressed Radix Trie for O(L) prefix autocomplete, (2) an Inverted Index with Coverage-Aware TF-IDF, (3) Okapi BM25 probabilistic ranking, and (4) a 128-dimensional LSA concept space precomputed via Truncated SVD across 50,000 documents.",
         NAVY_HEADER, BLUE_ACCENT),
        ("Empirical Findings", 
         "Verified benchmarks demonstrate: Coverage-Aware TF-IDF elevates Mean Precision@5 from 68.0% to 88.0% and nDCG@5 from 0.7859 to 0.8936, resolving single-term anomalies. Autocomplete traversals remain sub-millisecond (0.067–0.513 ms), while total engine heap footprint is only 132 MB.",
         NAVY_HEADER, EMERALD)
    ]

    for i, (title, body, hcolor, acolor) in enumerate(cards_data):
        x = Inches(0.8 + i * 3.97)
        add_card(slide2, x, Inches(1.5), Inches(3.75), Inches(5.3))
        tb = slide2.shapes.add_textbox(x + Inches(0.25), Inches(1.75), Inches(3.25), Inches(4.8))
        tf = tb.text_frame
        tf.word_wrap = True

        p_t = tf.paragraphs[0]
        p_t.text = title
        p_t.font.name = "Segoe UI"
        p_t.font.size = Pt(16)
        p_t.font.bold = True
        p_t.font.color.rgb = acolor

        p_b = tf.add_paragraph()
        p_b.text = f"\n{body}"
        p_b.font.name = "Segoe UI"
        p_b.font.size = Pt(12)
        p_b.font.color.rgb = TEXT_MAIN

    slide2.notes_slide.notes_text_frame.text = (
        "In this executive overview, we highlight the three pillars of our project. "
        "First, the core challenge: baseline vector-space models and TF-IDF lack term coverage awareness, producing severe ranking anomalies on multi-term queries. "
        "Second, our integrated solution: combining an edge-compressed Radix Trie, inverted indexing, Coverage-Aware TF-IDF, Okapi BM25, and 128-D LSA into a coherent engine. "
        "Third, verified empirical results: a 20% gain in Precision@5, near-instant prefix suggestions, and a compact 132 MB heap footprint."
    )

    # =========================================================================
    # SLIDE 3: Motivation & Problem Statement
    # =========================================================================
    slide3 = prs.slides.add_slide(blank_layout)
    add_header(slide3, "Motivation & Problem Statement")

    # Left Card: Problem Statement
    add_card(slide3, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_prob = slide3.shapes.add_textbox(Inches(1.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_p = tb_prob.text_frame
    tf_p.word_wrap = True

    p = tf_p.paragraphs[0]
    p.text = "Core Information Retrieval Bottlenecks"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = RGBColor(220, 38, 38) # Red accent

    points_p = [
        "1. The Single-Term Dominance Anomaly:\nIn standard TF-IDF (Score = Sum(TF * IDF)), documents repeating a single rare query term obtain artificially inflated scores, outranking documents matching 100% of query keywords.",
        "2. Disjoint Search Pipelines:\nTraditional search architectures separate prefix autocomplete, keyword indexing, and semantic vectors into fragmented external microservices, creating high latency and operational complexity.",
        "3. Synonymy and Vocabulary Mismatch:\nKeyword inverted indexes fail completely when users query synonymous terms (e.g., 'cardiac surgery' vs 'myocardial infarction' or 'heart disease').",
        "4. The 'Black-Box' Ranking Problem:\nModern search engines return ranked links with zero mathematical justification for why document A outranked document B."
    ]
    for pt in points_p:
        para = tf_p.add_paragraph()
        para.text = f"\n{pt}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(11)
        para.font.color.rgb = TEXT_MAIN

    # Right Card: Real Anomaly Proof
    add_card(slide3, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_anom = slide3.shapes.add_textbox(Inches(7.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_a = tb_anom.text_frame
    tf_a.word_wrap = True

    p = tf_a.paragraphs[0]
    p.text = "Concrete Empirical Evidence: The Baseline Failure"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = VIOLET

    p_case = tf_a.add_paragraph()
    p_case.text = "\nQuery Tested: \"heart failure clinical trial\" (4 valid terms)"
    p_case.font.name = "Segoe UI"
    p_case.font.size = Pt(12)
    p_case.font.bold = True
    p_case.font.color.rgb = NAVY_HEADER

    anom_desc = (
        "• Baseline TF-IDF Rank #1: ext_tech_0248\n"
        "  - Matches: 1 of 4 terms ('trial' repeated multiple times)\n"
        "  - Topic: Consumer electronics legal dispute\n"
        "  - Result: Irrelevant false positive ranked at top position!\n\n"
        "• Actual Medical Trial Article: health_0084\n"
        "  - Matches: 4 of 4 terms ('heart', 'failure', 'clinical', 'trial')\n"
        "  - Topic: Comprehensive Cardiology Phase-III Study\n"
        "  - Demoted under baseline TF-IDF due to length normalization lack!\n\n"
        "• Goal of this Project:\n"
        "  Eliminate this fundamental IR defect through rigorous Coverage-Aware penalty scaling, Okapi BM25 saturation, and 128-D LSA semantic concept projection."
    )
    para = tf_a.add_paragraph()
    para.text = anom_desc
    para.font.name = "Segoe UI"
    para.font.size = Pt(11)
    para.font.color.rgb = TEXT_MAIN

    slide3.notes_slide.notes_text_frame.text = (
        "To clearly articulate why this project matters, look at this concrete real-world failure we discovered in the baseline dataset. "
        "When querying 'heart failure clinical trial', standard TF-IDF placed a consumer technology article about a legal trial at Rank 1, "
        "because the single word 'trial' appeared densely. A genuine medical cardiology paper matching all four terms was pushed down. "
        "This motivated our research into Coverage-Aware scaling and multi-model ranking."
    )

    # =========================================================================
    # SLIDE 4: Project Objectives
    # =========================================================================
    slide4 = prs.slides.add_slide(blank_layout)
    add_header(slide4, "Project Objectives")

    objectives = [
        ("1. O(L) Radix Trie Autocomplete", "Design and construct an in-memory Compressed Trie that collapses non-branching character paths into edge substrings, achieving sub-millisecond prefix suggestion lookup independent of corpus volume N."),
        ("2. Coverage-Aware TF-IDF Engine", "Develop a non-linear term coverage penalty function that scales raw TF-IDF scores by (matched/valid)^1.5, penalizing single-term spurious matches and rewarding multi-term semantic completeness."),
        ("3. Probabilistic BM25 & Ranking Lab", "Implement the Okapi BM25 probabilistic model with term saturation (k1=1.2) and document length normalization (b=0.75), alongside an interactive Three-Way Comparison Lab."),
        ("4. 128-D LSA Semantic Search", "Integrate a dense 128-dimensional concept vector space via Truncated Singular Value Decomposition (SVD), enabling genuine meaning-based retrieval across synonymous vocabulary."),
        ("5. Explainable Ranking Diagnostics", "Decompose and display scoring factors (term coverage %, matched term pills, BM25 saturation, active latent topics) to provide transparent user interpretability."),
        ("6. Research Workspace & Telemetry", "Provide persistent user research tools (bookmarks, hierarchical collections, personal notes, saved queries) with live telemetry and Google Comparison Lab evaluation.")
    ]

    for i, (title, desc) in enumerate(objectives):
        col = i % 2
        row = i // 2
        x = Inches(0.8 + col * 5.95)
        y = Inches(1.5 + row * 1.8)

        add_card(slide4, x, y, Inches(5.75), Inches(1.65))
        tb = slide4.shapes.add_textbox(x + Inches(0.2), y + Inches(0.15), Inches(5.35), Inches(1.35))
        tf = tb.text_frame
        tf.word_wrap = True

        p_t = tf.paragraphs[0]
        p_t.text = title
        p_t.font.name = "Segoe UI"
        p_t.font.size = Pt(13)
        p_t.font.bold = True
        p_t.font.color.rgb = VIOLET

        p_d = tf.add_paragraph()
        p_d.text = desc
        p_d.font.name = "Segoe UI"
        p_d.font.size = Pt(10.5)
        p_d.font.color.rgb = TEXT_MAIN

    slide4.notes_slide.notes_text_frame.text = (
        "We formalized six concrete, measurable project objectives covering data structures, algorithm design, semantic analysis, explainability, and full-stack usability. "
        "Every single objective listed here has been implemented, validated through automated regression tests, and deployed into the running application."
    )

    # =========================================================================
    # SLIDE 5: Literature Survey & Comparative Analysis
    # =========================================================================
    slide5 = prs.slides.add_slide(blank_layout)
    add_header(slide5, "Literature Survey & Comparative Analysis")

    # Table of verified literature
    table_shape = slide5.shapes.add_table(6, 5, Inches(0.8), Inches(1.4), Inches(11.733), Inches(5.5))
    table = table_shape.table
    table.columns[0].width = Inches(2.2) # Model / Citation
    table.columns[1].width = Inches(2.2) # Core Approach
    table.columns[2].width = Inches(2.4) # Strengths
    table.columns[3].width = Inches(2.5) # Limitations
    table.columns[4].width = Inches(2.433) # Relevance to Project

    headers = ["Author & Reference", "Core Methodology", "Demonstrated Strengths", "Known Limitations", "Relevance to this Work"]
    for j, h in enumerate(headers):
        cell = table.cell(0, j)
        cell.fill.solid()
        cell.fill.fore_color.rgb = NAVY_HEADER
        p = cell.text_frame.paragraphs[0]
        p.text = h
        p.font.name = "Segoe UI"
        p.font.size = Pt(10.5)
        p.font.bold = True
        p.font.color.rgb = WHITE

    rows_data = [
        ("Salton & Buckley (1988)\nInf. Process. & Manage.\nDOI: 10.1016/0306-4573(88)90021-0",
         "Vector Space Model with TF-IDF term weighting.",
         "Efficient postings intersection; term discrimination via inverse document frequency.",
         "Vulnerable to length distortion and single-term dominance without coverage awareness.",
         "Forms the baseline ranking algorithm; modified with our coverage exponent penalty."),
        ("Robertson et al. (1994 / 2009)\nTREC-3 / FnTIR\nDOI: 10.1561/1500000019",
         "Okapi BM25 probabilistic 2-Poisson relevance model.",
         "Term frequency saturation (k1=1.2) and document length normalization (b=0.75).",
         "Heuristic parameter tuning; ignores latent conceptual and synonymous relationships.",
         "Serves as the high-precision probabilistic benchmark in our 3-Way Comparison Lab."),
        ("Deerwester et al. (1990)\nJASIS\nDOI: 10.1002/(SICI)1097-4571",
         "Latent Semantic Analysis via Truncated SVD.",
         "Uncovers latent concept associations; overcomes synonymy and polysemy.",
         "High SVD computation cost O(N·V·k); cannot serve real-time inverted lookups natively.",
         "Implemented as a 128-D dense concept space for semantic search and explainability."),
        ("Fredkin (1960) / Morrison (1968)\nCACM / JACM\nDOI: 10.1145/321479.321481",
         "Standard Trie vs Radix Tree (Edge Compression).",
         "Radix Tree collapses single-child paths into edge substrings; O(L) prefix lookup.",
         "High pointer memory overhead in standard tries if edges are uncompressed.",
         "Engineered as our in-memory Compressed Trie for sub-millisecond autocomplete."),
        ("Proposed Architecture\n(Sharma, Prasad, Prasanna,\nJhanavi, 2026)",
         "Integrated Radix Trie + Inverted Index + Coverage-Aware TF-IDF + BM25 + 128-D LSA.",
         "Combines exact keyword precision, probabilistic robustness, and semantic synonymy with explainability.",
         "Requires offline SVD staging; evaluated against multi-domain category proxy.",
         "End-to-end zero-dependency Information Retrieval architecture verified on 50,000 docs.")
    ]

    for i, row in enumerate(rows_data):
        for j, val in enumerate(row):
            cell = table.cell(i + 1, j)
            cell.fill.solid()
            cell.fill.fore_color.rgb = CARD_BG if i % 2 == 0 else WHITE
            p = cell.text_frame.paragraphs[0]
            p.text = val
            p.font.name = "Segoe UI"
            p.font.size = Pt(9.5)
            p.font.color.rgb = TEXT_MAIN

    slide5.notes_slide.notes_text_frame.text = (
        "Our literature survey spans the foundational pillars of Information Retrieval: "
        "Salton and Buckley's Vector Space Model, Robertson's Okapi BM25, Deerwester's Latent Semantic Analysis, and Morrison's PATRICIA Radix Trie. "
        "All citations have verified DOIs and venues. Notice how our architecture integrates the strengths of each model while addressing their documented limitations."
    )

    # =========================================================================
    # SLIDE 6: Research Gap & Defensible Novelty
    # =========================================================================
    slide6 = prs.slides.add_slide(blank_layout)
    add_header(slide6, "Research Gap & Defensible Novelty")

    # Left: Research Gap
    add_card(slide6, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_g = slide6.shapes.add_textbox(Inches(1.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_g = tb_g.text_frame
    tf_g.word_wrap = True

    p = tf_g.paragraphs[0]
    p.text = "Identified Research Gap"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = NAVY_HEADER

    gaps = [
        "1. Disconnected Educational & Production Paradigms:\nAcademic IR projects frequently present inverted indexes or vector space models as disjoint console scripts. Students and researchers lack unified testbeds that juxtapose classical keyword, probabilistic, and latent semantic models side-by-side on identical queries.",
        "2. Absence of Query-Coverage Penalties:\nStandard TF-IDF textbooks omit term-coverage constraints, leading to false positives where 1 of 4 search terms dominates the score due to raw frequency.",
        "3. Opacity in Retrieval Scoring:\nExisting search frameworks return opaque float scores without indicating which terms matched, what fraction of the query was covered, or which latent concepts contributed to relevance.",
        "4. Heavy Framework Bloat:\nMost semantic IR engines depend on heavy Python/PyTorch microservices, preventing lightweight, zero-dependency deployment."
    ]
    for g in gaps:
        para = tf_g.add_paragraph()
        para.text = f"\n{g}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(11)
        para.font.color.rgb = TEXT_MAIN

    # Right: Defensible Novelty
    add_card(slide6, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_n = slide6.shapes.add_textbox(Inches(7.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_n = tb_n.text_frame
    tf_n.word_wrap = True

    p = tf_n.paragraphs[0]
    p.text = "Defensible Scientific & Engineering Novelty"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = VIOLET

    novelties = [
        "We do NOT claim to have invented inverted indexes, BM25, or SVD. Rather, our defensible contribution lies in the rigorous integration and empirical validation of:",
        "• Unified Dual-Engine Hybrid Paradigm:\nSimultaneous co-existence of an exact inverted index and a 128-dimensional LSA concept space running in pure Java with zero external runtime dependencies.",
        "• Coverage-Aware Penalty Formulation:\nA simple yet mathematically sound non-linear term coverage multiplier that lifts Mean Precision@5 by +20.0% and prevents rare-word inflation.",
        "• Interactive 3-Way Comparative Ranking Lab:\nSide-by-side synchronized evaluation of Coverage-Aware, BM25, and Baseline models with transparent explainability metrics.",
        "• Integrated Research Workspace & Telemetry:\nPersistent client-server bookmarks, collections, notes, and manual Google evaluation suite operating across 50,000 verified documents."
    ]
    for n in novelties:
        para = tf_n.add_paragraph()
        para.text = f"\n{n}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(11)
        para.font.color.rgb = TEXT_MAIN

    slide6.notes_slide.notes_text_frame.text = (
        "We explicitly define our novelty in narrow, scientifically defensible terms. "
        "We do not claim to have invented BM25 or SVD. "
        "Our contribution is the architectural integration, the Coverage-Aware TF-IDF formulation that eliminates partial-match inflation, "
        "and the zero-dependency dual-engine runtime in pure Java with explainable diagnostics."
    )

    # =========================================================================
    # SLIDE 7: System Architecture & Data Flow
    # =========================================================================
    slide7 = prs.slides.add_slide(blank_layout)
    add_header(slide7, "System Architecture & End-to-End Data Flow")

    # Diagram Cards representing the 4 Tiers
    tiers = [
        ("Tier 1: Data Ingestion & Offline Modeling", 
         "• 50,000 Multi-Domain Corpus (11 Categories)\n• SHA-256 Deduplication & Token Ingestion\n• Offline Truncated SVD (128 Dimensions)\n• Dense Binary Matrices: doc_vectors.bin (25.6 MB), term_vectors.bin (12.8 MB)",
         NAVY_HEADER, VIOLET),
        ("Tier 2: In-Memory Indexing & Core Data Structures", 
         "• Compressed Radix Trie: Edge substring compression; O(L) prefix lookup\n• Inverted Index: Term-to-Document Posting Lists (64,380 unique terms)\n• LSA Vector Engine: Dense SIMD float arrays; 128-D dot product similarity\n• Ranking Engine: Coverage-Aware TF-IDF & Okapi BM25",
         NAVY_HEADER, BLUE_ACCENT),
        ("Tier 3: Zero-Dependency Server & REST APIs", 
         "• JDK com.sun.net.httpserver (Port 8080)\n• REST Endpoints: /api/suggest, /api/search, /api/document, /api/evaluate\n• WorkspaceManager: Persistent JSON bookmarks, collections, notes\n• AnalyticsManager: Real-time query telemetry & browser RTT logging",
         NAVY_HEADER, CYAN_ACCENT),
        ("Tier 4: Responsive Modern Web Presentation", 
         "• Single-Page Web Application (Vanilla JS, CSS3, Semantic HTML5)\n• Live Compressed Trie Autocomplete Dropdown\n• Explainable Result Cards (Coverage pills, active latent concepts)\n• 3-Way Comparison Lab, Analytics Dashboard, Google Evaluation Suite",
         NAVY_HEADER, EMERALD)
    ]

    for i, (title, details, hcolor, acolor) in enumerate(tiers):
        y = Inches(1.4 + i * 1.4)
        add_card(slide7, Inches(0.8), y, Inches(11.733), Inches(1.25))

        tb = slide7.shapes.add_textbox(Inches(1.0), y + Inches(0.1), Inches(11.333), Inches(1.05))
        tf = tb.text_frame
        tf.word_wrap = True

        p_t = tf.paragraphs[0]
        p_t.text = title
        p_t.font.name = "Segoe UI"
        p_t.font.size = Pt(13)
        p_t.font.bold = True
        p_t.font.color.rgb = acolor

        p_d = tf.add_paragraph()
        p_d.text = details
        p_d.font.name = "Segoe UI"
        p_d.font.size = Pt(10.5)
        p_d.font.color.rgb = TEXT_MAIN

    slide7.notes_slide.notes_text_frame.text = (
        "This slide presents the end-to-end layered architecture. "
        "Notice Tier 1: offline document deduplication and SVD modeling. "
        "Tier 2: in-memory Radix Trie, inverted index, and dense LSA vectors loaded into JVM heap in just 10 seconds. "
        "Tier 3: a zero-dependency Java HTTP server serving REST APIs. "
        "Tier 4: a responsive modern frontend offering instant autocomplete, explainable results, and research tools."
    )

    # =========================================================================
    # SLIDE 8: Data Structures: Radix Trie & Inverted Index
    # =========================================================================
    slide8 = prs.slides.add_slide(blank_layout)
    add_header(slide8, "Core Data Structures: Compressed Trie & Inverted Index")

    # Left: Compressed Trie
    add_card(slide8, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_trie = slide8.shapes.add_textbox(Inches(1.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_tr = tb_trie.text_frame
    tf_tr.word_wrap = True

    p = tf_tr.paragraphs[0]
    p.text = "Compressed Radix Trie (Trie.java)"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = BLUE_ACCENT

    trie_pts = [
        "• Edge Substring Compression:\nUnlike a standard Trie where each edge represents exactly 1 character, our Radix Trie stores full substrings on edges (e.g., 'crick' -> 'et' instead of 'c'-'r'-'i'-'c'-'k'-'e'-'t').",
        "• Node Splitting Algorithm:\nWhen inserting a word sharing a common prefix with an existing edge, the edge is split dynamically into a common parent node and distinct branch children.",
        "• Sub-Millisecond Complexity O(L):\nPrefix search latency depends solely on prefix string length L, completely independent of corpus document volume N.",
        "• Verified Benchmark Performance:\nLookup time ranges from 0.067 ms to 0.513 ms across 64,380 unique terms, providing silky smooth 60fps live typing suggestions."
    ]
    for pt in trie_pts:
        para = tf_tr.add_paragraph()
        para.text = f"\n{pt}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(11)
        para.font.color.rgb = TEXT_MAIN

    # Right: Inverted Index
    add_card(slide8, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_idx = slide8.shapes.add_textbox(Inches(7.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_idx = tb_idx.text_frame
    tf_idx.word_wrap = True

    p = tf_idx.paragraphs[0]
    p.text = "Inverted Index (InvertedIndex.java)"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = VIOLET

    idx_pts = [
        "• Postings List Organization:\nMap<String, Set<String>> mapping each unique normalized vocabulary term to the exact set of Document IDs in which it occurs.",
        "• Sublinear Candidate Pruning:\nDuring query evaluation, candidate documents are discovered via fast Set union across valid query terms, avoiding full O(N) linear corpus scans.",
        "• Memory Footprint Optimization:\nIndexes 3,152,014 tokens and 64,380 vocabulary terms using only 132 MB of heap memory, enabling local deployment without server clustering.",
        "• High Ingestion Throughput:\nFull document tokenization and inverted index construction across 50,000 files completes in 2.11 seconds."
    ]
    for pt in idx_pts:
        para = tf_idx.add_paragraph()
        para.text = f"\n{pt}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(11)
        para.font.color.rgb = TEXT_MAIN

    slide8.notes_slide.notes_text_frame.text = (
        "Here we examine our two primary retrieval data structures. "
        "The Compressed Trie collapses non-branching character paths into substring edges. "
        "Its prefix search complexity is O(L), taking less than half a millisecond. "
        "The Inverted Index maps 64,380 unique terms to posting lists, processing 3.15 million tokens in just 2.1 seconds on 132 MB of heap."
    )

    # =========================================================================
    # SLIDE 9: Ranking Algorithms & Coverage-Aware TF-IDF
    # =========================================================================
    slide9 = prs.slides.add_slide(blank_layout)
    add_header(slide9, "Ranking Algorithms & Coverage-Aware TF-IDF Formulation")

    # 3 Column Cards for the 3 Algorithms
    col_w = Inches(3.75)
    
    # Col 1: Baseline TF-IDF
    add_card(slide9, Inches(0.8), Inches(1.5), col_w, Inches(5.3))
    tb1 = slide9.shapes.add_textbox(Inches(0.95), Inches(1.75), Inches(3.45), Inches(4.8))
    tf1 = tb1.text_frame
    tf1.word_wrap = True
    p = tf1.paragraphs[0]
    p.text = "Baseline TF-IDF"
    p.font.name = "Segoe UI"
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = NAVY_HEADER

    p_f1 = tf1.add_paragraph()
    p_f1.text = "\nFormula:\nScore = Sum(TF(t, d) * IDF(t))"
    p_f1.font.name = "Consolas"
    p_f1.font.size = Pt(11)
    p_f1.font.bold = True
    p_f1.font.color.rgb = VIOLET

    p_d1 = tf1.add_paragraph()
    p_d1.text = (
        "\n• Linear summation over query terms.\n"
        "• IDF(t) = ln(N / DF(t)).\n"
        "• Severe Anomaly:\n"
        "  If one term has high TF and rare IDF, it overwhelms missing terms completely.\n"
        "• Mean P@5: 68.00%\n"
        "• Mean nDCG@5: 0.7859"
    )
    p_d1.font.name = "Segoe UI"
    p_d1.font.size = Pt(10.5)
    p_d1.font.color.rgb = TEXT_MAIN

    # Col 2: Coverage-Aware TF-IDF (Our Novelty)
    add_card(slide9, Inches(4.75), Inches(1.5), col_w, Inches(5.3), border_color=VIOLET)
    tb2 = slide9.shapes.add_textbox(Inches(4.9), Inches(1.75), Inches(3.45), Inches(4.8))
    tf2 = tb2.text_frame
    tf2.word_wrap = True
    p = tf2.paragraphs[0]
    p.text = "Coverage-Aware TF-IDF"
    p.font.name = "Segoe UI"
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = VIOLET

    p_f2 = tf2.add_paragraph()
    p_f2.text = "\nFormula:\nScore_cov = Score_raw * (m / k)^1.5"
    p_f2.font.name = "Consolas"
    p_f2.font.size = Pt(11)
    p_f2.font.bold = True
    p_f2.font.color.rgb = VIOLET

    p_d2 = tf2.add_paragraph()
    p_d2.text = (
        "\n• m = matched terms in doc\n"
        "• k = total valid query terms\n"
        "• Coverage Ratio = m / k\n"
        "• Exponent 1.5 creates steep non-linear penalty for partial matches.\n"
        "• 4/4 match -> factor 1.0 (100%)\n"
        "• 1/4 match -> factor 0.125 (87.5% penalty!)\n"
        "• Mean P@5: 88.00% (+20%)\n"
        "• Mean nDCG@5: 0.8936"
    )
    p_d2.font.name = "Segoe UI"
    p_d2.font.size = Pt(10.5)
    p_d2.font.color.rgb = TEXT_MAIN

    # Col 3: Okapi BM25
    add_card(slide9, Inches(8.7), Inches(1.5), col_w, Inches(5.3))
    tb3 = slide9.shapes.add_textbox(Inches(8.85), Inches(1.75), Inches(3.45), Inches(4.8))
    tf3 = tb3.text_frame
    tf3.word_wrap = True
    p = tf3.paragraphs[0]
    p.text = "Okapi BM25"
    p.font.name = "Segoe UI"
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = BLUE_ACCENT

    p_f3 = tf3.add_paragraph()
    p_f3.text = "\nFormula:\nIDF * (TF*(k1+1)) / (TF + k1*B)"
    p_f3.font.name = "Consolas"
    p_f3.font.size = Pt(11)
    p_f3.font.bold = True
    p_f3.font.color.rgb = BLUE_ACCENT

    p_d3 = tf3.add_paragraph()
    p_d3.text = (
        "\n• B = (1 - b + b * (docLen / avgLen))\n"
        "• k1 = 1.2 (Term Saturation)\n"
        "• b = 0.75 (Length Normalization)\n"
        "• Prevents TF blowup from term repetition in short files.\n"
        "• Probabilistic relevance foundation.\n"
        "• Mean P@5: 90.00%\n"
        "• Mean nDCG@5: 0.9431"
    )
    p_d3.font.name = "Segoe UI"
    p_d3.font.size = Pt(10.5)
    p_d3.font.color.rgb = TEXT_MAIN

    slide9.notes_slide.notes_text_frame.text = (
        "This slide presents our mathematical formulations. "
        "Notice the middle column: Coverage-Aware TF-IDF. "
        "By applying the exponent factor of (m/k)^1.5, a document covering all 4 terms retains 100% of its score, "
        "whereas a document matching only 1 of 4 terms receives an 87.5% penalty. "
        "This completely fixes the single-term dominance defect while maintaining computational efficiency."
    )

    # =========================================================================
    # SLIDE 10: 128-D LSA Semantic Search
    # =========================================================================
    slide10 = prs.slides.add_slide(blank_layout)
    add_header(slide10, "128-Dimensional Latent Semantic Analysis (LSA) Engine")

    # Left: SVD Formulation & Concept Projection
    add_card(slide10, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_lsa = slide10.shapes.add_textbox(Inches(1.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_lsa = tb_lsa.text_frame
    tf_lsa.word_wrap = True

    p = tf_lsa.paragraphs[0]
    p.text = "Truncated SVD & Concept Space Projection"
    p.font.name = "Segoe UI"
    p.font.size = Pt(16)
    p.font.bold = True
    p.font.color.rgb = VIOLET

    lsa_text = [
        "• Mathematical Foundation:\nMatrix decomposition: X ≈ U_k · Σ_k · V_k^T\nwhere k = 128 latent semantic dimensions, X is the 50,000 × 25,000 TF-IDF document-term matrix.",
        "• Query Projection into Latent Concept Space:\nq_vec = Sum(IDF(t) · TermVector(t))\nNormalized via unit L2 norm: q_norm = q_vec / ||q_vec||_2.",
        "• Semantic Cosine Similarity:\nScore(q, d) = DotProduct(q_norm, doc_vec(d))\nEvaluated via dense SIMD float arrays in pure Java.",
        "• Overcomes Vocabulary Mismatch:\nRetrieves relevant articles even when zero keywords overlap (e.g., query 'cardiac surgery' correctly retrieves cardiology papers discussing 'myocardial bypass' and 'valve repair')."
    ]
    for pt in lsa_text:
        para = tf_lsa.add_paragraph()
        para.text = f"\n{pt}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(11)
        para.font.color.rgb = TEXT_MAIN

    # Right: Screenshot of Semantic Results
    add_card(slide10, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    sem_img_path = os.path.join(screenshots_dir, "06_semantic_lsa_results.png")
    if os.path.exists(sem_img_path):
        slide10.shapes.add_picture(sem_img_path, Inches(6.9), Inches(1.6), Inches(5.5), Inches(4.2))
    
    tb_scap = slide10.shapes.add_textbox(Inches(6.9), Inches(5.9), Inches(5.5), Inches(0.8))
    tf_sc = tb_scap.text_frame
    tf_sc.word_wrap = True
    p_sc = tf_sc.paragraphs[0]
    p_sc.text = "Verified Screenshot: Semantic Search for 'cardiac surgery' showing Cosine Similarity (0.898), Active Latent Topics, Related Keywords, and Explanations."
    p_sc.font.name = "Segoe UI"
    p_sc.font.size = Pt(9.5)
    p_sc.font.color.rgb = TEXT_MUTED

    slide10.notes_slide.notes_text_frame.text = (
        "Slide 10 details our 128-dimensional Latent Semantic Analysis engine. "
        "By projecting the document collection into a low-rank concept space via Truncated SVD, "
        "the engine discovers semantic associations. Notice on the right the actual screenshot of our running application: "
        "querying 'cardiac surgery' brings up cardiovascular medicine documents with similarity scores, active latent topics, and transparent explainability."
    )

    # =========================================================================
    # SLIDE 11: Multi-Domain Dataset Engineering
    # =========================================================================
    slide11 = prs.slides.add_slide(blank_layout)
    add_header(slide11, "Multi-Domain Dataset Engineering (50,000 Documents)")

    # Left: Category Chart
    add_card(slide11, Inches(0.8), Inches(1.5), Inches(6.0), Inches(5.3))
    cat_chart_path = os.path.join(charts_dir, "02_corpus_category_distribution.png")
    if os.path.exists(cat_chart_path):
        slide11.shapes.add_picture(cat_chart_path, Inches(0.9), Inches(1.6), Inches(5.8), Inches(4.5))

    tb_ccap = slide11.shapes.add_textbox(Inches(0.9), Inches(6.15), Inches(5.8), Inches(0.6))
    tf_cc = tb_ccap.text_frame
    p_cc = tf_cc.paragraphs[0]
    p_cc.text = "Figure: Verified Multi-Domain Corpus Distribution across 11 topical domains."
    p_cc.font.name = "Segoe UI"
    p_cc.font.size = Pt(9.5)
    p_cc.font.color.rgb = TEXT_MUTED

    # Right: Data Provenance & Integrity
    add_card(slide11, Inches(7.1), Inches(1.5), Inches(5.4), Inches(5.3))
    tb_ds = slide11.shapes.add_textbox(Inches(7.3), Inches(1.7), Inches(5.0), Inches(4.8))
    tf_ds = tb_ds.text_frame
    tf_ds.word_wrap = True

    p = tf_ds.paragraphs[0]
    p.text = "Data Provenance & Quality Guarantees"
    p.font.name = "Segoe UI"
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = NAVY_HEADER

    ds_pts = [
        "• Verified Sources & Permissive Licenses:\n  - Misra News Category (44,175 docs; CC BY 4.0)\n  - OpenAlex Scholarly Catalog (3,000 docs; CC0)\n  - BBC News Corpus (2,225 docs; Academic Use)\n  - PubMed Central (600 docs; Public Domain)",
        "• Cryptographic Deduplication:\n100% SHA-256 deduplicated across normalized lowercase tokens. 1,853 duplicate records rejected during staging.",
        "• Collision-Free Document IDs:\nSequential IDs (exp_00001 to exp_42175 + baselines) guarantee zero ID collisions.",
        "• Substantive Content Filter:\nEvery document contains at least 20 words (median ~85 words). Zero stub headlines or empty files."
    ]
    for pt in ds_pts:
        para = tf_ds.add_paragraph()
        para.text = f"\n{pt}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(10.5)
        para.font.color.rgb = TEXT_MAIN

    slide11.notes_slide.notes_text_frame.text = (
        "Our dataset was scaled to exactly 50,000 documents across 11 diverse topical domains, from news governance to clinical medicine and AI. "
        "Every single document has a verified academic or open-access license. "
        "We applied SHA-256 cryptographic deduplication, rejecting over 1,850 duplicate items during staging to ensure corpus integrity."
    )

    # =========================================================================
    # SLIDE 12: Application UI & Autocomplete Walkthrough
    # =========================================================================
    slide12 = prs.slides.add_slide(blank_layout)
    add_header(slide12, "Production Application UI & Live Autocomplete")

    # Left: Homepage Screenshot
    add_card(slide12, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    ui1_path = os.path.join(screenshots_dir, "01_main_search_interface.png")
    if os.path.exists(ui1_path):
        slide12.shapes.add_picture(ui1_path, Inches(0.9), Inches(1.6), Inches(5.5), Inches(4.3))
    tb_u1 = slide12.shapes.add_textbox(Inches(0.9), Inches(6.0), Inches(5.5), Inches(0.7))
    tf_u1 = tb_u1.text_frame
    p = tf_u1.paragraphs[0]
    p.text = "Search Interface: Live stats banner (50,000 docs, 64,380 terms), algorithm tabs, category filter pills, quick query chips."
    p.font.name = "Segoe UI"
    p.font.size = Pt(9.5)
    p.font.color.rgb = TEXT_MUTED

    # Right: Trie Dropdown Screenshot
    add_card(slide12, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    ui2_path = os.path.join(screenshots_dir, "02_trie_autocomplete_dropdown.png")
    if os.path.exists(ui2_path):
        slide12.shapes.add_picture(ui2_path, Inches(6.9), Inches(1.6), Inches(5.5), Inches(4.3))
    tb_u2 = slide12.shapes.add_textbox(Inches(6.9), Inches(6.0), Inches(5.5), Inches(0.7))
    tf_u2 = tb_u2.text_frame
    p = tf_u2.paragraphs[0]
    p.text = "Compressed Trie Prefix Dropdown: Sub-millisecond prefix completion for 'crick' displaying vocabulary suggestions."
    p.font.name = "Segoe UI"
    p.font.size = Pt(9.5)
    p.font.color.rgb = TEXT_MUTED

    slide12.notes_slide.notes_text_frame.text = (
        "Here are genuine screenshots captured from the running application on port 8080. "
        "On the left, the main dashboard shows live counters reflecting the 50,000 documents and 64,380 terms in the inverted index. "
        "On the right, as the user types 'crick', the Compressed Trie instantly displays vocabulary suggestions in sub-millisecond time."
    )

    # =========================================================================
    # SLIDE 13: Three-Way Ranking Lab & Research Workspace
    # =========================================================================
    slide13 = prs.slides.add_slide(blank_layout)
    add_header(slide13, "Three-Way Ranking Lab & Research Workspace")

    # Left: 3-Way Comparison Lab Screenshot
    add_card(slide13, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    cmp_path = os.path.join(screenshots_dir, "07_three_way_comparison_lab.png")
    if os.path.exists(cmp_path):
        slide13.shapes.add_picture(cmp_path, Inches(0.9), Inches(1.6), Inches(5.5), Inches(4.3))
    tb_c = slide13.shapes.add_textbox(Inches(0.9), Inches(6.0), Inches(5.5), Inches(0.7))
    tf_c = tb_c.text_frame
    p = tf_c.paragraphs[0]
    p.text = "3-Way Ranking Comparison Lab: Side-by-side execution of Coverage-Aware TF-IDF, Okapi BM25, and Baseline TF-IDF for 'heart failure clinical trial'."
    p.font.name = "Segoe UI"
    p.font.size = Pt(9.5)
    p.font.color.rgb = TEXT_MUTED

    # Right: Workspace Screenshot
    add_card(slide13, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    ws_path = os.path.join(screenshots_dir, "08_research_workspace.png")
    if os.path.exists(ws_path):
        slide13.shapes.add_picture(ws_path, Inches(6.9), Inches(1.6), Inches(5.5), Inches(4.3))
    tb_w = slide13.shapes.add_textbox(Inches(6.9), Inches(6.0), Inches(5.5), Inches(0.7))
    tf_w = tb_w.text_frame
    p = tf_w.paragraphs[0]
    p.text = "Research Workspace: Client-server persistence for bookmarks, custom collections, personal research notes, and saved search queries."
    p.font.name = "Segoe UI"
    p.font.size = Pt(9.5)
    p.font.color.rgb = TEXT_MUTED

    slide13.notes_slide.notes_text_frame.text = (
        "Slide 13 shows our evaluation tools. "
        "On the left, the Three-Way Ranking Comparison Lab executes all three algorithms simultaneously on the same query. "
        "Notice how Baseline TF-IDF falls victim to rare-word repetition while Coverage-Aware and BM25 rank the genuine medical trial at Rank 1. "
        "On the right is the Research Workspace, allowing students and scholars to bookmark articles, organize them into collections, and write notes."
    )

    # =========================================================================
    # SLIDE 14: Experimental Results & Benchmarks
    # =========================================================================
    slide14 = prs.slides.add_slide(blank_layout)
    add_header(slide14, "Experimental Results & Measured Benchmarks")

    # Left: Relevance Chart
    add_card(slide14, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    rel_chart_path = os.path.join(charts_dir, "01_relevance_benchmark_chart.png")
    if os.path.exists(rel_chart_path):
        slide14.shapes.add_picture(rel_chart_path, Inches(0.9), Inches(1.6), Inches(5.5), Inches(4.3))
    tb_rc = slide14.shapes.add_textbox(Inches(0.9), Inches(6.0), Inches(5.5), Inches(0.7))
    tf_rc = tb_rc.text_frame
    p = tf_rc.paragraphs[0]
    p.text = "Relevance Quality: Coverage-Aware TF-IDF achieves 88.0% P@5 and 0.8936 nDCG@5 vs 68.0% P@5 for Baseline TF-IDF."
    p.font.name = "Segoe UI"
    p.font.size = Pt(9.5)
    p.font.color.rgb = TEXT_MUTED

    # Right: Latency & Scaling Chart
    add_card(slide14, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    lat_chart_path = os.path.join(charts_dir, "03_latency_breakdown_chart.png")
    if os.path.exists(lat_chart_path):
        slide14.shapes.add_picture(lat_chart_path, Inches(6.9), Inches(1.6), Inches(5.5), Inches(4.3))
    tb_lc = slide14.shapes.add_textbox(Inches(6.9), Inches(6.0), Inches(5.5), Inches(0.7))
    tf_lc = tb_lc.text_frame
    p = tf_lc.paragraphs[0]
    p.text = "Latency Breakdown: Distinguishing algorithmic backend latency (25-300 ms) from browser round-trip time (68-380 ms)."
    p.font.name = "Segoe UI"
    p.font.size = Pt(9.5)
    p.font.color.rgb = TEXT_MUTED

    slide14.notes_slide.notes_text_frame.text = (
        "Here are our measured empirical results. "
        "On the left, the bar chart shows a clear progression: Baseline TF-IDF at 68% P@5, Coverage-Aware jumping to 88%, and BM25 reaching 90-92%. "
        "On the right, we strictly distinguish backend algorithm computation from browser network round-trip time, "
        "confirming that our backend algorithms execute in 25 to 300 milliseconds while the Radix Trie resolves in 0.12 milliseconds."
    )

    # =========================================================================
    # SLIDE 15: Limitations, Conclusion & Demonstration Plan
    # =========================================================================
    slide15 = prs.slides.add_slide(blank_layout)
    add_header(slide15, "Scientific Limitations, Conclusion & Source Code")

    # Left: Limitations & Honest Constraints
    add_card(slide15, Inches(0.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_lim = slide15.shapes.add_textbox(Inches(1.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_lim = tb_lim.text_frame
    tf_lim.word_wrap = True

    p = tf_lim.paragraphs[0]
    p.text = "Honest Scientific Boundaries & Limitations"
    p.font.name = "Segoe UI"
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = RGBColor(220, 38, 38)

    lim_pts = [
        "• Category-Level Precision Proxy:\nAutomated precision/recall metrics measure categorical topical coherence, not fine-grained sentence-level factual correctness.",
        "• Offline LSA Matrix Recomputation:\nAdding new documents requires re-running Truncated SVD offline rather than online streaming SVD update.",
        "• Google Comparison Methodology:\nOur local engine indexes an offline curated 50,000-document corpus. We do not claim to outperform Google's web-scale trillion-page index; our Google Lab is designed for comparative human relevance evaluation on identical topical domains.",
        "• Single-Node Architecture:\nEngine runs on a single JVM instance; distributed sharding across nodes is reserved for future scope."
    ]
    for pt in lim_pts:
        para = tf_lim.add_paragraph()
        para.text = f"\n{pt}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(10.5)
        para.font.color.rgb = TEXT_MAIN

    # Right: Conclusion, Demo Plan & Source Link
    add_card(slide15, Inches(6.8), Inches(1.5), Inches(5.7), Inches(5.3))
    tb_con = slide15.shapes.add_textbox(Inches(7.05), Inches(1.75), Inches(5.2), Inches(4.8))
    tf_con = tb_con.text_frame
    tf_con.word_wrap = True

    p = tf_con.paragraphs[0]
    p.text = "Conclusion & Demonstration Details"
    p.font.name = "Segoe UI"
    p.font.size = Pt(15)
    p.font.bold = True
    p.font.color.rgb = EMERALD

    con_pts = [
        "• Summary of Achievements:\nSuccessfully built, verified, and benchmarked an end-to-end zero-dependency Intelligent Search Engine. Proved that non-linear coverage weighting eliminates TF-IDF single-term inflation (+20% P@5).",
        "• Live Demonstration Script:\n  1. Live Autocomplete typing at http://localhost:8080/\n  2. Query 'quantum cosmology' in Coverage & BM25\n  3. 3-Way Comparison Lab with 'heart failure clinical trial'\n  4. 128-D LSA semantic retrieval for 'cardiac surgery'\n  5. Research Workspace bookmarking & Live Analytics",
        "• Verified Source Code Repository:\nLocal Repository: C:\\Users\\mannu\\IdeaProjects\\SearchEngineProject\nZero external runtime libraries; runs under Oracle JDK 24."
    ]
    for pt in con_pts:
        para = tf_con.add_paragraph()
        para.text = f"\n{pt}"
        para.font.name = "Segoe UI"
        para.font.size = Pt(10.5)
        para.font.color.rgb = TEXT_MAIN

    slide15.notes_slide.notes_text_frame.text = (
        "In conclusion, our project achieves an integrated, verified Information Retrieval platform that eliminates single-term TF-IDF anomalies, "
        "provides sub-millisecond Radix Trie autocomplete, and delivers genuine 128-D semantic retrieval with full diagnostic transparency. "
        "We thank our guide Dr. Ardra P S and open the floor for questions."
    )

    # Save presentation
    output_path = os.path.join(deliverables_dir, "Final_Project_Presentation.pptx")
    prs.save(output_path)
    print(f"Presentation successfully saved to: {output_path}")

if __name__ == "__main__":
    build_presentation()
