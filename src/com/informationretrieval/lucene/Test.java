package com.informationretrieval.lucene;

import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

// Based on tutorials on https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm and http://www.lucenetutorial.com/sample-apps/textfileindexer-java.html

public class Test {
    String indexDirectory = "/home/darin/IdeaProjects/Lucene/Index";
    String dataDirectory = "/home/darin/IdeaProjects/Lucene/Data";
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
        indexer = new Indexer(indexDirectory);
        int num;
        long start = System.currentTimeMillis();
        num = indexer.createIndex(dataDirectory);
        long end = System.currentTimeMillis();
        indexer.close();
        System.out.println(num+"Indexed, time: "+(end-start)+"ms");
    }

    private void search(String searchQuery) throws IOException, ParseException {
        searcher = new Searcher(indexDirectory);
        long start = System.currentTimeMillis();
        TopDocs hits = searcher.search(searchQuery);
        long end = System.currentTimeMillis();
        System.out.println(hits.totalHits + " documents found. Time: "+(end-start));
        for (ScoreDoc scoreDoc: hits.scoreDocs){
            Document doc = searcher.getDocument(scoreDoc);
        }
    }
}
