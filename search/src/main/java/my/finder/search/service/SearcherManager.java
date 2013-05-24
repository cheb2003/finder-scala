package my.finder.search.service;

import my.finder.common.util.Util;
import my.finder.search.web.util.Index;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 *
 */

public class SearcherManager {
    private Logger logger = LoggerFactory.getLogger(SearcherManager.class);

    private Index ddIndex = new Index();
    @Value("#{conf.workDir}")
    private String wordDir;

    /*public void init(){
        IndexReader reader;
        try {
            Directory dir= FSDirectory.open(new File(wordDir));
            reader = DirectoryReader.open(dir);
        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
        ddSearcher = new IndexSearcher(reader);
    }*/
    public IndexSearcher getSearcher(String name) {
        return ddIndex.getSearcher();
    }

    public void updateIncrementalIndex(String name, String id) {
        try {
            if (ddIndex.getId().equals(id)) {
                DirectoryReader oldReader = ddIndex.getInc();
                DirectoryReader newReader = DirectoryReader.openIfChanged(oldReader);
                ddIndex.setInc(newReader);
                MultiReader multiReader = new MultiReader(ddIndex.getMajor(),ddIndex.getInc());
                IndexSearcher newSearcher = new IndexSearcher(multiReader);
                ddIndex.setSearcher(newSearcher);
                oldReader.close();
            }
        } catch (IOException e) {
            logger.error("{}",e);
        }
    }

    public void changeSearcher(String name, String id) {
        DirectoryReader reader;
        DirectoryReader readerIncrement;
        try {
            Directory dir = FSDirectory.open(new File(wordDir + Util.getKey(name, id)));
            Directory dirInc = FSDirectory.open(new File(wordDir + Util.getIncrementalPath(name, id)));
            reader = DirectoryReader.open(dir);
            readerIncrement = DirectoryReader.open(dirInc);
            List<IndexReader> lst = new ArrayList<IndexReader>();
            if (reader != null) {
                lst.add(reader);
            }
            if (readerIncrement != null) {
                lst.add(readerIncrement);
            }
            if (lst.size() > 0) {
                MultiReader multiReader = new MultiReader(lst.toArray(new IndexReader[]{}));
                IndexSearcher searcher = new IndexSearcher(multiReader);
                ddIndex.setSearcher(searcher);
                ddIndex.setId(id);
                ddIndex.setName(name);
                if (ddIndex.getMajor() != null) {
                    ddIndex.getMajor().close();
                    ddIndex.setMajor(reader);
                }
                if (ddIndex.getInc() != null) {
                    ddIndex.getInc().close();
                    ddIndex.setInc(readerIncrement);
                }
            }
        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
    }
}
