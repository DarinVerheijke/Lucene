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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Vector;

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
     * @param args The command line arguments passed to this script. There are a couple of options available that allow
     *             you to change the directory used to store the index, and to specify the path to a stackoverflow dump.
     *             The other arguments are the query parameters. Additional information can be found in `usage` below.
     */
    public static void main(String[] args) {
        final String usage = "Searcher [options] query_param+\n" +
                " -h,--help    Display the available options and required arguments.\n" +
                " -i,--index   The directory that stores the index. [default = ./Index]\n" +
                " -d,--dump    The stackoverflow dump file (if that's what was indexed). [default = ./Posts.xml]\n" +
                " query_param+ The query parameters.\n";
        String index_dir = Constants.index_dir;
        String dump_file = Constants.dump_file;
        Vector<String> query_params = new Vector<>();

        // Parse the argument string
        for (int index = 0; index < args.length; ++index) {
            switch (args[index]) {
                case "-h", "--help" -> {
                    System.out.println("Usage: " + usage);
                    return;
                }
                case "-i", "--index" -> index_dir = args[++index];
                case "-d", "--dump" -> dump_file = args[++index];
                default -> query_params.add(args[index]);
            }
        }
        // If there is no query we can't execute it either
        if (query_params.isEmpty())
            throw new IllegalArgumentException("Expected at least one query parameter, usage:\n\t" + usage);

        try {
            System.out.println("Searching through index in " + index_dir);

            long start = System.currentTimeMillis();
            Searcher searcher = new Searcher(index_dir);
            TopDocs hits = searcher.search(String.join(" ", query_params));

            for (ScoreDoc score_doc : hits.scoreDocs) {
                Document document = searcher.getDocument(score_doc);

                System.out.println("----------------------------------------");
                for (IndexableField field : document.getFields())
                    System.out.println(field.name() + "\t" + field.stringValue());

                // If the document has an ID field, then we assume that it was indexed from the stackoverflow dump
                // This means that we can use it to retrieve the complete XML element from the dump file
                if (document.getField("id") != null) {
                    RandomAccessFile file = new RandomAccessFile(dump_file, "r");
                    String element = getPost(file, Integer.parseUnsignedInt(document.getField("id").stringValue()));
                    System.out.println(element.replace("\" ", "\"\n\t"));
                    file.close();
                }
            }
            System.out.println("----------------------------------------");

            long end = System.currentTimeMillis();
            System.out.println(hits.totalHits + " documents found, time: " + (end - start) + "ms");
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
     * @return The complete post as an XML element.
     */
    static private String getCurrentPost(RandomAccessFile dump_file) throws IOException {
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
     * @return The current post's ID, or -1 if something went wrong.
     */
    static private int getPostId(RandomAccessFile dump_file) throws IOException {
        // Get the current XML element from the file
        String element = getCurrentPost(dump_file);
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

    /**
     * Returns the XML element in the stackoverflow dump file with the given ID. This function assumes that the XML file
     * is ordered on this ID attribute (low to high). This allows us to use a binary search algorithm.
     *
     * @param dump_file The opened stackoverflow dump file.
     * @param post_id The ID of the post that we're looking for.
     * @return The XML element with the given ID.
     */
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

        return getCurrentPost(dump_file);
    }
}
