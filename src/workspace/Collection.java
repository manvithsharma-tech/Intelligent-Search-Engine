package workspace;

public class Collection {
    private final String id;
    private String name;
    private final long createdAt;

    public Collection(String id, String name, long createdAt) {
        this.id = id != null ? id : "col_" + System.currentTimeMillis();
        this.name = name != null && !name.trim().isEmpty() ? name.trim() : "Untitled Collection";
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name.trim();
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String toJson() {
        return "{" +
                "\"id\":\"" + escapeJson(id) + "\"," +
                "\"name\":\"" + escapeJson(name) + "\"," +
                "\"createdAt\":" + createdAt +
                "}";
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
