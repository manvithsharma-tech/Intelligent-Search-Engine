package workspace;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SavedSearch {
    private final String id;
    private final String query;
    private final String algo;
    private final String category;
    private final long savedAt;

    public SavedSearch(String id, String query, String algo, String category, long savedAt) {
        this.id = id != null ? id : ("ss_" + System.currentTimeMillis());
        this.query = query != null ? query.trim() : "";
        this.algo = algo != null ? algo.toLowerCase().trim() : "coverage";
        this.category = category != null ? category.trim() : "all";
        this.savedAt = savedAt > 0 ? savedAt : System.currentTimeMillis();
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

    public long getSavedAt() {
        return savedAt;
    }

    public String getFormattedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
        return sdf.format(new Date(savedAt));
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(escapeJson(id)).append("\",");
        sb.append("\"query\":\"").append(escapeJson(query)).append("\",");
        sb.append("\"algo\":\"").append(escapeJson(algo)).append("\",");
        sb.append("\"category\":\"").append(escapeJson(category)).append("\",");
        sb.append("\"savedAt\":").append(savedAt).append(",");
        sb.append("\"formattedDate\":\"").append(escapeJson(getFormattedDate())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
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
