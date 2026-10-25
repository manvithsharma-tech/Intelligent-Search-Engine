package workspace;

import model.Document;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Thread-safe Research Workspace Manager.
 * Handles bookmarks, collections, notes, and saved search queries.
 * Persists workspace data across server restarts into a lightweight local JSON file.
 */
public class WorkspaceManager {

    private static final String DEFAULT_FILE = "workspace.json";
    private static volatile WorkspaceManager instance;

    public static synchronized WorkspaceManager getInstance() {
        if (instance == null) {
            instance = new WorkspaceManager(new File(DEFAULT_FILE));
        }
        return instance;
    }

    private final File storageFile;
    private final Map<String, Collection> collections;
    private final Map<String, Bookmark> bookmarks; // Keyed by docId for O(1) duplicate prevention
    private final List<SavedSearch> savedSearches;
    private final Object lock = new Object();

    public WorkspaceManager(File storageFile) {
        this.storageFile = storageFile;
        this.collections = new LinkedHashMap<>();
        this.bookmarks = new LinkedHashMap<>();
        this.savedSearches = new ArrayList<>();

        // Initialize default collection
        this.collections.put("default", new Collection("default", "General Research", System.currentTimeMillis()));
        loadFromFile();
    }

    /**
     * Bookmarks a document. Prevents duplicate bookmarks for the same docId within the workspace.
     * If already bookmarked, optionally updates the collection or note without duplicating.
     */
    public Bookmark addBookmark(String docId, String category, String collectionId, String notes,
                                Map<String, Document> documentMap) {
        if (docId == null || docId.trim().isEmpty()) {
            throw new IllegalArgumentException("Document ID cannot be null or empty");
        }

        String cleanDocId = docId.trim();
        String targetCollection = (collectionId != null && collections.containsKey(collectionId.trim()))
                ? collectionId.trim()
                : "default";

        synchronized (lock) {
            // Duplicate prevention check
            if (bookmarks.containsKey(cleanDocId)) {
                Bookmark existing = bookmarks.get(cleanDocId);
                if (notes != null && !notes.trim().isEmpty()) {
                    existing.setNotes(notes.trim());
                }
                if (collectionId != null && !collectionId.trim().isEmpty()) {
                    existing.setCollectionId(targetCollection);
                }
                saveToFile();
                return existing;
            }

            // Extract document metadata if available
            String title = "Document #" + cleanDocId;
            String fileName = cleanDocId + ".txt";
            String snippet = "";

            if (documentMap != null) {
                Document doc = null;
                if (category != null && !category.trim().isEmpty()) {
                    doc = documentMap.get(category.toLowerCase().trim() + ":" + cleanDocId);
                }
                if (doc == null) {
                    doc = documentMap.get(cleanDocId);
                }

                if (doc != null) {
                    fileName = doc.getFileName() != null ? doc.getFileName() : fileName;
                    String content = doc.getContent();
                    if (content != null && !content.trim().isEmpty()) {
                        String[] lines = content.split("\\r?\\n");
                        for (String line : lines) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty()) {
                                title = trimmed;
                                break;
                            }
                        }
                        snippet = content.length() > 240 ? content.substring(0, 240) + "..." : content;
                    }
                }
            }

            Bookmark bookmark = new Bookmark(
                    cleanDocId,
                    category != null ? category.trim() : "all",
                    fileName,
                    title,
                    snippet,
                    targetCollection,
                    notes != null ? notes.trim() : "",
                    System.currentTimeMillis()
            );

            bookmarks.put(cleanDocId, bookmark);
            saveToFile();
            return bookmark;
        }
    }

    /**
     * Removes a bookmark by document ID.
     */
    public boolean removeBookmark(String docId) {
        if (docId == null || docId.trim().isEmpty()) return false;
        synchronized (lock) {
            Bookmark removed = bookmarks.remove(docId.trim());
            if (removed != null) {
                saveToFile();
                return true;
            }
            return false;
        }
    }

    /**
     * Checks if a document is already bookmarked.
     */
    public boolean isBookmarked(String docId) {
        if (docId == null || docId.trim().isEmpty()) return false;
        synchronized (lock) {
            return bookmarks.containsKey(docId.trim());
        }
    }

    /**
     * Updates personal notes for a bookmarked document.
     */
    public boolean updateNotes(String docId, String notes) {
        if (docId == null || docId.trim().isEmpty()) return false;
        synchronized (lock) {
            Bookmark bm = bookmarks.get(docId.trim());
            if (bm != null) {
                bm.setNotes(notes != null ? notes.trim() : "");
                saveToFile();
                return true;
            }
            return false;
        }
    }

    /**
     * Assigns a bookmarked document to a specific collection.
     */
    public boolean assignCollection(String docId, String collectionId) {
        if (docId == null || docId.trim().isEmpty()) return false;
        String colId = collectionId != null && !collectionId.trim().isEmpty() ? collectionId.trim() : "default";

        synchronized (lock) {
            Bookmark bm = bookmarks.get(docId.trim());
            if (bm != null && collections.containsKey(colId)) {
                bm.setCollectionId(colId);
                saveToFile();
                return true;
            }
            return false;
        }
    }

    /**
     * Creates a new collection with a unique non-empty name.
     */
    public Collection createCollection(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Collection name cannot be blank");
        }
        String cleanName = name.trim();

        synchronized (lock) {
            // Check for duplicate name
            for (Collection c : collections.values()) {
                if (c.getName().equalsIgnoreCase(cleanName)) {
                    return c; // Reuse existing collection with same name
                }
            }

            String id = "col_" + System.currentTimeMillis() + "_" + (collections.size() + 1);
            Collection col = new Collection(id, cleanName, System.currentTimeMillis());
            collections.put(id, col);
            saveToFile();
            return col;
        }
    }

    /**
     * Renames an existing collection.
     */
    public boolean renameCollection(String id, String newName) {
        if (id == null || id.trim().isEmpty() || newName == null || newName.trim().isEmpty()) {
            return false;
        }
        String cleanId = id.trim();
        String cleanName = newName.trim();

        synchronized (lock) {
            Collection c = collections.get(cleanId);
            if (c != null) {
                c.setName(cleanName);
                saveToFile();
                return true;
            }
            return false;
        }
    }

    /**
     * Deletes a collection. Reassigns any documents in that collection to 'default'.
     * The default collection itself cannot be deleted.
     */
    public boolean deleteCollection(String id) {
        if (id == null || id.trim().isEmpty() || "default".equalsIgnoreCase(id.trim())) {
            return false; // Cannot delete default collection
        }
        String cleanId = id.trim();

        synchronized (lock) {
            if (collections.containsKey(cleanId)) {
                collections.remove(cleanId);
                // Reassign orphaned bookmarks to default
                for (Bookmark bm : bookmarks.values()) {
                    if (cleanId.equals(bm.getCollectionId())) {
                        bm.setCollectionId("default");
                    }
                }
                saveToFile();
                return true;
            }
            return false;
        }
    }

    /**
     * Saves a search query for fast recall.
     */
    public SavedSearch saveSearch(String query, String algo, String category) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be blank");
        }
        String cleanQuery = query.trim();
        String cleanAlgo = algo != null ? algo.trim().toLowerCase() : "coverage";
        String cleanCat = category != null ? category.trim() : "all";

        synchronized (lock) {
            // Check if identical query + algo already saved
            for (SavedSearch ss : savedSearches) {
                if (ss.getQuery().equalsIgnoreCase(cleanQuery) && ss.getAlgo().equalsIgnoreCase(cleanAlgo)) {
                    return ss; // Avoid duplicate
                }
            }

            String id = "ss_" + System.currentTimeMillis() + "_" + (savedSearches.size() + 1);
            SavedSearch ss = new SavedSearch(id, cleanQuery, cleanAlgo, cleanCat, System.currentTimeMillis());
            savedSearches.add(0, ss); // Newest at top
            saveToFile();
            return ss;
        }
    }

    /**
     * Deletes a saved search by ID.
     */
    public boolean deleteSearch(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        String cleanId = id.trim();

        synchronized (lock) {
            Iterator<SavedSearch> it = savedSearches.iterator();
            while (it.hasNext()) {
                if (it.next().getId().equals(cleanId)) {
                    it.remove();
                    saveToFile();
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Clears all workspace data (for testing or reset).
     */
    public void clearWorkspace() {
        synchronized (lock) {
            bookmarks.clear();
            savedSearches.clear();
            collections.clear();
            collections.put("default", new Collection("default", "General Research", System.currentTimeMillis()));
            saveToFile();
        }
    }

    /**
     * Returns full workspace JSON.
     */
    public String getWorkspaceJson() {
        synchronized (lock) {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"totalBookmarks\":").append(bookmarks.size()).append(",");
            sb.append("\"totalCollections\":").append(collections.size()).append(",");
            sb.append("\"totalSavedSearches\":").append(savedSearches.size()).append(",");

            // Collections
            sb.append("\"collections\":[");
            int cIdx = 0;
            for (Collection c : collections.values()) {
                if (cIdx++ > 0) sb.append(",");
                sb.append(c.toJson());
            }
            sb.append("],");

            // Bookmarks
            sb.append("\"bookmarks\":[");
            int bIdx = 0;
            // Newest bookmarks first
            List<Bookmark> list = new ArrayList<>(bookmarks.values());
            Collections.reverse(list);
            for (Bookmark bm : list) {
                if (bIdx++ > 0) sb.append(",");
                sb.append(bm.toJson());
            }
            sb.append("],");

            // Saved Searches
            sb.append("\"savedSearches\":[");
            int sIdx = 0;
            for (SavedSearch ss : savedSearches) {
                if (sIdx++ > 0) sb.append(",");
                sb.append(ss.toJson());
            }
            sb.append("]");

            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Persists workspace data to JSON file atomically.
     */
    private void saveToFile() {
        try {
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            String json = getWorkspaceJson();
            File tempFile = new File(storageFile.getAbsolutePath() + ".tmp");
            Files.writeString(tempFile.toPath(), json, StandardCharsets.UTF_8);
            Files.move(tempFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("WorkspaceManager: Failed to save workspace: " + e.getMessage());
        }
    }

    /**
     * Restores workspace data from JSON file.
     */
    private void loadFromFile() {
        if (!storageFile.exists() || storageFile.length() == 0) {
            return;
        }

        try {
            String content = Files.readString(storageFile.toPath(), StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || !content.startsWith("{")) {
                return;
            }

            synchronized (lock) {
                // Parse collections
                int colStart = content.indexOf("\"collections\":[");
                if (colStart != -1) {
                    int colEnd = content.indexOf("],", colStart);
                    if (colEnd != -1) {
                        String colArray = content.substring(colStart + 15, colEnd);
                        parseCollectionsJson(colArray);
                    }
                }

                // Parse bookmarks
                int bmStart = content.indexOf("\"bookmarks\":[");
                if (bmStart != -1) {
                    int bmEnd = content.indexOf("],", bmStart);
                    if (bmEnd != -1) {
                        String bmArray = content.substring(bmStart + 13, bmEnd);
                        parseBookmarksJson(bmArray);
                    }
                }

                // Parse saved searches
                int ssStart = content.indexOf("\"savedSearches\":[");
                if (ssStart != -1) {
                    int ssEnd = content.lastIndexOf("]");
                    if (ssEnd != -1 && ssEnd > ssStart) {
                        String ssArray = content.substring(ssStart + 17, ssEnd);
                        parseSavedSearchesJson(ssArray);
                    }
                }
            }
            System.out.println("WorkspaceManager: Loaded " + bookmarks.size() + " bookmarks, " +
                    collections.size() + " collections, " + savedSearches.size() + " saved searches from " + storageFile.getName());
        } catch (Exception e) {
            System.err.println("WorkspaceManager: Could not load persistence file: " + e.getMessage());
        }
    }

    private void parseCollectionsJson(String json) {
        List<String> objects = extractJsonObjects(json);
        for (String s : objects) {
            String id = extractString(s, "id");
            String name = extractString(s, "name");
            long created = extractLong(s, "createdAt", System.currentTimeMillis());
            if (id != null && !id.isEmpty() && name != null) {
                collections.put(id, new Collection(id, name, created));
            }
        }
        if (!collections.containsKey("default")) {
            collections.put("default", new Collection("default", "General Research", System.currentTimeMillis()));
        }
    }

    private void parseBookmarksJson(String json) {
        List<String> objects = extractJsonObjects(json);
        for (String s : objects) {
            String docId = extractString(s, "docId");
            String cat = extractString(s, "category");
            String fileName = extractString(s, "fileName");
            String title = extractString(s, "title");
            String snippet = extractString(s, "snippet");
            String colId = extractString(s, "collectionId");
            String notes = extractString(s, "notes");
            long savedAt = extractLong(s, "savedAt", System.currentTimeMillis());

            if (docId != null && !docId.isEmpty()) {
                bookmarks.put(docId, new Bookmark(
                        docId, cat, fileName, title, snippet,
                        colId != null ? colId : "default",
                        notes != null ? notes : "",
                        savedAt
                ));
            }
        }
    }

    private void parseSavedSearchesJson(String json) {
        List<String> objects = extractJsonObjects(json);
        for (String s : objects) {
            String id = extractString(s, "id");
            String q = extractString(s, "query");
            String algo = extractString(s, "algo");
            String cat = extractString(s, "category");
            long savedAt = extractLong(s, "savedAt", System.currentTimeMillis());

            if (id != null && q != null && !q.isEmpty()) {
                savedSearches.add(new SavedSearch(id, q, algo, cat, savedAt));
            }
        }
    }

    private List<String> extractJsonObjects(String json) {
        List<String> list = new ArrayList<>();
        int i = 0;
        int n = json.length();

        while (i < n) {
            int start = json.indexOf('{', i);
            if (start == -1) break;

            int depth = 0;
            int end = -1;
            boolean inString = false;
            boolean escaped = false;

            for (int j = start; j < n; j++) {
                char c = json.charAt(j);
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = !inString;
                    continue;
                }
                if (!inString) {
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) {
                            end = j;
                            break;
                        }
                    }
                }
            }

            if (end != -1) {
                list.add(json.substring(start, end + 1));
                i = end + 1;
            } else {
                break;
            }
        }
        return list;
    }

    private static String extractString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return null;
        start += pattern.length();

        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == 'n') sb.append('\n');
                else if (c == 'r') sb.append('\r');
                else if (c == 't') sb.append('\t');
                else sb.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static long extractLong(String json, String key, long defaultVal) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return defaultVal;
        start += pattern.length();

        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }

        if (start < end) {
            try {
                return Long.parseLong(json.substring(start, end));
            } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }
}
