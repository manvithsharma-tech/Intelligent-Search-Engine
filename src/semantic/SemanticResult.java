package semantic;

import model.Document;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a ranked semantic search result with genuine LSA cosine similarity
 * score and human-interpretable conceptual explanation.
 */
public class SemanticResult {

    private final int rank;
    private final Document document;
    private final double score; // Cosine similarity in [0, 1]
    private final List<String> topConcepts;
    private final List<String> relatedKeywords;
    private final String explanation;
    private final String snippet;

    public SemanticResult(int rank,
                          Document document,
                          double score,
                          List<String> topConcepts,
                          List<String> relatedKeywords,
                          String explanation,
                          String snippet) {
        this.rank = rank;
        this.document = document;
        this.score = score;
        this.topConcepts = topConcepts != null ? topConcepts : Collections.emptyList();
        this.relatedKeywords = relatedKeywords != null ? relatedKeywords : Collections.emptyList();
        this.explanation = explanation != null ? explanation : "";
        this.snippet = snippet != null ? snippet : "";
    }

    public int getRank() {
        return rank;
    }

    public Document getDocument() {
        return document;
    }

    public double getScore() {
        return score;
    }

    public List<String> getTopConcepts() {
        return topConcepts;
    }

    public List<String> getRelatedKeywords() {
        return relatedKeywords;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSnippet() {
        return snippet;
    }

    @Override
    public String toString() {
        return String.format("SemanticResult[#%d, %s, score=%.4f, category=%s]",
                rank, document.getId(), score, document.getCategory());
    }
}
