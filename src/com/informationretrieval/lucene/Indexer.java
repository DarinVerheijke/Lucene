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

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Vector;

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
     *             The other arguments are the directories and files that should be added to the index. If the only
     *             additional argument is "-", then the default "./Posts.xml" will be used.
     */
    public static void main(String[] args) {
        final String usage = "Indexer [options] document+\n" +
                " -h,--help  Display the available options and required arguments.\n" +
                " -i,--index The directory that stores the new index. [default = ./Index]\n" +
                " -d,--dump  An indicator that the (XML) documents should be read as stackoverflow dumps.\n" +
                "            If this flag is set and no document are given, then ./Posts.xml is read.\n" +
                " document+  The files that should be indexed.\n";
        String index_dir = Constants.index_dir;
        boolean dump_file = false;
        Vector<String> documents = new Vector<>();

        for (int index = 0; index < args.length; ++index) {
            switch (args[index]) {
                case "-h", "--help" -> {
                    System.out.println("Usage: " + usage);
                    return;
                }
                case "-i", "--index" -> index_dir = args[++index];
                case "-d", "--dump" -> dump_file = true;
                default -> documents.add(args[index]);
            }
        }
        if (documents.isEmpty() && !dump_file)
            throw new IllegalArgumentException("\nExpected at least one argument, usage: " + usage);
        else if (documents.isEmpty())
            documents.add(Constants.dump_file);

        try {
            Indexer indexer = new Indexer(index_dir);

            System.out.println("Creating index in directory " + index_dir);
            long start_nr = indexer.writer.getDocStats().numDocs;
            long start = System.currentTimeMillis();

            for (String filename: documents) {
                // Assuming all XML files are the stackoverflow dump if they're given directly by the user
                // XML files in a directory that is being indexed won't be treated as such
                if (dump_file && filename.endsWith(".xml"))
                    indexer.addDumpToIndex(new File(filename));
                else
                    indexer.addToIndex(filename);
            }

            long end = System.currentTimeMillis();
            long end_nr = indexer.writer.getDocStats().numDocs;
            System.out.println((end_nr - start_nr) + " documents indexed, time: " + (end - start) + "ms");

            indexer.close();
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
        int initial_nr = writer.getDocStats().numDocs;
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
            return writer.getDocStats().numDocs - initial_nr;
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
            // Verify that the current element's name starts with 'r', which should only be for 'row' elements
            if (qname.charAt(0) != 'r')
                return;
            Document document = new Document();
            char type = attributes.getValue("PostTypeId").charAt(0);    // 1: question ; 2: answer

            document.add(new TextField("contents", attributes.getValue("Body"), Field.Store.NO));
            document.add(new TextField("id", attributes.getValue("Id"), Field.Store.YES));
            if (type == '1')        // For questions, also include the title
                document.add(new TextField("title", attributes.getValue("Title"), Field.Store.NO));
            else if (type == '2')   // For answers, also include the question
                document.add(new TextField("parent", attributes.getValue("ParentId"), Field.Store.NO));

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
     * @return The number of posts in total in the index.
     */
    public int addDumpToIndex(File file) {
        int initial_nr = writer.getDocStats().numDocs;
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            // Without disabling FEATURE_SECURE_PROCESSING the parser stops at 50.000.000
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
            SAXParser parser = factory.newSAXParser();
            DefaultHandler handler = new SODumpHandler();

            parser.parse(file, handler);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        return writer.getDocStats().numDocs - initial_nr;
    }
}
