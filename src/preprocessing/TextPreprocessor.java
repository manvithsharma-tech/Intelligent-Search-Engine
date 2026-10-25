package preprocessing;

import java.util.*;

public class TextPreprocessor {

    public List<String> preprocess(String text) {

        // Convert everything to lowercase
        text = text.toLowerCase();

        // Remove punctuation
        text = text.replaceAll("[^a-z0-9\\s]", " ");

        // Split text into words
        String[] words = text.split("\\s+");

        List<String> tokens = new ArrayList<>();

        for (String word : words) {

            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }

        return tokens;
    }
}