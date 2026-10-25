import os
import matplotlib.pyplot as plt
import numpy as np

# Set font and style
plt.rcParams['font.sans-serif'] = 'DejaVu Sans'
plt.rcParams['axes.edgecolor'] = '#cbd5e1'
plt.rcParams['axes.linewidth'] = 1.0

out_dir = os.path.abspath("deliverables/evidence/charts")
os.makedirs(out_dir, exist_ok=True)

# -----------------------------------------------------------------------------
# Chart 1: Relevance Benchmark Comparison (P@5, MRR, nDCG@5)
# -----------------------------------------------------------------------------
models = ['Baseline\nTF-IDF', 'Coverage-Aware\nTF-IDF', 'Okapi\nBM25', 'BM25 +\nCoverage']
p5 = [68.0, 88.0, 90.0, 92.0]
mrr = [80.0, 90.0, 92.5, 100.0] # converted to %
ndcg = [78.59, 89.36, 94.31, 100.0] # converted to %

x = np.arange(len(models))
width = 0.25

fig, ax = plt.subplots(figsize=(9, 5.5), dpi=300)
rects1 = ax.bar(x - width, p5, width, label='Mean Precision@5 (%)', color='#3b82f6', edgecolor='#1d4ed8')
rects2 = ax.bar(x, mrr, width, label='Mean MRR (x100)', color='#8b5cf6', edgecolor='#6d28d9')
rects3 = ax.bar(x + width, ndcg, width, label='Mean nDCG@5 (x100)', color='#10b981', edgecolor='#047857')

ax.set_ylabel('Score (%)', fontsize=12, fontweight='bold', color='#1e293b')
ax.set_title('Relevance Quality Benchmark Across Ranking Models (10 Queries)', fontsize=14, fontweight='bold', pad=15, color='#0f172a')
ax.set_xticks(x)
ax.set_xticklabels(models, fontsize=11, fontweight='semibold', color='#334155')
ax.set_ylim(50, 108)
ax.legend(frameon=True, facecolor='#f8fafc', edgecolor='#cbd5e1', fontsize=10)
ax.grid(axis='y', linestyle='--', alpha=0.5)

# Value labels on bars
def autolabel(rects):
    for rect in rects:
        h = rect.get_height()
        ax.annotate(f'{h:.1f}%',
                    xy=(rect.get_x() + rect.get_width() / 2, h),
                    xytext=(0, 4), textcoords="offset points",
                    ha='center', va='bottom', fontsize=8.5, fontweight='bold', color='#0f172a')

autolabel(rects1)
autolabel(rects2)
autolabel(rects3)

plt.tight_layout()
chart1_path = os.path.join(out_dir, "01_relevance_benchmark_chart.png")
plt.savefig(chart1_path)
plt.close()
print("Generated:", chart1_path)

# -----------------------------------------------------------------------------
# Chart 2: Category Distribution of 50,000 Documents
# -----------------------------------------------------------------------------
categories = ['politics', 'entertainment', 'health', 'sport', 'environment', 
              'business', 'education', 'tech', 'science', 'ai', 'finance']
counts = [27631, 11107, 2670, 1901, 1496, 1314, 998, 844, 839, 600, 600]
colors = ['#1e3a8a', '#2563eb', '#0284c7', '#0d9488', '#10b981', 
          '#65a30d', '#d97706', '#ea580c', '#dc2626', '#7c3aed', '#9333ea']

fig, ax = plt.subplots(figsize=(10, 5.5), dpi=300)
bars = ax.barh(categories[::-1], counts[::-1], color=colors[::-1], edgecolor='#334155', height=0.65)
ax.set_xlabel('Number of Documents', fontsize=12, fontweight='bold', color='#1e293b')
ax.set_title('Multi-Domain Corpus Distribution (N = 50,000 Documents)', fontsize=14, fontweight='bold', pad=15, color='#0f172a')
ax.grid(axis='x', linestyle='--', alpha=0.5)

for bar in bars:
    w = bar.get_width()
    pct = (w / 50000.0) * 100
    ax.annotate(f'{w:,} ({pct:.1f}%)',
                xy=(w, bar.get_y() + bar.get_height() / 2),
                xytext=(6, 0), textcoords="offset points",
                ha='left', va='center', fontsize=9, fontweight='semibold', color='#0f172a')

ax.set_xlim(0, 32000)
plt.tight_layout()
chart2_path = os.path.join(out_dir, "02_corpus_category_distribution.png")
plt.savefig(chart2_path)
plt.close()
print("Generated:", chart2_path)

# -----------------------------------------------------------------------------
# Chart 3: Latency Comparison: Backend Algorithm vs Browser RTT
# -----------------------------------------------------------------------------
query_types = [
    'Trie Prefix Autocomplete\n("quant")',
    'Focused 3-Term Query\n("quantum mechanics cosmology")',
    '2-Term Query\n("quantum cosmology")',
    'Broad 4-Term Query\n("heart failure clinical trial")',
    'LSA Semantic Retrieval\n("cardiac surgery")'
]
backend_ms = [0.12, 25.31, 83.0, 301.83, 117.0]
browser_rtt_ms = [4.5, 68.0, 185.0, 380.0, 142.0]

x = np.arange(len(query_types))
width = 0.35

fig, ax = plt.subplots(figsize=(10, 5.5), dpi=300)
rects1 = ax.bar(x - width/2, backend_ms, width, label='Backend Execution Latency (ms)', color='#6366f1', edgecolor='#4338ca')
rects2 = ax.bar(x + width/2, browser_rtt_ms, width, label='Total Browser Round-Trip Time (ms)', color='#f59e0b', edgecolor='#d97706')

ax.set_ylabel('Latency (Milliseconds - Log Scale)', fontsize=12, fontweight='bold', color='#1e293b')
ax.set_title('Query Execution Latency: Backend Algorithm vs Full Browser Round-Trip', fontsize=13, fontweight='bold', pad=15, color='#0f172a')
ax.set_xticks(x)
ax.set_xticklabels(query_types, fontsize=9.5, fontweight='semibold', color='#334155')
ax.set_yscale('log')
ax.set_ylim(0.05, 1000)
ax.legend(frameon=True, facecolor='#f8fafc', edgecolor='#cbd5e1', fontsize=10.5)
ax.grid(axis='y', linestyle='--', alpha=0.5, which='both')

for rect in rects1:
    h = rect.get_height()
    ax.annotate(f'{h:.2f} ms' if h < 1 else f'{h:.1f} ms',
                xy=(rect.get_x() + rect.get_width() / 2, h),
                xytext=(0, 4), textcoords="offset points",
                ha='center', va='bottom', fontsize=8.5, fontweight='bold', color='#312e81')

for rect in rects2:
    h = rect.get_height()
    ax.annotate(f'{h:.1f} ms',
                xy=(rect.get_x() + rect.get_width() / 2, h),
                xytext=(0, 4), textcoords="offset points",
                ha='center', va='bottom', fontsize=8.5, fontweight='bold', color='#78350f')

plt.tight_layout()
chart3_path = os.path.join(out_dir, "03_latency_breakdown_chart.png")
plt.savefig(chart3_path)
plt.close()
print("Generated:", chart3_path)

# -----------------------------------------------------------------------------
# Chart 4: Scaling Performance: Baseline vs 50,000 Scaled Corpus
# -----------------------------------------------------------------------------
metrics = ['Corpus Documents\n(x1,000)', 'Vocabulary Terms\n(x1,000)', 'Pipeline Init Time\n(Seconds)', 'Heap Memory In-Use\n(Megabytes)']
baseline_vals = [7.825, 48.537, 4.77, 58.0]
scaled_vals = [50.000, 64.380, 10.35, 132.03]

x = np.arange(len(metrics))
width = 0.35

fig, ax = plt.subplots(figsize=(9, 5.5), dpi=300)
rects1 = ax.bar(x - width/2, baseline_vals, width, label='Baseline Engine (7,825 Docs)', color='#94a3b8', edgecolor='#64748b')
rects2 = ax.bar(x + width/2, scaled_vals, width, label='Scaled Production Engine (50,000 Docs)', color='#4f46e5', edgecolor='#3730a3')

ax.set_ylabel('Measured Value', fontsize=12, fontweight='bold', color='#1e293b')
ax.set_title('System Scaling & Memory Footprint Benchmark', fontsize=14, fontweight='bold', pad=15, color='#0f172a')
ax.set_xticks(x)
ax.set_xticklabels(metrics, fontsize=10.5, fontweight='semibold', color='#334155')
ax.legend(frameon=True, facecolor='#f8fafc', edgecolor='#cbd5e1', fontsize=10.5)
ax.grid(axis='y', linestyle='--', alpha=0.5)

for rect in rects1:
    h = rect.get_height()
    ax.annotate(f'{h:.1f}',
                xy=(rect.get_x() + rect.get_width() / 2, h),
                xytext=(0, 4), textcoords="offset points",
                ha='center', va='bottom', fontsize=9, fontweight='bold', color='#475569')

for rect in rects2:
    h = rect.get_height()
    ax.annotate(f'{h:.1f}',
                xy=(rect.get_x() + rect.get_width() / 2, h),
                xytext=(0, 4), textcoords="offset points",
                ha='center', va='bottom', fontsize=9, fontweight='bold', color='#312e81')

plt.tight_layout()
chart4_path = os.path.join(out_dir, "04_system_scaling_benchmark.png")
plt.savefig(chart4_path)
plt.close()
print("Generated:", chart4_path)
print("All charts successfully generated!")
