package index;

import model.Document;

import java.util.*;

public class InvertedIndex {

    private final Map<String, Set<String>> index;

    public InvertedIndex() {
        index = new HashMap<>();
    }

    public void addDocument(Document document, List<String> words) {

        for (String word : words) {

            if (word == null || word.isEmpty()) {
                continue;
            }

            index
                    .computeIfAbsent(word, k -> new HashSet<>())
                    .add(document.getId());
        }
    }

    public Set<String> search(String word) {

        return index.getOrDefault(
                word.toLowerCase(),
                Collections.emptySet()
        );
    }

    public int getNumberOfTerms() {
        return index.size();
    }

    public void displayIndex(int limit) {

        int count = 0;

        for (Map.Entry<String, Set<String>> entry : index.entrySet()) {

            System.out.println(
                    entry.getKey() + " -> " + entry.getValue()
            );

            count++;

            if (count >= limit) {
                break;
            }
        }
    }
}