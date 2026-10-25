package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import analytics.AnalyticsManager;
import analytics.SearchRecord;
import comparison.GoogleComparisonManager;
import comparison.GoogleComparisonRecord;
import workspace.WorkspaceManager;
import evaluation.SearchEvaluator;
import index.InvertedIndex;
import model.Document;
import preprocessing.DocumentReader;
import preprocessing.TextPreprocessor;
import ranking.RankingEngine;
import semantic.SemanticResult;
import semantic.SemanticSearchEngine;
import trie.Trie;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Lightweight, zero-dependency HTTP server powering the Intelligent Search Engine.
 * Built using JDK's built-in com.sun.net.httpserver.HttpServer.
 */
public class SearchServer {

    private static final int DEFAULT_PORT = 8080;
    private static final String WEB_DIR = "web";

    private final List<Document> documents;
    private final Map<String, Document> documentMap;
    private final Map<String, Integer> categoryCounts;
    private final InvertedIndex invertedIndex;
    private final Trie trie;
    private final RankingEngine rankingEngine;
    private final SemanticSearchEngine semanticSearchEngine;
    private final SearchEvaluator evaluator;
    private final AnalyticsManager analyticsManager;
    private final WorkspaceManager workspaceManager;
    private final GoogleComparisonManager googleComparisonManager;
    private final int port;

    public SearchServer(int port) {
        this.port = port;
        this.analyticsManager = AnalyticsManager.getInstance();
        this.workspaceManager = WorkspaceManager.getInstance();
        this.googleComparisonManager = GoogleComparisonManager.getInstance();

        System.out.println("=================================================");
        System.out.println("INITIALIZING INTELLIGENT SEARCH ENGINE BACKEND");
        System.out.println("=================================================");

        // 1. Read Dataset
        long startTime = System.currentTimeMillis();
        DocumentReader reader = new DocumentReader();
        this.documents = reader.readDocuments();
        System.out.println("Loaded " + documents.size() + " documents across multi-domain dataset.");

        this.documentMap = new HashMap<>(131072, 0.75f);
        this.categoryCounts = new TreeMap<>();
        for (Document doc : documents) {
            documentMap.put(doc.getId(), doc);
            documentMap.put(doc.getCategory().toLowerCase() + ":" + doc.getId(), doc);
            categoryCounts.put(doc.getCategory(),
                    categoryCounts.getOrDefault(doc.getCategory(), 0) + 1);
        }

        // 2. Build Inverted Index and Compressed Trie
        this.invertedIndex = new InvertedIndex();
        this.trie = new Trie();
        TextPreprocessor preprocessor = new TextPreprocessor();

        for (Document document : documents) {
            List<String> words = preprocessor.preprocess(document.getContent());
            document.setWordCount(words.size());
            invertedIndex.addDocument(document, words);
        }
        invertedIndex.finalizeIndex();

        for (String term : invertedIndex.getTerms()) {
            trie.insert(term);
        }

        System.out.println("Indexed " + invertedIndex.getNumberOfTerms() + " unique vocabulary terms.");
        System.out.println("Compressed Trie initialized and loaded with vocabulary.");

        // 3. Initialize Ranking Engine & Evaluator
        this.rankingEngine = new RankingEngine(documents, invertedIndex);
        this.evaluator = new SearchEvaluator();

        // 4. Initialize LSA Semantic Search Engine (128-D Concept Space)
        this.semanticSearchEngine = new SemanticSearchEngine(documents, documentMap);
        if (semanticSearchEngine.isAvailable()) {
            System.out.println("Semantic Search Engine (LSA 128-D) initialized and active ("
                    + semanticSearchEngine.getTotalDocuments() + " documents, "
                    + semanticSearchEngine.getTotalVocabulary() + " vocabulary).");
        } else {
            System.out.println("Semantic Search Engine unavailable: " + semanticSearchEngine.getInitError());
        }

        long loadTime = System.currentTimeMillis() - startTime;
        System.out.println("Engine initialized in " + loadTime + " ms.");
        System.out.println("=================================================");
    }

    public void start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));

        // REST API routes
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/api/suggest", new SuggestHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/document", new DocumentHandler());
        server.createContext("/api/evaluate", new EvaluateHandler());
        server.createContext("/api/analytics", new AnalyticsHandler());
        server.createContext("/api/workspace", new WorkspaceHandler());
        server.createContext("/api/google-eval", new GoogleComparisonHandler());

        // Static files handler (serves web/ directory)
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("Search Engine Web Server is running at: http://localhost:" + port + "/");
        System.out.println("Ready to serve requests.");
    }

    // =========================================================================
    // API HANDLERS
    // =========================================================================

    /**
     * GET /api/stats
     * Returns real dataset statistics: document count, vocabulary count, category distribution.
     */
    private class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"status\":\"ready\",");
            sb.append("\"totalDocuments\":").append(documents.size()).append(",");
            sb.append("\"totalTerms\":").append(invertedIndex.getNumberOfTerms()).append(",");
            sb.append("\"semanticEngine\":{");
            sb.append("\"available\":").append(semanticSearchEngine.isAvailable()).append(",");
            sb.append("\"dimensions\":").append(semanticSearchEngine.getDimensions()).append(",");
            sb.append("\"vocabularySize\":").append(semanticSearchEngine.getTotalVocabulary());
            sb.append("},");
            sb.append("\"categories\":[");
            int cIndex = 0;
            for (String cat : categoryCounts.keySet()) {
                if (cIndex++ > 0) sb.append(",");
                sb.append("\"").append(escapeJson(cat)).append("\"");
            }
            sb.append("],");
            sb.append("\"categoryCounts\":{");
            int countIndex = 0;
            for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                if (countIndex++ > 0) sb.append(",");
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":").append(entry.getValue());
            }
            sb.append("}");
            sb.append("}");

            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    /**
     * GET /api/suggest?q=crick
     * Live prefix suggestions powered by the Compressed Trie.
     */
    private class SuggestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            Map<String, String> params = parseQueryParams(exchange.getRequestURI());
            String query = params.getOrDefault("q", "").trim();

            List<String> suggestions = new ArrayList<>();
            if (!query.isEmpty()) {
                suggestions = trie.getWordsWithPrefix(query.toLowerCase());
                // Limit to top 10 suggestions for responsive UI
                if (suggestions.size() > 10) {
                    suggestions = suggestions.subList(0, 10);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"prefix\":\"").append(escapeJson(query)).append("\",");
            sb.append("\"totalSuggestions\":").append(suggestions.size()).append(",");
            sb.append("\"suggestions\":[");
            for (int i = 0; i < suggestions.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(suggestions.get(i))).append("\"");
            }
            sb.append("]");
            sb.append("}");

            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    /**
     * GET /api/search?q=cricket+t20&category=sport&algo=coverage&limit=10
     * Multi-algorithm ranked search (coverage, baseline, bm25) with explainability metrics.
     */
    private class SearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            try {
                long start = System.currentTimeMillis();
                Map<String, String> params = parseQueryParams(exchange.getRequestURI());
                String query = params.getOrDefault("q", "").trim();
                String category = params.getOrDefault("category", "all").trim();
                String algo = params.getOrDefault("algo", "coverage").trim().toLowerCase();
                if (!algo.equals("bm25") && !algo.equals("baseline") && !algo.equals("semantic") && !algo.equals("coverage")) {
                    algo = "coverage";
                }

                int limit = 10;
                try {
                    if (params.containsKey("limit")) {
                        limit = Math.max(1, Math.min(100, Integer.parseInt(params.get("limit"))));
                    }
                } catch (NumberFormatException ignored) {}

                // Branch for Semantic Search (LSA Concept Space)
                if ("semantic".equals(algo)) {
                    if (!semanticSearchEngine.isAvailable()) {
                        sendJsonResponse(exchange, 503, "{\"error\":\"Semantic Search Model is unavailable: "
                                + escapeJson(semanticSearchEngine.getInitError()) + "\"}");
                        return;
                    }

                    List<SemanticResult> semanticResults = query.isEmpty()
                            ? Collections.emptyList()
                            : semanticSearchEngine.search(query, category, limit);

                    int totalMatches = semanticResults.size();
                    int returnCount = Math.min(limit, semanticResults.size());
                    List<SemanticResult> topResults = semanticResults.subList(0, returnCount);
                    long duration = System.currentTimeMillis() - start;

                    SearchRecord searchRecord = null;
                    if (!query.isEmpty()) {
                        searchRecord = analyticsManager.recordSingleSearch(query, category, "semantic", duration, totalMatches);
                    }

                    StringBuilder sb = new StringBuilder();
                    sb.append("{");
                    sb.append("\"query\":\"").append(escapeJson(query)).append("\",");
                    sb.append("\"category\":\"").append(escapeJson(category)).append("\",");
                    sb.append("\"algo\":\"semantic\",");
                    sb.append("\"algoLabel\":\"Semantic Search (LSA)\",");
                    if (searchRecord != null) {
                        sb.append("\"searchId\":\"").append(escapeJson(searchRecord.getId())).append("\",");
                    }
                    sb.append("\"totalValidTerms\":").append(query.isEmpty() ? 0 : query.trim().split("\\s+").length).append(",");
                    sb.append("\"totalMatches\":").append(totalMatches).append(",");
                    sb.append("\"returnedCount\":").append(returnCount).append(",");
                    sb.append("\"executionTimeMs\":").append(duration).append(",");
                    sb.append("\"results\":[");

                    for (int i = 0; i < topResults.size(); i++) {
                        SemanticResult res = topResults.get(i);
                        Document doc = res.getDocument();
                        if (i > 0) sb.append(",");
                        sb.append("{");
                        sb.append("\"rank\":").append(res.getRank()).append(",");
                        sb.append("\"id\":\"").append(escapeJson(doc.getId())).append("\",");
                        sb.append("\"category\":\"").append(escapeJson(doc.getCategory())).append("\",");
                        sb.append("\"fileName\":\"").append(escapeJson(doc.getFileName())).append("\",");
                        sb.append("\"score\":").append(String.format(Locale.US, "%.6f", res.getScore())).append(",");
                        sb.append("\"scoreLabel\":\"Semantic Sim\",");
                        sb.append("\"explanation\":\"").append(escapeJson(res.getExplanation())).append("\",");
                        sb.append("\"topConcepts\":[");
                        for (int c = 0; c < res.getTopConcepts().size(); c++) {
                            if (c > 0) sb.append(",");
                            sb.append("\"").append(escapeJson(res.getTopConcepts().get(c))).append("\"");
                        }
                        sb.append("],");
                        sb.append("\"relatedKeywords\":[");
                        for (int k = 0; k < res.getRelatedKeywords().size(); k++) {
                            if (k > 0) sb.append(",");
                            sb.append("\"").append(escapeJson(res.getRelatedKeywords().get(k))).append("\"");
                        }
                        sb.append("],");
                        sb.append("\"snippet\":\"").append(escapeJson(res.getSnippet())).append("\"");
                        sb.append("}");
                    }

                    sb.append("]");
                    sb.append("}");

                    sendJsonResponse(exchange, 200, sb.toString());
                    return;
                }

                List<Document> ranked = query.isEmpty()
                        ? Collections.emptyList()
                        : rankingEngine.rank(query, algo);

                // Filter by category if requested
                List<Document> filtered = new ArrayList<>();
                for (Document doc : ranked) {
                    if (category.equalsIgnoreCase("all") || doc.getCategory().equalsIgnoreCase(category)) {
                        filtered.add(doc);
                    }
                }

                int totalMatches = filtered.size();
                int returnCount = Math.min(limit, filtered.size());
                List<Document> topDocs = filtered.subList(0, returnCount);
                int totalValidTerms = rankingEngine.getValidTermsCount(query);

                long duration = System.currentTimeMillis() - start;

                String mode = params.getOrDefault("mode", "single").trim().toLowerCase();
                String compareId = params.getOrDefault("compareId", "").trim();
                SearchRecord searchRecord = null;
                if (!query.isEmpty()) {
                    if ("compare".equals(mode) && !compareId.isEmpty()) {
                        searchRecord = analyticsManager.recordComparisonSubquery(compareId, query, category, algo, duration, totalMatches);
                    } else {
                        searchRecord = analyticsManager.recordSingleSearch(query, category, algo, duration, totalMatches);
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"query\":\"").append(escapeJson(query)).append("\",");
                sb.append("\"category\":\"").append(escapeJson(category)).append("\",");
                sb.append("\"algo\":\"").append(escapeJson(algo)).append("\",");
                if (searchRecord != null) {
                    sb.append("\"searchId\":\"").append(escapeJson(searchRecord.getId())).append("\",");
                }
                sb.append("\"totalValidTerms\":").append(totalValidTerms).append(",");
                sb.append("\"totalMatches\":").append(totalMatches).append(",");
                sb.append("\"returnedCount\":").append(returnCount).append(",");
                sb.append("\"executionTimeMs\":").append(duration).append(",");
                sb.append("\"results\":[");

                for (int i = 0; i < topDocs.size(); i++) {
                    Document doc = topDocs.get(i);
                    double score = rankingEngine.getScore(query, doc, algo);
                    String snippet = rankingEngine.getSnippet(query, doc);
                    List<String> matchedTerms = rankingEngine.getMatchedTerms(query, doc);
                    int matchedCount = matchedTerms.size();
                    double coveragePct = totalValidTerms > 0 ? ((double) matchedCount / totalValidTerms) * 100.0 : 0.0;

                    if (i > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"rank\":").append(i + 1).append(",");
                    sb.append("\"id\":\"").append(escapeJson(doc.getId())).append("\",");
                    sb.append("\"category\":\"").append(escapeJson(doc.getCategory())).append("\",");
                    sb.append("\"fileName\":\"").append(escapeJson(doc.getFileName())).append("\",");
                    sb.append("\"score\":").append(String.format(Locale.US, "%.6f", score)).append(",");
                    sb.append("\"matchedTermsCount\":").append(matchedCount).append(",");
                    sb.append("\"totalValidTerms\":").append(totalValidTerms).append(",");
                    sb.append("\"coveragePercentage\":").append(String.format(Locale.US, "%.1f", coveragePct)).append(",");
                    sb.append("\"matchedTerms\":[");
                    for (int t = 0; t < matchedTerms.size(); t++) {
                        if (t > 0) sb.append(",");
                        sb.append("\"").append(escapeJson(matchedTerms.get(t))).append("\"");
                    }
                    sb.append("],");
                    sb.append("\"snippet\":\"").append(escapeJson(snippet)).append("\"");
                    sb.append("}");
                }

                sb.append("]");
                sb.append("}");

                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                System.err.println("SearchHandler exception: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"Search operation failed\"}");
            }
        }
    }

    /**
     * GET /api/document?id=402
     * Returns full content of a document by ID for previewing articles.
     */
    private class DocumentHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI());
                String id = params.getOrDefault("id", "").trim();
                String category = params.getOrDefault("category", "").trim();

                Document doc = null;
                if (!category.isEmpty()) {
                    doc = documentMap.get(category.toLowerCase() + ":" + id);
                }
                if (doc == null) {
                    doc = documentMap.get(id);
                }
                if (doc == null) {
                    sendJsonResponse(exchange, 404, "{\"error\":\"Document not found\"}");
                    return;
                }

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"id\":\"").append(escapeJson(doc.getId())).append("\",");
                sb.append("\"category\":\"").append(escapeJson(doc.getCategory())).append("\",");
                sb.append("\"fileName\":\"").append(escapeJson(doc.getFileName())).append("\",");
                sb.append("\"content\":\"").append(escapeJson(doc.getContent())).append("\"");
                sb.append("}");

                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                System.err.println("DocumentHandler exception: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"Document retrieval failed\"}");
            }
        }
    }

    /**
     * GET /api/evaluate?q=cricket+t20&category=sport&algo=coverage
     * Computes search evaluation metrics: Precision, Recall, and F1-score across algorithms.
     */
    private class EvaluateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            try {
                Map<String, String> params = parseQueryParams(exchange.getRequestURI());
                String query = params.getOrDefault("q", "").trim();
                String category = params.getOrDefault("category", "sport").trim();
                String algo = params.getOrDefault("algo", "coverage").trim().toLowerCase();
                if (!algo.equals("bm25") && !algo.equals("baseline") && !algo.equals("semantic") && !algo.equals("coverage")) {
                    algo = "coverage";
                }

                List<Document> ranked;
                if ("semantic".equals(algo)) {
                    List<SemanticResult> sRes = query.isEmpty()
                            ? Collections.emptyList()
                            : semanticSearchEngine.search(query, category, 100);
                    ranked = new ArrayList<>(sRes.size());
                    for (SemanticResult sr : sRes) {
                        ranked.add(sr.getDocument());
                    }
                } else {
                    ranked = query.isEmpty()
                            ? Collections.emptyList()
                            : rankingEngine.rank(query, algo);
                }

                double precision = evaluator.calculatePrecision(ranked, category);
                double recall = evaluator.calculateRecall(ranked, documents, category);
                double f1 = evaluator.calculateF1(precision, recall);

                StringBuilder sb = new StringBuilder();
                sb.append("{");
                sb.append("\"query\":\"").append(escapeJson(query)).append("\",");
                sb.append("\"expectedCategory\":\"").append(escapeJson(category)).append("\",");
                sb.append("\"algo\":\"").append(escapeJson(algo)).append("\",");
                sb.append("\"totalRetrieved\":").append(ranked.size()).append(",");
                sb.append("\"precision\":").append(String.format(Locale.US, "%.4f", precision)).append(",");
                sb.append("\"recall\":").append(String.format(Locale.US, "%.4f", recall)).append(",");
                sb.append("\"f1Score\":").append(String.format(Locale.US, "%.4f", f1));
                sb.append("}");

                sendJsonResponse(exchange, 200, sb.toString());
            } catch (Exception e) {
                System.err.println("EvaluateHandler exception: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"Evaluation failed\"}");
            }
        }
    }

    /**
     * GET /api/analytics
     * Returns live search engine analytics KPIs, algorithm usage breakdown, recent searches,
     * and latency series.
     *
     * POST /api/analytics/rtt
     * Updates recorded search record with browser round-trip time.
     *
     * POST /api/analytics/clear
     * Resets analytics history and updates storage file.
     */
    private class AnalyticsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            try {
                String method = exchange.getRequestMethod().toUpperCase();
                String path = exchange.getRequestURI().getPath();
                Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());

                if ("GET".equals(method)) {
                    String json = analyticsManager.getAnalyticsJson();
                    sendJsonResponse(exchange, 200, json);
                    return;
                }

                if ("POST".equals(method)) {
                    String body = readRequestBody(exchange);
                    Map<String, String> bodyParams = parseSimpleJson(body);

                    String action = queryParams.getOrDefault("action", bodyParams.getOrDefault("action", ""));
                    if (path.endsWith("/clear") || "clear".equalsIgnoreCase(action)) {
                        analyticsManager.clearAnalytics();
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"cleared\":true}");
                        return;
                    }

                    if (path.endsWith("/rtt") || "rtt".equalsIgnoreCase(action) || bodyParams.containsKey("searchId") || queryParams.containsKey("searchId")) {
                        String searchId = queryParams.getOrDefault("searchId", bodyParams.getOrDefault("searchId", ""));
                        long rtt = 0;
                        try {
                            String rttStr = queryParams.getOrDefault("roundTripTimeMs",
                                    bodyParams.getOrDefault("roundTripTimeMs",
                                    queryParams.getOrDefault("rtt", bodyParams.getOrDefault("rtt", "0"))));
                            rtt = Long.parseLong(rttStr);
                        } catch (NumberFormatException ignored) {}

                        boolean updated = analyticsManager.updateRoundTripTime(searchId, rtt);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"updated\":" + updated + "}");
                        return;
                    }

                    sendJsonResponse(exchange, 200, "{\"status\":\"ok\"}");
                    return;
                }

                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            } catch (Exception e) {
                System.err.println("AnalyticsHandler exception: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"Analytics processing failed\"}");
            }
        }
    }

    /**
     * Research Workspace REST API
     * GET /api/workspace -> returns full workspace JSON (collections, bookmarks, saved searches)
     * POST /api/workspace -> dispatches workspace mutations:
     *   - addBookmark (docId, category, collectionId, notes)
     *   - removeBookmark (docId)
     *   - updateNotes (docId, notes)
     *   - assignCollection (docId, collectionId)
     *   - createCollection (name)
     *   - renameCollection (id, name)
     *   - deleteCollection (id)
     *   - saveSearch (query, algo, category)
     *   - deleteSearch (id)
     *   - clear
     */
    private class WorkspaceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            try {
                String method = exchange.getRequestMethod().toUpperCase();
                Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());

                if ("GET".equals(method)) {
                    String json = workspaceManager.getWorkspaceJson();
                    sendJsonResponse(exchange, 200, json);
                    return;
                }

                if ("POST".equals(method)) {
                    String body = readRequestBody(exchange);
                    Map<String, String> bodyParams = parseSimpleJson(body);

                    String action = queryParams.getOrDefault("action", bodyParams.getOrDefault("action", "")).trim();

                    if ("addBookmark".equalsIgnoreCase(action)) {
                        String docId = queryParams.getOrDefault("docId", bodyParams.getOrDefault("docId", "")).trim();
                        String category = queryParams.getOrDefault("category", bodyParams.getOrDefault("category", "all")).trim();
                        String collectionId = queryParams.getOrDefault("collectionId", bodyParams.getOrDefault("collectionId", "default")).trim();
                        String notes = queryParams.getOrDefault("notes", bodyParams.getOrDefault("notes", "")).trim();

                        if (docId.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Document ID is required\"}");
                            return;
                        }

                        workspace.Bookmark bm = workspaceManager.addBookmark(docId, category, collectionId, notes, documentMap);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"bookmarked\":true,\"docId\":\"" + escapeJson(bm.getDocId()) + "\"}");
                        return;
                    }

                    if ("removeBookmark".equalsIgnoreCase(action)) {
                        String docId = queryParams.getOrDefault("docId", bodyParams.getOrDefault("docId", "")).trim();
                        if (docId.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Document ID is required\"}");
                            return;
                        }

                        boolean removed = workspaceManager.removeBookmark(docId);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"removed\":" + removed + "}");
                        return;
                    }

                    if ("updateNotes".equalsIgnoreCase(action)) {
                        String docId = queryParams.getOrDefault("docId", bodyParams.getOrDefault("docId", "")).trim();
                        String notes = queryParams.getOrDefault("notes", bodyParams.getOrDefault("notes", ""));
                        if (docId.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Document ID is required\"}");
                            return;
                        }

                        boolean updated = workspaceManager.updateNotes(docId, notes);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"updated\":" + updated + "}");
                        return;
                    }

                    if ("assignCollection".equalsIgnoreCase(action) || "setCollection".equalsIgnoreCase(action)) {
                        String docId = queryParams.getOrDefault("docId", bodyParams.getOrDefault("docId", "")).trim();
                        String collectionId = queryParams.getOrDefault("collectionId", bodyParams.getOrDefault("collectionId", "default")).trim();
                        if (docId.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Document ID is required\"}");
                            return;
                        }

                        boolean assigned = workspaceManager.assignCollection(docId, collectionId);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"assigned\":" + assigned + "}");
                        return;
                    }

                    if ("createCollection".equalsIgnoreCase(action)) {
                        String name = queryParams.getOrDefault("name", bodyParams.getOrDefault("name", "")).trim();
                        if (name.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Collection name cannot be blank\"}");
                            return;
                        }

                        workspace.Collection col = workspaceManager.createCollection(name);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"collection\":" + col.toJson() + "}");
                        return;
                    }

                    if ("renameCollection".equalsIgnoreCase(action)) {
                        String id = queryParams.getOrDefault("id", bodyParams.getOrDefault("id", "")).trim();
                        String name = queryParams.getOrDefault("name", bodyParams.getOrDefault("name", "")).trim();
                        if (id.isEmpty() || name.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Collection ID and new name are required\"}");
                            return;
                        }

                        boolean renamed = workspaceManager.renameCollection(id, name);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"renamed\":" + renamed + "}");
                        return;
                    }

                    if ("deleteCollection".equalsIgnoreCase(action)) {
                        String id = queryParams.getOrDefault("id", bodyParams.getOrDefault("id", "")).trim();
                        if (id.isEmpty() || "default".equalsIgnoreCase(id)) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Cannot delete default collection\"}");
                            return;
                        }

                        boolean deleted = workspaceManager.deleteCollection(id);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"deleted\":" + deleted + "}");
                        return;
                    }

                    if ("saveSearch".equalsIgnoreCase(action)) {
                        String query = queryParams.getOrDefault("query", bodyParams.getOrDefault("query", "")).trim();
                        String algo = queryParams.getOrDefault("algo", bodyParams.getOrDefault("algo", "coverage")).trim();
                        String category = queryParams.getOrDefault("category", bodyParams.getOrDefault("category", "all")).trim();

                        if (query.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Search query cannot be blank\"}");
                            return;
                        }

                        workspace.SavedSearch ss = workspaceManager.saveSearch(query, algo, category);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"savedSearch\":" + ss.toJson() + "}");
                        return;
                    }

                    if ("deleteSearch".equalsIgnoreCase(action)) {
                        String id = queryParams.getOrDefault("id", bodyParams.getOrDefault("id", "")).trim();
                        if (id.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Search ID is required\"}");
                            return;
                        }

                        boolean deleted = workspaceManager.deleteSearch(id);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"deleted\":" + deleted + "}");
                        return;
                    }

                    if ("clear".equalsIgnoreCase(action)) {
                        workspaceManager.clearWorkspace();
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"cleared\":true}");
                        return;
                    }

                    sendJsonResponse(exchange, 400, "{\"error\":\"Unknown workspace action: " + escapeJson(action) + "\"}");
                    return;
                }

                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            } catch (Exception e) {
                System.err.println("WorkspaceHandler exception: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"Workspace operation failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    /**
     * REST API for Google Comparison Lab evaluation records.
     * GET /api/google-eval: returns list of observations & summary precision metrics.
     * POST /api/google-eval: adds a new observation or handles action=delete / action=clear.
     */
    private class GoogleComparisonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            try {
                String method = exchange.getRequestMethod().toUpperCase();
                Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI());

                if ("GET".equals(method)) {
                    String json = googleComparisonManager.getEvaluationSummaryJson();
                    sendJsonResponse(exchange, 200, json);
                    return;
                }

                if ("POST".equals(method)) {
                    String body = readRequestBody(exchange);
                    Map<String, String> bodyParams = parseSimpleJson(body);

                    String action = queryParams.getOrDefault("action", bodyParams.getOrDefault("action", "add")).trim();

                    if ("delete".equalsIgnoreCase(action)) {
                        String id = queryParams.getOrDefault("id", bodyParams.getOrDefault("id", "")).trim();
                        if (id.isEmpty()) {
                            sendJsonResponse(exchange, 400, "{\"error\":\"Record ID is required\"}");
                            return;
                        }
                        boolean deleted = googleComparisonManager.deleteRecord(id);
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"deleted\":" + deleted + "}");
                        return;
                    }

                    if ("clear".equalsIgnoreCase(action)) {
                        googleComparisonManager.clearRecords();
                        sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"cleared\":true}");
                        return;
                    }

                    // Default: Add observation
                    String query = queryParams.getOrDefault("query", bodyParams.getOrDefault("query", "")).trim();
                    String engine = queryParams.getOrDefault("engine", bodyParams.getOrDefault("engine", "local")).trim();
                    String localAlgo = queryParams.getOrDefault("localAlgo", bodyParams.getOrDefault("localAlgo", "coverage")).trim();
                    String category = queryParams.getOrDefault("category", bodyParams.getOrDefault("category", "all")).trim();
                    String notes = queryParams.getOrDefault("notes", bodyParams.getOrDefault("notes", "")).trim();

                    int relevantCount = 0;
                    try {
                        String relStr = queryParams.getOrDefault("relevantCount", bodyParams.getOrDefault("relevantCount", "0"));
                        relevantCount = Integer.parseInt(relStr.trim());
                    } catch (NumberFormatException ignored) {}

                    long responseTimeMs = 0;
                    try {
                        String rtStr = queryParams.getOrDefault("responseTimeMs", bodyParams.getOrDefault("responseTimeMs", "0"));
                        responseTimeMs = Long.parseLong(rtStr.trim());
                    } catch (NumberFormatException ignored) {}

                    if (query.isEmpty()) {
                        sendJsonResponse(exchange, 400, "{\"error\":\"Query cannot be blank\"}");
                        return;
                    }

                    GoogleComparisonRecord record = googleComparisonManager.addRecord(
                            query, engine, localAlgo, category, relevantCount, notes, responseTimeMs);

                    sendJsonResponse(exchange, 200, "{\"status\":\"ok\",\"saved\":true,\"record\":" + record.toJson() + "}");
                    return;
                }

                sendJsonResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            } catch (Exception e) {
                System.err.println("GoogleComparisonHandler exception: " + e.getMessage());
                sendJsonResponse(exchange, 500, "{\"error\":\"Google comparison operation failed: " + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    // =========================================================================
    // STATIC FILE HANDLER
    // =========================================================================

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleCors(exchange)) return;

            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            // Prevent path traversal
            if (path.contains("..")) {
                sendResponse(exchange, 403, "text/plain", "Forbidden".getBytes(StandardCharsets.UTF_8));
                return;
            }

            File file = new File(WEB_DIR, path.startsWith("/") ? path.substring(1) : path);
            if (!file.exists() || file.isDirectory()) {
                // Fallback to index.html for SPA-style routing if applicable
                file = new File(WEB_DIR, "index.html");
                if (!file.exists()) {
                    sendResponse(exchange, 404, "text/plain", "Not Found".getBytes(StandardCharsets.UTF_8));
                    return;
                }
            }

            String contentType = getContentType(file.getName());
            byte[] bytes = Files.readAllBytes(file.toPath());
            sendResponse(exchange, 200, contentType, bytes);
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================

    private boolean handleCors(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return true;
        }
        return false;
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String getContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".ico")) return "image/x-icon";
        return "text/plain; charset=UTF-8";
    }

    private Map<String, String> parseQueryParams(URI uri) {
        Map<String, String> params = new HashMap<>();
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    params.put(key, value);
                } else if (!pair.isEmpty()) {
                    String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                    params.put(key, "");
                }
            } catch (Exception ignored) {}
        }
        return params;
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toString(StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.trim().isEmpty()) return map;

        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*(?:\"([^\"]*)\"|([0-9]+|true|false))");
        java.util.regex.Matcher m = p.matcher(json);
        while (m.find()) {
            String key = m.group(1);
            String val = m.group(2) != null ? m.group(2) : m.group(3);
            map.put(key, val);
        }
        return map;
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

    // =========================================================================
    // MAIN LAUNCHER
    // =========================================================================

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        } else if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port provided, using default " + DEFAULT_PORT);
            }
        }

        try {
            SearchServer server = new SearchServer(port);
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start Search Engine Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
