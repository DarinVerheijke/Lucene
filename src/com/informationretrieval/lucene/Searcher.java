package com.informationretrieval.lucene;

import java.io.IOException;
import java.nio.file.Paths;

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

// Based on tutorials on https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm and http://www.lucenetutorial.com/sample-apps/textfileindexer-java.html

public class Searcher {
    IndexSearcher indexSearcher;
    QueryParser queryParser;
    Query query;
    public Searcher(String directoryPath) throws IOException{
        Directory dir = FSDirectory.open(Paths.get(directoryPath));
        IndexReader reader = DirectoryReader.open(dir);
        indexSearcher = new IndexSearcher(reader);
        queryParser = new QueryParser(Constants.contents, new StandardAnalyzer());
    }

    public TopDocs search(String searchQuery) throws IOException, ParseException{
        query = queryParser.parse(searchQuery);
        // Can be any number, shows that amount of searches
        return indexSearcher.search(query, Constants.MAX_AMOUNT);
    }

    public Document getDocument(ScoreDoc scoreDoc) throws CorruptIndexException, IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }
}
