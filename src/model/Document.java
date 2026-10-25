package model;

public class Document {

    private String id;
    private String category;
    private String fileName;
    private String content;
    private int wordCount = -1;

    public Document(String id, String category, String fileName, String content) {
        this.id = id;
        this.category = category;
        this.fileName = fileName;
        this.content = content;
    }

    public int getWordCount() {
        return wordCount;
    }

    public void setWordCount(int wordCount) {
        this.wordCount = wordCount;
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Document{" +
                "id='" + id + '\'' +
                ", category='" + category + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}