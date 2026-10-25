package index;

import model.Document;

import java.util.*;

/**
 * High-performance, memory-optimized Inverted Index.
 * Postings are stored as compact string arrays inside CompactDocSet,
 * completely eliminating millions of HashMap.Node wrapper allocations.
 */
public class InvertedIndex {

    private final Map<String, CompactDocSet> index;
    private Map<String, ArrayList<String>> builderIndex;
    private boolean finalized = false;

    public InvertedIndex() {
        this.index = new HashMap<>(131072, 0.75f);
        this.builderIndex = new HashMap<>(131072, 0.75f);
    }

    public synchronized void addDocument(Document document, List<String> words) {
        if (words == null || words.isEmpty() || document == null) {
            return;
        }

        if (finalized) {
            reopenBuilder();
        }

        String docId = document.getId();

        // Deduplicate words for this document so each term gets docId added once
        Set<String> uniqueWords = new HashSet<>(words.size());
        for (String word : words) {
            if (word != null && !word.isEmpty()) {
                uniqueWords.add(word);
            }
        }

        for (String word : uniqueWords) {
            builderIndex
                    .computeIfAbsent(word, k -> new ArrayList<>(4))
                    .add(docId);
        }
    }

    /**
     * Finalizes the index by converting builder lists into compact String arrays
     * and releasing each builder list incrementally to prevent peak heap spikes.
     */
    public synchronized void finalizeIndex() {
        if (finalized || builderIndex == null) {
            return;
        }

        index.clear();
        Iterator<Map.Entry<String, ArrayList<String>>> it = builderIndex.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ArrayList<String>> entry = it.next();
            ArrayList<String> list = entry.getValue();
            String[] arr = (list != null) ? list.toArray(new String[list.size()]) : new String[0];
            index.put(entry.getKey(), new CompactDocSet(arr));
            entry.setValue(null);
            it.remove();
        }

        builderIndex.clear();
        builderIndex = null;
        finalized = true;
    }

    private synchronized void reopenBuilder() {
        if (builderIndex == null) {
            builderIndex = new HashMap<>(index.size() + 16, 0.75f);
            for (Map.Entry<String, CompactDocSet> entry : index.entrySet()) {
                ArrayList<String> list = new ArrayList<>(Arrays.asList(entry.getValue().getDocIds()));
                builderIndex.put(entry.getKey(), list);
            }
        }
        finalized = false;
    }

    private void ensureFinalized() {
        if (!finalized) {
            finalizeIndex();
        }
    }

    private static final CompactDocSet EMPTY_SET = new CompactDocSet(new String[0]);

    public Set<String> search(String word) {
        if (word == null || word.isEmpty()) {
            return EMPTY_SET;
        }
        ensureFinalized();
        return index.getOrDefault(word.toLowerCase(), EMPTY_SET);
    }

    public int getNumberOfTerms() {
        ensureFinalized();
        return index.size();
    }

    public Set<String> getTerms() {
        ensureFinalized();
        return index.keySet();
    }

    public void displayIndex(int limit) {
        ensureFinalized();
        int count = 0;
        for (Map.Entry<String, CompactDocSet> entry : index.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
            count++;
            if (count >= limit) {
                break;
            }
        }
    }

    /**
     * Compact, memory-efficient Set implementation backed by a flat String[] array.
     * Implements java.util.Set<String> for full backwards compatibility with zero per-entry Node overhead.
     */
    public static class CompactDocSet extends AbstractSet<String> {
        private final String[] docIds;

        public CompactDocSet(String[] docIds) {
            this.docIds = docIds != null ? docIds : new String[0];
        }

        @Override
        public int size() {
            return docIds.length;
        }

        @Override
        public boolean isEmpty() {
            return docIds.length == 0;
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof String) || docIds.length == 0) return false;
            String s = (String) o;
            for (int i = 0; i < docIds.length; i++) {
                if (s.equals(docIds[i])) return true;
            }
            return false;
        }

        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < docIds.length;
                }

                @Override
                public String next() {
                    if (cursor >= docIds.length) {
                        throw new NoSuchElementException();
                    }
                    return docIds[cursor++];
                }
            };
        }

        public String[] getDocIds() {
            return docIds;
        }
    }
}