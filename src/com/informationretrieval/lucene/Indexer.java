package com.informationretrieval.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.print.Doc;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

// Based on tutorials on https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm and
//  http://www.lucenetutorial.com/sample-apps/textfileindexer-java.html


/**
 * A class that offers indexing functionality. The created indices are stored in the directory that is passed to the
 * object's constructor. Creating indices can be done by either passing a `File` object to `indexFile()` or by passing
 * a directory path to `createIndex()`.
 */
public class Indexer {
    private final IndexWriter writer;

    /**
     * A main function that indexes one or more directories or files.
     *
     * @param args The command line arguments passed to this script.
     *             The first argument should be the directory where the index should be stored, or "-" if the default
     *             directory "./Index" should be used.
     *             The other arguments are the directories and files that should be added to the index.
     */
    public static void main(String[] args) {
        try {
            String index_dir = args[0].equals("-") ? Constants.index_dir : args[0];
            System.out.println("Creating index in directory " + index_dir);
            Indexer indexer = new Indexer(index_dir);

            int nr_files = 0;
            long start = System.currentTimeMillis();
            for (int index = 1; index < args.length; ++index)
                nr_files += indexer.addToIndex(args[index]);
            long end = System.currentTimeMillis();

            indexer.close();
            System.out.println(nr_files + " files indexed, time: " + (end - start) + "ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The constructor; initialises the `writer` object.
     *
     * @param directoryPath The directory used to store the index.
     */
    public Indexer(String directoryPath) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(directoryPath));
        for (String file: dir.listAll())
            dir.deleteFile(file);

        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(dir, config);
    }

    /**
     * Closes the `writer` object.
     */
    public void close() throws IOException {
        writer.close();
    }

    /**
     * Creates a `Document` object from the given `File` object. The new object gets the following fields:
     * contents, file_name, file_path.
     *
     * @param file The `File` object for which a `Document` object is to be created.
     * @return The new `Document` object.
     */
    private Document getDocument(File file) throws IOException {
        Document doc = new Document();

        TextField cfield = new TextField(Constants.contents, new FileReader(file));
        TextField filenameField = new TextField(Constants.file_name, file.getName(), TextField.Store.YES);
        TextField filepathField = new TextField(Constants.file_path, file.getCanonicalPath(), TextField.Store.YES);

        doc.add(cfield);
        doc.add(filenameField);
        doc.add(filepathField);
        return doc;
    }

    /**
     * Adds a single file to the index.
     *
     * @param file The `File` object that is to be added to the index.
     */
    private void indexFile(File file) throws IOException {
        System.out.println("Indexing" + file.getCanonicalPath());
        Document doc = getDocument(file);
        writer.addDocument(doc);
    }

    /**
     * Adds a file or all files in a directory to the index.
     *
     * @param path The file or directory path containing the files that are to be added to the index.
     * @return The number of files that were added to the index.
     */
    public int addToIndex(String path) throws IOException {
        File file_or_dir = new File(path);

        // Skip non-existent and unreadable files/directories
        // Don't skip hidden files/directories here because they've been explicitly requested by the caller
        if (!file_or_dir.exists() || !file_or_dir.canRead())
            throw new FileNotFoundException(path + " doesn't exist or is unreadable.");
        // Simply add files to the index
        else if (file_or_dir.isFile()) {
            indexFile(file_or_dir);
            return 1;
        }
        // Add all files in a directory to the index
        else {
            File[] files = file_or_dir.listFiles();
            // If the previous statement for some reason returned a null pointer
            if (files == null)
                return 0;

            for (File file : files) {
                // Skip non-existent and unreadable files/directories
                // Skip hidden files/directories as well
                if (!file.exists() || !file.canRead() || file.isHidden())
                    continue;
                // Recursively add all files in sub-directories as well
                else if (file.isDirectory())
                    addToIndex(file.getAbsolutePath());
                else
                    indexFile(file);
            }
            return writer.numRamDocs();
        }
    }

    /**
     * A subclass of `DefaultHandler` that implements the `startElement` function.
     */
    private class SODumpHandler extends DefaultHandler {

        /**
         * A function that is called whenever a starting XML tag has been read. All tags that don't start with the
         * character 'r' are ignored. This is because we assume that the only tags that will be read are `row` tags and
         * a single `posts` tag. This means that we can compare the two tags more efficiently by only comparing the
         * first character.
         *
         * @param uri The namespace URI; this parameter is not used.
         * @param localname The local name (without prefix); this parameter is not used.
         * @param qname The qualified name, or in this case the type of tag.
         * @param attributes The attributes of the element.
         */
        public void startElement(String uri, String localname, String qname, Attributes attributes) {
            if (qname.charAt(0) != 'r')
                return;
            Document document = new Document();
            String type = attributes.getValue("PostTypeId"); // 1: question ; 2: answer

            document.add(new TextField("id", attributes.getValue("Id"), Field.Store.YES));
            document.add(new TextField("contents", attributes.getValue("Body"), Field.Store.YES));
            // For questions: also include the title
            if (type.equals("1"))
                document.add(new TextField("title", attributes.getValue("Title"), Field.Store.YES));

            try {
                writer.addDocument(document);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reads the stackoverflow dump XML file and adds its posts to the index.
     *
     * @param file The XML dump file.
     *
     * @return The number of posts that was added to the index.
     */
    public int addDumpToIndex(File file) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            DefaultHandler handler = new SODumpHandler();

            parser.parse(file, handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return writer.numRamDocs();
    }
}
