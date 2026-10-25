
package main;

import evaluation.SearchEvaluator;
import index.InvertedIndex;
import model.Document;
import preprocessing.DocumentReader;
import preprocessing.TextPreprocessor;
import ranking.RankingEngine;
import trie.Trie;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        // Check if user requested starting the web server directly
        if (args.length > 0 && "--server".equalsIgnoreCase(args[0])) {
            String[] serverArgs = new String[args.length - 1];
            System.arraycopy(args, 1, serverArgs, 0, serverArgs.length);
            server.SearchServer.main(serverArgs);
            return;
        }

        // 1. READ DATASET
        DocumentReader reader = new DocumentReader();
        List<Document> documents = reader.readDocuments();

        System.out.println(
                "Total Documents Read: " + documents.size()
        );

        // 2. TEXT PREPROCESSING
        TextPreprocessor preprocessor = new TextPreprocessor();

        // 3. CREATE INVERTED INDEX AND COMPRESSED TRIE
        InvertedIndex invertedIndex = new InvertedIndex();
        Trie trie = new Trie();

        // Process every document
        for (Document document : documents) {

            List<String> words =
                    preprocessor.preprocess(document.getContent());

            // Add words to the inverted index
            invertedIndex.addDocument(document, words);

            // Add the same vocabulary to the compressed Trie
            for (String word : words) {
                trie.insert(word);
            }
        }

        // 4. DISPLAY INDEX INFORMATION
        System.out.println();
        System.out.println("====================================");
        System.out.println("INVERTED INDEX CREATED");
        System.out.println("====================================");

        System.out.println(
                "Total Unique Terms: " +
                        invertedIndex.getNumberOfTerms()
        );

        // 5. TEMPORARY COMPRESSED TRIE INTEGRATION TEST
        System.out.println();
        System.out.println("====================================");
        System.out.println("COMPRESSED TRIE INTEGRATION TEST");
        System.out.println("====================================");

        System.out.println(
                "Exact search 'cricket': " +
                        trie.search("cricket")
        );

        System.out.println(
                "Prefix suggestions for 'crick': " +
                        trie.getWordsWithPrefix("crick")
        );

        // 6. CREATE RANKING ENGINE
        RankingEngine rankingEngine =
                new RankingEngine(documents, invertedIndex);

        // 7. CREATE SEARCH EVALUATOR
        SearchEvaluator evaluator = new SearchEvaluator();

        // 8. CREATE SCANNER
        Scanner scanner = new Scanner(System.in);

        // 9. SEARCH ENGINE
        System.out.println();
        System.out.println("====================================");
        System.out.println("SEARCH ENGINE");
        System.out.println("====================================");

        System.out.print("Enter search query: ");

        String query = scanner.nextLine()
                .toLowerCase()
                .trim();

        if (query.isEmpty()) {
            System.out.println("Search query cannot be empty.");
            scanner.close();
            return;
        }

        // 10. RANK SEARCH RESULTS
        List<Document> rankedDocuments = rankingEngine.rank(query);

        // 11. DISPLAY TOP 10 RESULTS
        rankingEngine.displayResults(
                query,
                rankedDocuments,
                10
        );

        // 12. EVALUATION
        System.out.println();
        System.out.print(
                "Enter expected category for evaluation " +
                        "(business/entertainment/politics/sport/tech/science/education/health/environment/finance/ai): "
        );

        String expectedCategory = scanner.nextLine()
                .toLowerCase()
                .trim();

        // 13. CALCULATE PRECISION
        double precision = evaluator.calculatePrecision(
                rankedDocuments,
                expectedCategory
        );

        // 14. CALCULATE RECALL
        double recall = evaluator.calculateRecall(
                rankedDocuments,
                documents,
                expectedCategory
        );

        // 15. CALCULATE F1 SCORE
        double f1Score = evaluator.calculateF1(
                precision,
                recall
        );

        // 16. DISPLAY EVALUATION RESULTS
        evaluator.displayEvaluation(
                precision,
                recall,
                f1Score
        );

        // 17. CLOSE SCANNER
        scanner.close();
    }
}
