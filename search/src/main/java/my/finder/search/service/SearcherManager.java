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
    private Map<String, IndexSearcher> indexSearcherMap = new HashMap<String, IndexSearcher>();
    @Value("#{conf.indexDir}")
    private String indexDir;
    public void init(){
        IndexReader reader;
        try {
            Directory dir= FSDirectory.open(new File("d:\\indexs\\ddProduct_2013-05-15-18-47-18\\final\\"));
            reader = DirectoryReader.open(dir);
        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
        IndexSearcher searcher = new IndexSearcher(reader);
        indexSearcherMap.put("dd_product",searcher);

        /*IndexReader reader1;
        try {
            Directory dir= FSDirectory.open(new File(indexDir + "/" + "dd_attributes"));
            reader1 = DirectoryReader.open(dir);
        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
        IndexSearcher searcher1 = new IndexSearcher(reader1);
        indexSearcherMap.put("dd_attributes",searcher1);*/
    }
    public IndexSearcher getSearcher(String name) {
        return indexSearcherMap.get(name);
    }
}
