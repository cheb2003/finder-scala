package my.finder.search.service;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */

public class SearcherManager {
    private Logger logger = LoggerFactory.getLogger(SearcherManager.class);

    IndexSearcher ddSearcher = null;
    @Value("#{conf.indexDir}")
    private String indexDir;
    public void init(){
        IndexReader reader;
        try {
            Directory dir= FSDirectory.open(new File(indexDir));
            reader = DirectoryReader.open(dir);
        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
        ddSearcher = new IndexSearcher(reader);
    }
    public IndexSearcher getSearcher(String name) {
        return ddSearcher;
    }

    public void changeSearcher(String name, String id) {
        IndexReader reader;
        IndexReader readerIncrement;
        try {
            Directory dir= FSDirectory.open(new File(indexDir));
            reader = DirectoryReader.open(dir);
            readerIncrement = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            ddSearcher = searcher;

        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
    }
}
