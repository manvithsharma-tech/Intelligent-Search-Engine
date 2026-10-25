package analytics;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Represents a single genuine search request recorded by the search engine.
 * Records actual backend execution time and optional client browser round-trip time.
 */
public class SearchRecord {

    private final String id;
    private final String query;
    private final String algo;
    private final String category;
    private final long timestamp;
    private long executionTimeMs;
    private long roundTripTimeMs;
    private int totalMatches;
    private final boolean isComparison;
    private final Map<String, Long> algoTimings;

    public SearchRecord(String id, String query, String algo, String category,
                        long timestamp, long executionTimeMs, long roundTripTimeMs,
                        int totalMatches, boolean isComparison) {
        this.id = id;
        this.query = query != null ? query : "";
        this.algo = algo != null ? algo : "coverage";
        this.category = category != null ? category : "all";
        this.timestamp = timestamp;
        this.executionTimeMs = executionTimeMs;
        this.roundTripTimeMs = roundTripTimeMs;
        this.totalMatches = totalMatches;
        this.isComparison = isComparison;
        this.algoTimings = new LinkedHashMap<>();
    }

    public String getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public String getAlgo() {
        return algo;
    }

    public String getCategory() {
        return category;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public long getRoundTripTimeMs() {
        return roundTripTimeMs;
    }

    public void setRoundTripTimeMs(long roundTripTimeMs) {
        this.roundTripTimeMs = roundTripTimeMs;
    }

    public int getTotalMatches() {
        return totalMatches;
    }

    public void setTotalMatches(int totalMatches) {
        this.totalMatches = totalMatches;
    }

    public boolean isComparison() {
        return isComparison;
    }

    public Map<String, Long> getAlgoTimings() {
        return algoTimings;
    }

    public void addAlgoTiming(String algorithm, long durationMs) {
        algoTimings.put(algorithm, durationMs);
        // Recalculate combined execution time as sum of individual algorithm timings
        long sum = 0;
        for (Long t : algoTimings.values()) {
            if (t != null) sum += t;
        }
        this.executionTimeMs = sum;
    }

    public String getAlgoLabel() {
        if (isComparison || "compare".equalsIgnoreCase(algo)) {
            return "3-Way Comparison";
        }
        switch (algo.toLowerCase()) {
            case "semantic":
                return "Semantic Search (LSA)";
            case "bm25":
                return "Okapi BM25";
            case "baseline":
                return "Baseline TF-IDF";
            case "coverage":
            default:
                return "Coverage-Aware TF-IDF";
        }
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(id)).append("\",");
        sb.append("\"query\":\"").append(escapeJson(query)).append("\",");
        sb.append("\"algo\":\"").append(escapeJson(algo)).append("\",");
        sb.append("\"algoLabel\":\"").append(escapeJson(getAlgoLabel())).append("\",");
        sb.append("\"category\":\"").append(escapeJson(category)).append("\",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"formattedTime\":\"").append(escapeJson(getFormattedTime())).append("\",");
        sb.append("\"executionTimeMs\":").append(executionTimeMs).append(",");
        sb.append("\"roundTripTimeMs\":").append(roundTripTimeMs).append(",");
        sb.append("\"totalMatches\":").append(totalMatches).append(",");
        sb.append("\"isComparison\":").append(isComparison);

        if (!algoTimings.isEmpty()) {
            sb.append(",\"algoTimings\":{");
            int idx = 0;
            for (Map.Entry<String, Long> entry : algoTimings.entrySet()) {
                if (idx++ > 0) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":").append(entry.getValue());
            }
            sb.append("}");
        }

        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
