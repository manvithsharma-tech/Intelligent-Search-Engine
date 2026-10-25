package comparison;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Encapsulates a manual comparison observation between the Local Search Engine and Google Search.
 */
public class GoogleComparisonRecord {

    private final String id;
    private final String query;
    private final String engine; // "local" or "google"
    private final String localAlgo; // "coverage", "bm25", "baseline", "semantic", or "-"
    private final String category; // "all", "science", etc.
    private final int relevantCount; // 0 to 5
    private final double precisionAt5; // relevantCount / 5.0
    private final String notes;
    private final long responseTimeMs; // optional manually entered or measured latency
    private final long timestamp;
    private final String formattedDate;

    public GoogleComparisonRecord(String id, String query, String engine, String localAlgo,
                                  String category, int relevantCount, String notes,
                                  long responseTimeMs, long timestamp) {
        this.id = id != null ? id : "gcomp_" + System.currentTimeMillis();
        this.query = query != null ? query.trim() : "";
        this.engine = engine != null ? engine.toLowerCase() : "local";
        this.localAlgo = localAlgo != null ? localAlgo : "coverage";
        this.category = category != null ? category : "all";
        this.relevantCount = Math.max(0, Math.min(5, relevantCount));
        this.precisionAt5 = (double) this.relevantCount / 5.0;
        this.notes = notes != null ? notes.trim() : "";
        this.responseTimeMs = Math.max(0, responseTimeMs);
        this.timestamp = timestamp > 0 ? timestamp : System.currentTimeMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        this.formattedDate = sdf.format(new Date(this.timestamp));
    }

    public String getId() {
        return id;
    }

    public String getQuery() {
        return query;
    }

    public String getEngine() {
        return engine;
    }

    public String getLocalAlgo() {
        return localAlgo;
    }

    public String getCategory() {
        return category;
    }

    public int getRelevantCount() {
        return relevantCount;
    }

    public double getPrecisionAt5() {
        return precisionAt5;
    }

    public String getNotes() {
        return notes;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(id)).append("\",");
        sb.append("\"query\":\"").append(escapeJson(query)).append("\",");
        sb.append("\"engine\":\"").append(escapeJson(engine)).append("\",");
        sb.append("\"localAlgo\":\"").append(escapeJson(localAlgo)).append("\",");
        sb.append("\"category\":\"").append(escapeJson(category)).append("\",");
        sb.append("\"relevantCount\":").append(relevantCount).append(",");
        sb.append("\"precisionAt5\":").append(String.format(Locale.US, "%.4f", precisionAt5)).append(",");
        sb.append("\"notes\":\"").append(escapeJson(notes)).append("\",");
        sb.append("\"responseTimeMs\":").append(responseTimeMs).append(",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"formattedDate\":\"").append(escapeJson(formattedDate)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
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
