package com.informationretrieval.lucene;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

// Based on tutorials on https://www.tutorialspoint.com/lucene/lucene_indexing_process.htm and
//  http://www.lucenetutorial.com/sample-apps/textfileindexer-java.html

/**
 * A class that offers indexing functionality. The created indices are stored in the directory that is passed to the
 *  object's constructor. Creating indices can be done by either passing a `File` object to `indexFile()` or by passing
 *  a directory path to `createIndex()`.
 */
public class Indexer {
    private IndexWriter writer;


    /**
     * The constructor; initialises the `writer` object and opens the indexing directory.
     *
     * @param directoryPath The directory used to store the indices.
     */
    public Indexer(String directoryPath) throws IOException {

        Directory dir = FSDirectory.open(Paths.get(directoryPath));
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
     *  contents, file_name, file_path.
     *
     * @param file The `File` object for which a `Document` object is to be created.
     *
     * @return The new `Document` object.
     */
    private Document getDocument(File file) throws IOException{
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
    private void indexFile(File file) throws IOException{
        System.out.println("Indexing" + file.getCanonicalPath());
        Document doc = getDocument(file);
        writer.addDocument(doc);
    }

    /**
     * Adds all files in a directory to the index.
     *
     * @param directoryPath The directory path containing the files that are to be added to the index.
     *
     * @return The number of files that were added to the index.
     */
    public int createIndex(String directoryPath) throws IOException{
        File[] files = new File(directoryPath).listFiles();
        for (File file : files) {
            if(!file.isDirectory() && !file.isHidden() && file.exists() && file.canRead()
            ){
                indexFile(file);
            }
        }
        return writer.numRamDocs();
    }
}
