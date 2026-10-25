package preprocessing;

import model.Document;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class DocumentReader {

    private static final String DATASET_PATH = "dataset";

    public List<Document> readDocuments() {

        List<Document> documentsList = new ArrayList<>();

        File datasetFolder = new File(DATASET_PATH);

        if (!datasetFolder.exists()) {
            System.out.println("Dataset folder not found!");
            return documentsList;
        }

        File[] categoryFolders = datasetFolder.listFiles(File::isDirectory);

        if (categoryFolders == null) {
            System.out.println("No category folders found!");
            return documentsList;
        }

        for (File category : categoryFolders) {

            File[] documents = category.listFiles();

            if (documents == null) {
                continue;
            }

            for (File document : documents) {

                if (!document.isFile() ||
                        !document.getName().toLowerCase().endsWith(".txt")) {
                    continue;
                }

                try {

                    String content;

                    try {
                        // Try UTF-8 first
                        content = Files.readString(
                                document.toPath(),
                                StandardCharsets.UTF_8
                        );

                    } catch (Exception e) {

                        // If UTF-8 fails, use ISO-8859-1
                        content = Files.readString(
                                document.toPath(),
                                StandardCharsets.ISO_8859_1
                        );

                        System.out.println(
                                "Used alternate encoding for: "
                                        + document.getName()
                        );
                    }

                    String fileName = document.getName();

                    String id = fileName.substring(
                            0,
                            fileName.lastIndexOf(".")
                    );

                    String categoryName = category.getName();

                    Document doc = new Document(
                            id,
                            categoryName,
                            fileName,
                            content
                    );

                    documentsList.add(doc);

                } catch (Exception e) {

                    System.out.println(
                            "Could not read: " + document.getName()
                    );
                }
            }
        }

        return documentsList;
    }
}