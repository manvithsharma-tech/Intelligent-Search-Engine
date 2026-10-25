package trie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Trie {

    private final TrieNode root;

    public Trie() {
        root = new TrieNode();
    }

    // =========================================
    // INSERT
    // =========================================

    public void insert(String word) {

        if (word == null || word.isEmpty()) {
            return;
        }

        word = word.toLowerCase();

        insertWord(root, word);
    }

    private void insertWord(TrieNode node, String word) {

        for (Map.Entry<String, TrieNode> entry :
                node.getChildren().entrySet()) {

            String edge = entry.getKey();
            TrieNode child = entry.getValue();

            int common =
                    commonPrefixLength(word, edge);

            if (common == 0) {
                continue;
            }

            // Exact match
            if (common == word.length()
                    && common == edge.length()) {

                child.setEndOfWord(true);
                return;
            }

            // Existing edge completely matches
            if (common == edge.length()) {

                String remaining =
                        word.substring(common);

                insertWord(child, remaining);
                return;
            }

            // Split the edge
            String commonPart =
                    edge.substring(0, common);

            String oldPart =
                    edge.substring(common);

            String newPart =
                    word.substring(common);

            TrieNode middle =
                    new TrieNode();

            node.getChildren().remove(edge);

            node.getChildren().put(
                    commonPart,
                    middle
            );

            middle.getChildren().put(
                    oldPart,
                    child
            );

            if (newPart.isEmpty()) {

                middle.setEndOfWord(true);

            } else {

                TrieNode newNode =
                        new TrieNode();

                newNode.setEndOfWord(true);

                middle.getChildren().put(
                        newPart,
                        newNode
                );
            }

            return;
        }

        // No matching edge
        TrieNode newNode =
                new TrieNode();

        newNode.setEndOfWord(true);

        node.getChildren().put(
                word,
                newNode
        );
    }

    // =========================================
    // EXACT SEARCH
    // =========================================

    public boolean search(String word) {

        if (word == null || word.isEmpty()) {
            return false;
        }

        word = word.toLowerCase();

        return searchWord(root, word);
    }

    private boolean searchWord(
            TrieNode node,
            String word) {

        if (word.isEmpty()) {
            return node.isEndOfWord();
        }

        for (Map.Entry<String, TrieNode> entry :
                node.getChildren().entrySet()) {

            String edge = entry.getKey();

            if (word.startsWith(edge)) {

                String remaining =
                        word.substring(edge.length());

                return searchWord(
                        entry.getValue(),
                        remaining
                );
            }
        }

        return false;
    }

    // =========================================
    // PREFIX SEARCH
    // =========================================

    public boolean startsWith(String prefix) {

        if (prefix == null || prefix.isEmpty()) {
            return false;
        }

        prefix = prefix.toLowerCase();

        return prefixExists(root, prefix);
    }

    private boolean prefixExists(
            TrieNode node,
            String prefix) {

        for (Map.Entry<String, TrieNode> entry :
                node.getChildren().entrySet()) {

            String edge = entry.getKey();

            // Prefix completely matches edge
            if (edge.startsWith(prefix)) {
                return true;
            }

            // Prefix continues through this edge
            if (prefix.startsWith(edge)) {

                String remaining =
                        prefix.substring(edge.length());

                if (prefixExists(
                        entry.getValue(),
                        remaining)) {

                    return true;
                }
            }
        }

        return false;
    }

    // =========================================
    // GET WORDS WITH PREFIX
    // =========================================

    public List<String> getWordsWithPrefix(
            String prefix) {

        List<String> results =
                new ArrayList<>();

        if (prefix == null || prefix.isEmpty()) {
            return results;
        }

        prefix = prefix.toLowerCase();

        collectPrefixWords(
                root,
                prefix,
                "",
                results
        );

        return results;
    }

    private void collectPrefixWords(
            TrieNode node,
            String prefix,
            String currentWord,
            List<String> results) {

        for (Map.Entry<String, TrieNode> entry :
                node.getChildren().entrySet()) {

            String edge = entry.getKey();
            TrieNode child = entry.getValue();

            // =================================
            // Prefix is inside this edge
            // =================================

            if (edge.startsWith(prefix)) {

                String completeWord =
                        currentWord + edge;

                if (child.isEndOfWord()) {
                    results.add(completeWord);
                }

                collectAllWords(
                        child,
                        completeWord,
                        results
                );

                return;
            }

            // =================================
            // Prefix continues through this edge
            // =================================

            if (prefix.startsWith(edge)) {

                String remaining =
                        prefix.substring(edge.length());

                collectPrefixWords(
                        child,
                        remaining,
                        currentWord + edge,
                        results
                );

                return;
            }
        }
    }

    // =========================================
    // COLLECT ALL WORDS
    // =========================================

    private void collectAllWords(
            TrieNode node,
            String currentWord,
            List<String> results) {

        for (Map.Entry<String, TrieNode> entry :
                node.getChildren().entrySet()) {

            String edge = entry.getKey();
            TrieNode child = entry.getValue();

            String completeWord =
                    currentWord + edge;

            if (child.isEndOfWord()) {
                results.add(completeWord);
            }

            collectAllWords(
                    child,
                    completeWord,
                    results
            );
        }
    }

    // =========================================
    // COMMON PREFIX
    // =========================================

    private int commonPrefixLength(
            String first,
            String second) {

        int length =
                Math.min(
                        first.length(),
                        second.length()
                );

        int i = 0;

        while (i < length
                && first.charAt(i)
                == second.charAt(i)) {

            i++;
        }

        return i;
    }
}