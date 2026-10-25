package workspace;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Bookmark {
    private final String docId;
    private final String category;
    private final String fileName;
    private final String title;
    private final String snippet;
    private String collectionId;
    private String notes;
    private final long savedAt;

    public Bookmark(String docId, String category, String fileName, String title,
                    String snippet, String collectionId, String notes, long savedAt) {
        this.docId = docId != null ? docId : "";
        this.category = category != null ? category : "all";
        this.fileName = fileName != null ? fileName : (docId + ".txt");
        this.title = title != null && !title.trim().isEmpty() ? title.trim() : ("Document #" + docId);
        this.snippet = snippet != null ? snippet : "";
        this.collectionId = collectionId != null && !collectionId.trim().isEmpty() ? collectionId.trim() : "default";
        this.notes = notes != null ? notes.trim() : "";
        this.savedAt = savedAt > 0 ? savedAt : System.currentTimeMillis();
    }

    public String getDocId() {
        return docId;
    }

    public String getCategory() {
        return category;
    }

    public String getFileName() {
        return fileName;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(String collectionId) {
        if (collectionId != null && !collectionId.trim().isEmpty()) {
            this.collectionId = collectionId.trim();
        }
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : "";
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
        sb.append("\"docId\":\"").append(escapeJson(docId)).append("\",");
        sb.append("\"category\":\"").append(escapeJson(category)).append("\",");
        sb.append("\"fileName\":\"").append(escapeJson(fileName)).append("\",");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"snippet\":\"").append(escapeJson(snippet)).append("\",");
        sb.append("\"collectionId\":\"").append(escapeJson(collectionId)).append("\",");
        sb.append("\"notes\":\"").append(escapeJson(notes)).append("\",");
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
