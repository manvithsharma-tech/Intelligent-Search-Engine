package trie;

import java.util.List;

public class TrieTest {

    public static void main(String[] args) {

        Trie trie = new Trie();

        // Insert sample words
        trie.insert("football");
        trie.insert("footballer");
        trie.insert("foot");
        trie.insert("food");
        trie.insert("world");
        trie.insert("worldcup");
        trie.insert("cricket");

        System.out.println("=================================");
        System.out.println("COMPRESSED TRIE TEST");
        System.out.println("=================================");

        // Exact search
        System.out.println(
                "Search football: "
                        + trie.search("football")
        );

        System.out.println(
                "Search cricket: "
                        + trie.search("cricket")
        );

        System.out.println(
                "Search basketball: "
                        + trie.search("basketball")
        );

        // Prefix search
        System.out.println();
        System.out.println("Prefix: foot");

        List<String> results =
                trie.getWordsWithPrefix("foot");

        for (String word : results) {
            System.out.println("  " + word);
        }

        System.out.println();
        System.out.println("Prefix: wor");

        results =
                trie.getWordsWithPrefix("wor");

        for (String word : results) {
            System.out.println("  " + word);
        }
    }
}