package analytics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe search analytics manager.
 * Tracks search counts, response times (backend execution and browser round-trip),
 * algorithm distribution, recent queries, and persists data to a local JSON file.
 */
public class AnalyticsManager {

    private static final String DEFAULT_FILE = "analytics.json";
    private static final int MAX_RECORD_CAPACITY = 1000;

    private static volatile AnalyticsManager instance;

    public static synchronized AnalyticsManager getInstance() {
        if (instance == null) {
            instance = new AnalyticsManager(new File(DEFAULT_FILE));
        }
        return instance;
    }

    private final File storageFile;
    private final List<SearchRecord> records;
    private final Map<String, SearchRecord> recordMap;
    private final AtomicLong idCounter;
    private final Object lock = new Object();

    public AnalyticsManager(File storageFile) {
        this.storageFile = storageFile;
        this.records = new ArrayList<>();
        this.recordMap = new HashMap<>();
        this.idCounter = new AtomicLong(System.currentTimeMillis() % 1000000);
        loadFromFile();
    }

    /**
     * Records a genuine single-algorithm search request.
     */
    public SearchRecord recordSingleSearch(String query, String category, String algo,
                                           long executionTimeMs, int totalMatches) {
        if (query == null || query.trim().isEmpty()) {
            return null; // Ignore blank/empty queries
        }

        String searchId = "srch_" + System.currentTimeMillis() + "_" + idCounter.incrementAndGet();
        SearchRecord record = new SearchRecord(
                searchId,
                query.trim(),
                algo != null ? algo.toLowerCase() : "coverage",
                category != null ? category : "all",
                System.currentTimeMillis(),
                Math.max(0, executionTimeMs),
                0, // round-trip time initially 0 until client reports it
                totalMatches,
                false
        );

        synchronized (lock) {
            addRecordInternal(record);
            saveToFile();
        }

        return record;
    }

    /**
     * Records a subquery belonging to a 3-Way Comparison Lab search.
     * All subqueries sharing the same compareId are aggregated into EXACTLY ONE user search record.
     */
    public SearchRecord recordComparisonSubquery(String compareId, String query, String category,
                                                 String algo, long durationMs, int totalMatches) {
        if (query == null || query.trim().isEmpty() || compareId == null || compareId.trim().isEmpty()) {
            return null;
        }

        String cleanCompareId = compareId.trim();
        SearchRecord record;

        synchronized (lock) {
            record = recordMap.get(cleanCompareId);
            if (record == null) {
                record = new SearchRecord(
                        cleanCompareId,
                        query.trim(),
                        "compare",
                        category != null ? category : "all",
                        System.currentTimeMillis(),
                        Math.max(0, durationMs),
                        0,
                        totalMatches,
                        true
                );
                record.addAlgoTiming(algo, Math.max(0, durationMs));
                addRecordInternal(record);
            } else {
                record.addAlgoTiming(algo, Math.max(0, durationMs));
                if (totalMatches > record.getTotalMatches()) {
                    record.setTotalMatches(totalMatches);
                }
            }
            saveToFile();
        }

        return record;
    }

    /**
     * Updates the client-side round-trip time measured by the browser for a recorded search ID.
     */
    public boolean updateRoundTripTime(String searchId, long roundTripTimeMs) {
        if (searchId == null || searchId.trim().isEmpty() || roundTripTimeMs < 0) {
            return false;
        }

        synchronized (lock) {
            SearchRecord record = recordMap.get(searchId.trim());
            if (record != null) {
                record.setRoundTripTimeMs(roundTripTimeMs);
                saveToFile();
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all recorded analytics history and updates the persistence file.
     */
    public void clearAnalytics() {
        synchronized (lock) {
            records.clear();
            recordMap.clear();
            saveToFile();
        }
    }

    private void addRecordInternal(SearchRecord record) {
        records.add(record);
        recordMap.put(record.getId(), record);

        // Prune oldest if over maximum capacity
        if (records.size() > MAX_RECORD_CAPACITY) {
            SearchRecord removed = records.remove(0);
            if (removed != null) {
                recordMap.remove(removed.getId());
            }
        }
    }

    /**
     * Generates comprehensive analytics JSON payload for GET /api/analytics.
     */
    public String getAnalyticsJson() {
        synchronized (lock) {
            int total = records.size();

            long totalExecTime = 0;
            long totalRttTime = 0;
            int rttCount = 0;

            int countCoverage = 0;
            int countBm25 = 0;
            int countBaseline = 0;
            int countSemantic = 0;
            int countCompare = 0;

            long execCoverage = 0;
            long execBm25 = 0;
            long execBaseline = 0;
            long execSemantic = 0;
            long execCompare = 0;

            for (SearchRecord r : records) {
                totalExecTime += r.getExecutionTimeMs();
                if (r.getRoundTripTimeMs() > 0) {
                    totalRttTime += r.getRoundTripTimeMs();
                    rttCount++;
                }

                if (r.isComparison() || "compare".equalsIgnoreCase(r.getAlgo())) {
                    countCompare++;
                    execCompare += r.getExecutionTimeMs();
                } else if ("semantic".equalsIgnoreCase(r.getAlgo())) {
                    countSemantic++;
                    execSemantic += r.getExecutionTimeMs();
                } else if ("bm25".equalsIgnoreCase(r.getAlgo())) {
                    countBm25++;
                    execBm25 += r.getExecutionTimeMs();
                } else if ("baseline".equalsIgnoreCase(r.getAlgo())) {
                    countBaseline++;
                    execBaseline += r.getExecutionTimeMs();
                } else {
                    countCoverage++;
                    execCoverage += r.getExecutionTimeMs();
                }
            }

            double avgExec = total > 0 ? (double) totalExecTime / total : 0.0;
            double avgRtt = rttCount > 0 ? (double) totalRttTime / rttCount : 0.0;

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"totalSearches\":").append(total).append(",");
            sb.append("\"averageExecutionTimeMs\":").append(String.format(Locale.US, "%.2f", avgExec)).append(",");
            sb.append("\"averageRoundTripTimeMs\":").append(String.format(Locale.US, "%.2f", avgRtt)).append(",");

            // Algorithm breakdown
            sb.append("\"algorithmStats\":{");
            sb.append("\"coverage\":{");
            sb.append("\"key\":\"coverage\",");
            sb.append("\"name\":\"Coverage-Aware TF-IDF\",");
            sb.append("\"count\":").append(countCoverage).append(",");
            sb.append("\"percentage\":").append(String.format(Locale.US, "%.1f", total > 0 ? ((double) countCoverage / total) * 100.0 : 0.0)).append(",");
            sb.append("\"avgExecutionTimeMs\":").append(String.format(Locale.US, "%.2f", countCoverage > 0 ? (double) execCoverage / countCoverage : 0.0));
            sb.append("},");

            sb.append("\"semantic\":{");
            sb.append("\"key\":\"semantic\",");
            sb.append("\"name\":\"Semantic Search (LSA)\",");
            sb.append("\"count\":").append(countSemantic).append(",");
            sb.append("\"percentage\":").append(String.format(Locale.US, "%.1f", total > 0 ? ((double) countSemantic / total) * 100.0 : 0.0)).append(",");
            sb.append("\"avgExecutionTimeMs\":").append(String.format(Locale.US, "%.2f", countSemantic > 0 ? (double) execSemantic / countSemantic : 0.0));
            sb.append("},");

            sb.append("\"bm25\":{");
            sb.append("\"key\":\"bm25\",");
            sb.append("\"name\":\"Okapi BM25\",");
            sb.append("\"count\":").append(countBm25).append(",");
            sb.append("\"percentage\":").append(String.format(Locale.US, "%.1f", total > 0 ? ((double) countBm25 / total) * 100.0 : 0.0)).append(",");
            sb.append("\"avgExecutionTimeMs\":").append(String.format(Locale.US, "%.2f", countBm25 > 0 ? (double) execBm25 / countBm25 : 0.0));
            sb.append("},");

            sb.append("\"baseline\":{");
            sb.append("\"key\":\"baseline\",");
            sb.append("\"name\":\"Baseline TF-IDF\",");
            sb.append("\"count\":").append(countBaseline).append(",");
            sb.append("\"percentage\":").append(String.format(Locale.US, "%.1f", total > 0 ? ((double) countBaseline / total) * 100.0 : 0.0)).append(",");
            sb.append("\"avgExecutionTimeMs\":").append(String.format(Locale.US, "%.2f", countBaseline > 0 ? (double) execBaseline / countBaseline : 0.0));
            sb.append("},");

            sb.append("\"compare\":{");
            sb.append("\"key\":\"compare\",");
            sb.append("\"name\":\"3-Way Comparison Lab\",");
            sb.append("\"count\":").append(countCompare).append(",");
            sb.append("\"percentage\":").append(String.format(Locale.US, "%.1f", total > 0 ? ((double) countCompare / total) * 100.0 : 0.0)).append(",");
            sb.append("\"avgExecutionTimeMs\":").append(String.format(Locale.US, "%.2f", countCompare > 0 ? (double) execCompare / countCompare : 0.0));
            sb.append("}");
            sb.append("},");

            // Recent searches (newest first, up to 50)
            sb.append("\"recentSearches\":[");
            int recentLimit = Math.min(50, records.size());
            for (int i = 0; i < recentLimit; i++) {
                // Read from end of list
                SearchRecord r = records.get(records.size() - 1 - i);
                if (i > 0) sb.append(",");
                sb.append(r.toJson());
            }
            sb.append("],");

            // Latency History (chronological order, last 30, for chart plotting)
            sb.append("\"latencyHistory\":[");
            int historyLimit = Math.min(30, records.size());
            int startIndex = Math.max(0, records.size() - historyLimit);
            for (int i = startIndex; i < records.size(); i++) {
                SearchRecord r = records.get(i);
                if (i > startIndex) sb.append(",");
                sb.append("{");
                sb.append("\"id\":\"").append(escapeJson(r.getId())).append("\",");
                sb.append("\"query\":\"").append(escapeJson(r.getQuery())).append("\",");
                sb.append("\"algo\":\"").append(escapeJson(r.getAlgo())).append("\",");
                sb.append("\"algoLabel\":\"").append(escapeJson(r.getAlgoLabel())).append("\",");
                sb.append("\"executionTimeMs\":").append(r.getExecutionTimeMs()).append(",");
                sb.append("\"roundTripTimeMs\":").append(r.getRoundTripTimeMs()).append(",");
                sb.append("\"timestamp\":").append(r.getTimestamp());
                sb.append("}");
            }
            sb.append("]");

            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Persists in-memory search records to the local JSON file.
     */
    private void saveToFile() {
        try {
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < records.size(); i++) {
                if (i > 0) sb.append(",\n");
                sb.append("  ").append(records.get(i).toJson());
            }
            sb.append("\n]\n");

            // Atomic file write using temporary file to prevent corruption
            File tempFile = new File(storageFile.getAbsolutePath() + ".tmp");
            Files.writeString(tempFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
            Files.move(tempFile.toPath(), storageFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            System.err.println("Failed to persist analytics to " + storageFile.getPath() + ": " + e.getMessage());
        }
    }

    /**
     * Loads search records from the local JSON file if it exists.
     */
    private void loadFromFile() {
        if (!storageFile.exists() || storageFile.length() == 0) {
            return;
        }

        try {
            String content = Files.readString(storageFile.toPath(), StandardCharsets.UTF_8).trim();
            if (content.isEmpty() || !content.startsWith("[")) {
                return;
            }

            // Simple robust JSON record extractor
            List<SearchRecord> loaded = parseRecordsJson(content);
            synchronized (lock) {
                records.clear();
                recordMap.clear();
                for (SearchRecord r : loaded) {
                    addRecordInternal(r);
                }
            }
            System.out.println("AnalyticsManager: Loaded " + records.size() + " persisted search records from " + storageFile.getName());
        } catch (Exception e) {
            System.err.println("AnalyticsManager: Could not load persistence file: " + e.getMessage());
        }
    }

    /**
     * Zero-dependency parser for persisted SearchRecord JSON array.
     */
    private static List<SearchRecord> parseRecordsJson(String json) {
        List<SearchRecord> list = new ArrayList<>();
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
                String objJson = json.substring(start, end + 1);
                SearchRecord record = parseSingleRecord(objJson);
                if (record != null) {
                    list.add(record);
                }
                i = end + 1;
            } else {
                break;
            }
        }

        return list;
    }

    private static SearchRecord parseSingleRecord(String s) {
        try {
            String id = extractStringField(s, "id");
            String query = extractStringField(s, "query");
            String algo = extractStringField(s, "algo");
            String category = extractStringField(s, "category");
            long timestamp = extractLongField(s, "timestamp", System.currentTimeMillis());
            long executionTimeMs = extractLongField(s, "executionTimeMs", 0);
            long roundTripTimeMs = extractLongField(s, "roundTripTimeMs", 0);
            int totalMatches = (int) extractLongField(s, "totalMatches", 0);
            boolean isComparison = extractBooleanField(s, "isComparison", false);

            if (id == null || id.isEmpty()) {
                return null;
            }

            return new SearchRecord(
                    id, query, algo, category, timestamp,
                    executionTimeMs, roundTripTimeMs, totalMatches, isComparison
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractStringField(String json, String key) {
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

    private static long extractLongField(String json, String key, long defaultVal) {
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

    private static boolean extractBooleanField(String json, String key, boolean defaultVal) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return defaultVal;
        start += pattern.length();

        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }

        if (json.startsWith("true", start)) return true;
        if (json.startsWith("false", start)) return false;
        return defaultVal;
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
