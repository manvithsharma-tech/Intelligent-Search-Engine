package trie;

import java.util.HashMap;
import java.util.Map;

public class TrieNode {

    // Each edge stores a string segment
    private Map<String, TrieNode> children;

    // True if a complete word ends here
    private boolean endOfWord;

    public TrieNode() {
        children = new HashMap<>();
        endOfWord = false;
    }

    public Map<String, TrieNode> getChildren() {
        return children;
    }

    public boolean isEndOfWord() {
        return endOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        this.endOfWord = endOfWord;
    }
}