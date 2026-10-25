package evaluation;

import model.Document;

import java.util.List;

public class SearchEvaluator {

    // Calculate Precision
    public double calculatePrecision(
            List<Document> results,
            String expectedCategory) {

        if (results.isEmpty()) {
            return 0.0;
        }

        int relevantDocuments = 0;

        for (Document document : results) {

            if (document.getCategory()
                    .equalsIgnoreCase(expectedCategory)) {

                relevantDocuments++;
            }
        }

        return (double) relevantDocuments / results.size();
    }

    // Calculate Recall
    public double calculateRecall(
            List<Document> results,
            List<Document> allDocuments,
            String expectedCategory) {

        int totalRelevantDocuments = 0;

        for (Document document : allDocuments) {

            if (document.getCategory()
                    .equalsIgnoreCase(expectedCategory)) {

                totalRelevantDocuments++;
            }
        }

        if (totalRelevantDocuments == 0) {
            return 0.0;
        }

        int retrievedRelevantDocuments = 0;

        for (Document document : results) {

            if (document.getCategory()
                    .equalsIgnoreCase(expectedCategory)) {

                retrievedRelevantDocuments++;
            }
        }

        return (double) retrievedRelevantDocuments
                / totalRelevantDocuments;
    }

    // Calculate F1 Score
    public double calculateF1(
            double precision,
            double recall) {

        if (precision + recall == 0) {
            return 0.0;
        }

        return 2 * (precision * recall)
                / (precision + recall);
    }

    // Display evaluation results
    public void displayEvaluation(
            double precision,
            double recall,
            double f1Score) {

        System.out.println();
        System.out.println("=================================");
        System.out.println("SEARCH ENGINE EVALUATION");
        System.out.println("=================================");

        System.out.printf(
                "Precision : %.4f%n",
                precision
        );

        System.out.printf(
                "Recall    : %.4f%n",
                recall
        );

        System.out.printf(
                "F1 Score  : %.4f%n",
                f1Score
        );
    }
}