package com.informationretrieval.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Arrays;

// Based on tutorials on https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm and
//  http://www.lucenetutorial.com/sample-apps/textfileindexer-java.html

/**
 * A class that offers searching functionality. The directory containing the indices of the collection that is being
 * searched is passed to the object's constructor. Queries can then be passed to the `search()` function, after which
 * the actual documents from the results can be retrieved using `getDocument()`.
 */
public class Searcher {
    IndexSearcher indexSearcher;
    QueryParser queryParser;
    Query query;

    /**
     * A main function that performs a query on an index.
     *
     * @param args The command line arguments passed to this script.
     *             The first argument should be the directory in which the index is stored, or "-" if the default
     *             directory "./Index" should be used.
     *             The other arguments are the query parameters.
     */
    public static void main(String[] args) {
        try {
            String index_dir = args[0].equals("-") ? Constants.index_dir : args[0];
            System.out.println("Searching through index in " + index_dir);
            Searcher searcher = new Searcher(index_dir);

            String query = String.join("", Arrays.copyOfRange(args, 1, args.length));
            long start = System.currentTimeMillis();
            TopDocs hits = searcher.search(query);
            long end = System.currentTimeMillis();

            System.out.println(hits.totalHits + " documents found, time: " + (end - start) + "ms");
            RandomAccessFile file = new RandomAccessFile("/home/jules/Downloads/stackoverflow.com-Posts/Posts.xml", "r");
            for (ScoreDoc score_doc: hits.scoreDocs) {
                Document document = searcher.getDocument(score_doc);

                System.out.println("----------------------------------------");
                for (IndexableField field: document.getFields())
                    System.out.println(field.name() + "\t" + field.stringValue());
                if (document.getField("id") != null)
                    System.out.println(getPost(file, Integer.parseUnsignedInt(document.getField("id").stringValue())));
            }
            System.out.println("----------------------------------------");
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * The constructor; initialises the `indexSearcher` and `queryParser` objects.
     *
     * @param directoryPath The directory used to store the index.
     */
    public Searcher(String directoryPath) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(directoryPath));
        IndexReader reader = DirectoryReader.open(dir);
        indexSearcher = new IndexSearcher(reader);
        queryParser = new QueryParser(Constants.contents, new StandardAnalyzer());
    }

    /**
     * Searches the collection of documents for documents matching the given query.
     *
     * @param searchQuery The query for which documents need to be returned.
     * @return The documents matching the search query.
     */
    public TopDocs search(String searchQuery) throws IOException, ParseException {
        query = queryParser.parse(searchQuery);
        // Can be any number, shows that amount of searches
        return indexSearcher.search(query, Constants.MAX_AMOUNT);
    }

    /**
     * Returns the `Document` object associated with the score that was returned by `search()`.
     *
     * @param scoreDoc The scored document returned by `search()`.
     * @return The associated `Document` object.
     */
    public Document getDocument(ScoreDoc scoreDoc) throws IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }

    /**
     * Returns the post with the given ID as an XML element.
     *
     * @param dump_file The opened stackoverflow dump file.
     *
     * @return The complete post as an XML element.
     */
    static public String getPostXML(RandomAccessFile dump_file) throws IOException {
        // Go to the start of the current element
        while (dump_file.read() != '<')
            dump_file.seek(dump_file.getFilePointer() - 2);

        // Verify that we're in a 'row' element
        byte[] temp = new byte[3];
        if (dump_file.read(temp) == -1 || !Arrays.equals(temp, "row".getBytes()))
            return "";

        // Read the current element, assuming that there are no children
        StringBuilder element = new StringBuilder("<row");
        char character;
        do {
            character = (char) dump_file.read();
            element.append(character);
        } while (character != '>');

        return element.toString();
    }

    /**
     * Returns the post ID of the first post that comes after the current file pointer. This function was written very
     * specifically for the downloaded stackoverflow dump file, which means that this can't be used for (most) other XML
     * files.
     *
     * @param dump_file The opened stackoverflow dump file.
     *
     * @return The current post's ID, or -1 if something went wrong.
     */
    static private int getPostId(RandomAccessFile dump_file) throws IOException {
        // Get the current XML element from the file
        String element = getPostXML(dump_file);
        if (element.isEmpty())
            return -1;

        // Find the element's 'Id' attribute, assuming that attributes are separated by (any number of) spaces
        int location = element.indexOf(" Id=\"");
        if (location == -1)
            return -1;

        // Read the ID from the string, and then convert it to an actual integer
        StringBuilder result = new StringBuilder();
        for (int index = location + 5; element.charAt(index) != '"'; ++index)
            result.append(element.charAt(index));
        return Integer.parseUnsignedInt(result.toString());
    }

    static public String getPost(RandomAccessFile dump_file, int post_id) throws IOException {
        long interval_start = 0;
        long interval_end = dump_file.length();
        long current_position;
        int current_post;

        do {
            current_position = (interval_start + interval_end) / 2;
            dump_file.seek(current_position);
            current_post = getPostId(dump_file);

            if (current_post < post_id)         // We're before the target
                interval_start = current_position;
            else if (current_post > post_id)    // We're after the target
                interval_end = current_position;
        } while (current_post != post_id);

        return getPostXML(dump_file);
    }
}
