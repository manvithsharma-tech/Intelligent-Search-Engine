package ranking;

import index.InvertedIndex;
import model.Document;

import java.util.*;

public class RankingEngine {

    private final List<Document> documents;
    private final InvertedIndex invertedIndex;
    private final Map<String, Integer> docLengths;
    private final double avgDocLength;

    public RankingEngine(
            List<Document> documents,
            InvertedIndex invertedIndex) {

        this.documents = documents;
        this.invertedIndex = invertedIndex;

        this.docLengths = new HashMap<>(documents.size() + 16, 0.75f);
        long totalTokens = 0;
        for (Document doc : documents) {
            int len = doc.getWordCount();
            if (len < 0) {
                len = countWords(doc.getContent());
                doc.setWordCount(len);
            }
            docLengths.put(doc.getId(), len);
            totalTokens += len;
        }
        this.avgDocLength = documents.isEmpty() ? 1.0 : (double) totalTokens / documents.size();
    }

    // -----------------------------------------
    // Calculate Term Frequency (TF)
    // -----------------------------------------
    private double calculateTF(
            String term,
            Document document) {

        int count = countTermOccurrences(term, document);
        if (count == 0) {
            return 0.0;
        }

        int totalWords = docLengths.getOrDefault(document.getId(), 0);
        if (totalWords <= 0) {
            totalWords = countWords(document.getContent());
        }

        if (totalWords == 0) {
            return 0.0;
        }

        return (double) count / totalWords;
    }

    // -----------------------------------------
    // Calculate Inverse Document Frequency
    // -----------------------------------------
    private double calculateIDF(String term) {

        int totalDocuments =
                documents.size();

        Set<String> matchingDocuments =
                invertedIndex.search(term);

        int documentFrequency =
                matchingDocuments.size();

        if (documentFrequency == 0) {
            return 0;
        }

        return Math.log(
                (double) totalDocuments
                        / documentFrequency
        );
    }

    // -----------------------------------------
    // Calculate TF-IDF Score
    // -----------------------------------------
    private double calculateScore(
            String term,
            Document document) {

        double tf =
                calculateTF(
                        term,
                        document
                );

        double idf =
                calculateIDF(term);

        return tf * idf;
    }

    // -----------------------------------------
    // Word Counting and Occurrence Helpers
    // -----------------------------------------
    private int countWords(String text) {
        if (text == null || text.isEmpty()) return 0;
        int count = 0;
        boolean inWord = false;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            char c = text.charAt(i);
            boolean isWordChar = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
            if (isWordChar) {
                if (!inWord) {
                    count++;
                    inWord = true;
                }
            } else {
                inWord = false;
            }
        }
        return count;
    }

    private int countTermOccurrences(String term, Document document) {
        if (term == null || term.isEmpty() || document == null) return 0;
        String text = document.getContent();
        if (text == null || text.isEmpty()) return 0;

        int count = 0;
        int termLen = term.length();
        int textLen = text.length();

        for (int i = 0; i <= textLen - termLen; ) {
            // Check word boundary at start
            if (i > 0) {
                char prev = text.charAt(i - 1);
                if ((prev >= 'a' && prev <= 'z') || (prev >= 'A' && prev <= 'Z') || (prev >= '0' && prev <= '9')) {
                    i++;
                    continue;
                }
            }

            // Case-insensitive match against term
            if (text.regionMatches(true, i, term, 0, termLen)) {
                // Check word boundary at end
                int nextIdx = i + termLen;
                if (nextIdx == textLen) {
                    count++;
                    i = nextIdx;
                } else {
                    char next = text.charAt(nextIdx);
                    if (!((next >= 'a' && next <= 'z') || (next >= 'A' && next <= 'Z') || (next >= '0' && next <= '9'))) {
                        count++;
                        i = nextIdx;
                    } else {
                        i++;
                    }
                }
            } else {
                i++;
            }
        }
        return count;
    }

    // -----------------------------------------
    // Calculate Okapi BM25 Score for a Single Term
    // -----------------------------------------
    private double calculateBM25Score(String term, Document document) {
        int f = countTermOccurrences(term, document);
        if (f == 0) return 0.0;

        int df = invertedIndex.search(term).size();
        if (df == 0) return 0.0;

        int N = documents.size();
        double idf = Math.log(1.0 + (N - df + 0.5) / (df + 0.5));

        int docLen = docLengths.getOrDefault(document.getId(), countWords(document.getContent()));
        double k1 = 1.2;
        double b = 0.75;
        double lenNorm = 1.0 - b + b * (docLen / (avgDocLength > 0 ? avgDocLength : 1.0));
        double tfWeight = (f * (k1 + 1.0)) / (f + k1 * lenNorm);

        return idf * tfWeight;
    }

    // -----------------------------------------
    // Query Term Normalization & Explanation Helpers
    // -----------------------------------------
    public List<String> extractValidTerms(String query) {
        if (query == null) return Collections.emptyList();
        String[] rawTerms = query.toLowerCase().trim().split("\\s+");
        List<String> valid = new ArrayList<>();
        for (String raw : rawTerms) {
            String clean = raw.replaceAll("[^a-z0-9]", "");
            if (!clean.isEmpty()) {
                valid.add(clean);
            }
        }
        return valid;
    }

    public int getValidTermsCount(String query) {
        return extractValidTerms(query).size();
    }

    public List<String> getMatchedTerms(String query, Document document) {
        List<String> valid = extractValidTerms(query);
        List<String> matched = new ArrayList<>();
        for (String term : valid) {
            if (countTermOccurrences(term, document) > 0) {
                matched.add(term);
            }
        }
        return matched;
    }

    public int getMatchedTermsCount(String query, Document document) {
        return getMatchedTerms(query, document).size();
    }

    // -----------------------------------------
    // Rank Documents (Default: Coverage-Aware TF-IDF)
    // -----------------------------------------
    public List<Document> rank(String query) {
        return rank(query, "coverage");
    }

    // -----------------------------------------
    // Rank Documents with Selectable Algorithm Lab
    // Supports: "coverage" (Default), "baseline", "bm25"
    // -----------------------------------------
    public List<Document> rank(String query, String algo) {
        if (algo == null || algo.trim().isEmpty()) {
            algo = "coverage";
        }
        algo = algo.trim().toLowerCase();

        List<String> validQueryTerms = extractValidTerms(query);
        if (validQueryTerms.isEmpty()) {
            return Collections.emptyList();
        }

        int validTermsCount = validQueryTerms.size();
        Set<String> candidateIds = new HashSet<>();

        for (String term : validQueryTerms) {
            Set<String> ids = invertedIndex.search(term);
            candidateIds.addAll(ids);
        }

        Map<Document, Double> scores = new HashMap<>();

        for (Document document : documents) {
            if (!candidateIds.contains(document.getId())) {
                continue;
            }

            double totalScore = 0.0;
            int matchedQueryTerms = 0;

            for (String term : validQueryTerms) {
                double termScore;
                if ("bm25".equals(algo)) {
                    termScore = calculateBM25Score(term, document);
                } else {
                    termScore = calculateScore(term, document);
                }

                if (termScore > 0) {
                    matchedQueryTerms++;
                    totalScore += termScore;
                }
            }

            if (totalScore > 0 && validTermsCount > 0) {
                if ("coverage".equals(algo)) {
                    double coverage = (double) matchedQueryTerms / validTermsCount;
                    totalScore *= Math.pow(coverage, 1.5);
                }

                if (totalScore > 0) {
                    scores.put(document, totalScore);
                }
            }
        }

        List<Document> rankedDocuments = new ArrayList<>(scores.keySet());
        rankedDocuments.sort(
                (doc1, doc2) -> {
                    int scoreCompare = Double.compare(
                            scores.get(doc2),
                            scores.get(doc1)
                    );
                    if (scoreCompare != 0) {
                        return scoreCompare;
                    }
                    return doc1.getId().compareTo(doc2.getId());
                }
        );

        return rankedDocuments;
    }

    // -----------------------------------------
    // Generate Result Snippet
    // -----------------------------------------
    private String generateSnippet(
            String query,
            Document document) {

        String text =
                document.getContent();

        String lowerText =
                text.toLowerCase();

        String[] queryTerms =
                query.toLowerCase()
                        .split("\\s+");

        int position = -1;

        // Find the first query term
        for (String term : queryTerms) {

            term = term.replaceAll(
                    "[^a-z0-9]",
                    ""
            );

            if (term.isEmpty()) {
                continue;
            }

            position =
                    lowerText.indexOf(term);

            if (position != -1) {
                break;
            }
        }

        // If no query term is found
        if (position == -1) {

            if (text.length() > 150) {
                return text.substring(0, 150)
                        + "...";
            }

            return text;
        }

        // Start before the matching term
        int start =
                Math.max(
                        0,
                        position - 60
                );

        // End after the matching term
        int end =
                Math.min(
                        text.length(),
                        position + 120
                );

        String snippet =
                text.substring(start, end)
                        .replaceAll(
                                "\\s+",
                                " "
                        )
                        .trim();

        if (start > 0) {
            snippet = "... " + snippet;
        }

        if (end < text.length()) {
            snippet = snippet + " ...";
        }

        return snippet;
    }

    // -----------------------------------------
    // Display Ranked Results
    // -----------------------------------------
    public void displayResults(
            String query,
            List<Document> rankedDocuments,
            int limit) {

        System.out.println();

        System.out.println(
                "================================="
        );

        System.out.println(
                "RANKED SEARCH RESULTS"
        );

        System.out.println(
                "================================="
        );

        System.out.println(
                "Query: " + query
        );

        System.out.println();

        int count = 0;

        for (Document document :
                rankedDocuments) {

            double score = getScore(query, document);

            // Display document information
            System.out.println(
                    (count + 1)
                            + ". Document ID: "
                            + document.getId()
                            + " | Category: "
                            + document.getCategory()
                            + " | Score: "
                            + score
            );

            // Display snippet
            System.out.println(
                    "   Snippet: "
                            + generateSnippet(
                            query,
                            document
                    )
            );

            System.out.println();

            count++;

            if (count >= limit) {
                break;
            }
        }

        System.out.println(
                "Total matching documents: "
                        + rankedDocuments.size()
        );
    }

    // -----------------------------------------
    // Helper to get snippet for a document
    // -----------------------------------------
    public String getSnippet(String query, Document document) {
        return generateSnippet(query, document);
    }

    // -----------------------------------------
    // Helper to get score for a document (Default: Coverage-Aware TF-IDF)
    // -----------------------------------------
    public double getScore(String query, Document document) {
        return getScore(query, document, "coverage");
    }

    // -----------------------------------------
    // Helper to get score for a document with specific algorithm
    // Supports: "coverage" (Default), "baseline", "bm25"
    // -----------------------------------------
    public double getScore(String query, Document document, String algo) {
        if (algo == null || algo.trim().isEmpty()) {
            algo = "coverage";
        }
        algo = algo.trim().toLowerCase();

        List<String> validTerms = extractValidTerms(query);
        if (validTerms.isEmpty()) {
            return 0.0;
        }

        double score = 0.0;
        int matched = 0;
        for (String term : validTerms) {
            double termScore;
            if ("bm25".equals(algo)) {
                termScore = calculateBM25Score(term, document);
            } else {
                termScore = calculateScore(term, document);
            }

            if (termScore > 0) {
                matched++;
                score += termScore;
            }
        }

        if (score > 0 && "coverage".equals(algo)) {
            double coverage = (double) matched / validTerms.size();
            score *= Math.pow(coverage, 1.5);
        }

        return score;
    }
}