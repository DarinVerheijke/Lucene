package com.informationretrieval.lucene;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

// Based on tutorials on https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm and
//  http://www.lucenetutorial.com/sample-apps/textfileindexer-java.html

public class Test {
    Indexer indexer;
    Searcher searcher;

    public static void main(String[] args){
        Test tester;
        try {
            tester = new Test();
            tester.createIndex();
            tester.search("test");

        } catch (IOException e){
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    private void createIndex() throws IOException {
        indexer = new Indexer(Constants.index_dir);
        int num;
        long start = System.currentTimeMillis();
        num = indexer.addToIndex(Constants.data_dir);
        long end = System.currentTimeMillis();
        indexer.close();
        System.out.println(num+"Indexed, time: "+(end-start)+"ms");
    }

    private void search(String searchQuery) throws IOException, ParseException {
        searcher = new Searcher(Constants.index_dir);
        long start = System.currentTimeMillis();
        TopDocs hits = searcher.search(searchQuery);
        long end = System.currentTimeMillis();
        System.out.println(hits.totalHits + " documents found. Time: "+(end-start));
        for (ScoreDoc scoreDoc: hits.scoreDocs){
            Document doc = searcher.getDocument(scoreDoc);
        }
    }
}
