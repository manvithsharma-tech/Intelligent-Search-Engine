package comparison;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe manager for recording and persisting manual comparison evaluations
 * between the local search engine and Google Search.
 */
public class GoogleComparisonManager {

    private static final String DEFAULT_FILE = "google_evals.json";
    private static volatile GoogleComparisonManager instance;

    public static synchronized GoogleComparisonManager getInstance() {
        if (instance == null) {
            instance = new GoogleComparisonManager(new File(DEFAULT_FILE));
        }
        return instance;
    }

    private final File storageFile;
    private final List<GoogleComparisonRecord> records;
    private final Map<String, GoogleComparisonRecord> recordMap;
    private final AtomicLong idCounter;
    private final Object lock = new Object();

    public GoogleComparisonManager(File storageFile) {
        this.storageFile = storageFile;
        this.records = new ArrayList<>();
        this.recordMap = new HashMap<>();
        this.idCounter = new AtomicLong(System.currentTimeMillis() % 1000000);
        loadFromFile();
    }

    /**
     * Adds and persists a new comparison observation.
     */
    public GoogleComparisonRecord addRecord(String query, String engine, String localAlgo,
                                            String category, int relevantCount, String notes,
                                            long responseTimeMs) {
        if (query == null || query.trim().isEmpty()) {
            return null;
        }

        String id = "gcomp_" + System.currentTimeMillis() + "_" + idCounter.incrementAndGet();
        GoogleComparisonRecord record = new GoogleComparisonRecord(
                id,
                query.trim(),
                engine,
                localAlgo,
                category,
                relevantCount,
                notes,
                responseTimeMs,
                System.currentTimeMillis()
        );

        synchronized (lock) {
            records.add(0, record); // newest first
            recordMap.put(record.getId(), record);
            saveToFile();
        }

        return record;
    }

    /**
     * Deletes a record by ID.
     */
    public boolean deleteRecord(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        synchronized (lock) {
            GoogleComparisonRecord removed = recordMap.remove(id.trim());
            if (removed != null) {
                records.remove(removed);
                saveToFile();
                return true;
            }
        }
        return false;
    }

    /**
     * Clears all evaluation observations.
     */
    public void clearRecords() {
        synchronized (lock) {
            records.clear();
            recordMap.clear();
            saveToFile();
        }
    }

    public List<GoogleComparisonRecord> getRecords() {
        synchronized (lock) {
            return new ArrayList<>(records);
        }
    }

    /**
     * Returns a JSON representation of all saved observations with summary analytics.
     */
    public String getEvaluationSummaryJson() {
        synchronized (lock) {
            int total = records.size();
            int localCount = 0;
            double localPrecSum = 0;
            int googleCount = 0;
            double googlePrecSum = 0;

            for (GoogleComparisonRecord r : records) {
                if ("local".equalsIgnoreCase(r.getEngine())) {
                    localCount++;
                    localPrecSum += r.getPrecisionAt5();
                } else if ("google".equalsIgnoreCase(r.getEngine())) {
                    googleCount++;
                    googlePrecSum += r.getPrecisionAt5();
                }
            }

            double localAvgPrec = localCount > 0 ? (localPrecSum / localCount) : 0.0;
            double googleAvgPrec = googleCount > 0 ? (googlePrecSum / googleCount) : 0.0;

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"totalObservations\":").append(total).append(",");
            sb.append("\"localObservationsCount\":").append(localCount).append(",");
            sb.append("\"googleObservationsCount\":").append(googleCount).append(",");
            sb.append("\"localAvgPrecisionAt5\":").append(String.format(Locale.US, "%.4f", localAvgPrec)).append(",");
            sb.append("\"googleAvgPrecisionAt5\":").append(String.format(Locale.US, "%.4f", googleAvgPrec)).append(",");
            sb.append("\"observations\":[");

            for (int i = 0; i < records.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(records.get(i).toJson());
            }

            sb.append("]");
            sb.append("}");
            return sb.toString();
        }
    }

    private void saveToFile() {
        File tempFile = new File(storageFile.getAbsolutePath() + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), StandardCharsets.UTF_8))) {
            writer.write(getEvaluationSummaryJson());
            writer.flush();
        } catch (IOException e) {
            System.err.println("GoogleComparisonManager: Failed to write temp file: " + e.getMessage());
            return;
        }

        try {
            Files.move(tempFile.toPath(), storageFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            if (tempFile.renameTo(storageFile)) {
                return;
            }
            System.err.println("GoogleComparisonManager: Failed to persist to " + storageFile.getName() + ": " + e.getMessage());
        }
    }

    private void loadFromFile() {
        if (!storageFile.exists() || storageFile.length() == 0) {
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(storageFile.toPath());
            String json = new String(bytes, StandardCharsets.UTF_8).trim();
            if (json.isEmpty()) return;

            int obsIdx = json.indexOf("\"observations\":[");
            if (obsIdx == -1) return;

            int start = obsIdx + 16;
            int end = json.lastIndexOf("]");
            if (end <= start) return;

            String arrayContent = json.substring(start, end).trim();
            if (arrayContent.isEmpty()) return;

            List<String> objects = splitJsonObjects(arrayContent);
            for (String objStr : objects) {
                GoogleComparisonRecord record = parseRecord(objStr);
                if (record != null) {
                    records.add(record);
                    recordMap.put(record.getId(), record);
                }
            }
            System.out.println("GoogleComparisonManager: Loaded " + records.size()
                    + " persisted observations from " + storageFile.getName());
        } catch (Exception e) {
            System.err.println("GoogleComparisonManager: Error loading from " + storageFile.getName() + ": " + e.getMessage());
        }
    }

    private List<String> splitJsonObjects(String str) {
        List<String> list = new ArrayList<>();
        int braceDepth = 0;
        int start = -1;
        boolean inQuotes = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"' && (i == 0 || str.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') {
                    if (braceDepth == 0) start = i;
                    braceDepth++;
                } else if (c == '}') {
                    braceDepth--;
                    if (braceDepth == 0 && start != -1) {
                        list.add(str.substring(start, i + 1));
                        start = -1;
                    }
                }
            }
        }
        return list;
    }

    private GoogleComparisonRecord parseRecord(String json) {
        try {
            String id = extractField(json, "id");
            String query = extractField(json, "query");
            String engine = extractField(json, "engine");
            String localAlgo = extractField(json, "localAlgo");
            String category = extractField(json, "category");
            String notes = extractField(json, "notes");

            int relevantCount = (int) extractDoubleField(json, "relevantCount", 0);
            long responseTimeMs = (long) extractDoubleField(json, "responseTimeMs", 0);
            long timestamp = (long) extractDoubleField(json, "timestamp", System.currentTimeMillis());

            return new GoogleComparisonRecord(id, query, engine, localAlgo, category, relevantCount, notes, responseTimeMs, timestamp);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractField(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) {
            String numPattern = "\"" + key + "\":";
            int numIdx = json.indexOf(numPattern);
            if (numIdx == -1) return "";
            int start = numIdx + numPattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            if (end == -1) return "";
            return json.substring(start, end).trim();
        }
        int start = idx + pattern.length();
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    default: sb.append(c); break;
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private double extractDoubleField(String json, String key, double defaultVal) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return defaultVal;
        int start = idx + pattern.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return defaultVal;
        try {
            return Double.parseDouble(json.substring(start, end).replace("\"", "").trim());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }
}
