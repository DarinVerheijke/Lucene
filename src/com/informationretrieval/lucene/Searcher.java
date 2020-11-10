package com.informationretrieval.lucene;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import javax.print.Doc;
import java.io.IOException;
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
            for (ScoreDoc score_doc: hits.scoreDocs) {
                Document document = searcher.getDocument(score_doc);
                System.out.println("(" + score_doc.score + ") \t" + document.getField("filename").stringValue());
            }
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
    public Document getDocument(ScoreDoc scoreDoc) throws CorruptIndexException, IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }
}
