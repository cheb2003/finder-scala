package my.finder.search.service;

import my.finder.common.util.Util;
import my.finder.search.web.util.Index;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
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
import java.util.Date;
import java.util.List;


/**
 *
 */

public class SearcherManager {
    private Logger logger = LoggerFactory.getLogger(SearcherManager.class);

    private Index ddIndex = new Index();
    @Value("#{conf.workDir}")
    private String wordDir;

    public void init(){
        IndexReader reader;
        try {
            Directory dir= FSDirectory.open(new File(wordDir));
            reader = DirectoryReader.open(dir);
        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
        ddIndex.setSearcher(new IndexSearcher(reader));
        //ddSearcher =
    }
    public IndexSearcher getSearcher(String name) {
        return ddIndex.getSearcher();
    }

    public void updateIncrementalIndex(String name, Date date) {
        logger.info("receive index incremental {},{}",name,date);
        try {
            if (ddIndex.getDate().equals(date)) {
                logger.info("update index incremental {},{}",name,date);
                DirectoryReader oldReader = ddIndex.getInc();
                if (oldReader == null) {
                    Directory dirInc = FSDirectory.open(new File(wordDir + Util.getIncrementalPath(name, date)));
                    try{
                        oldReader = DirectoryReader.open(dirInc);
                    } catch (IndexNotFoundException e){

                    }
                    ddIndex.setInc(oldReader);
                } else {
                    DirectoryReader newReader = DirectoryReader.openIfChanged(oldReader);
                    ddIndex.setInc(newReader);
                    MultiReader multiReader = new MultiReader(ddIndex.getMajor(),ddIndex.getInc());
                    IndexSearcher newSearcher = new IndexSearcher(multiReader);
                    ddIndex.setSearcher(newSearcher);
                    oldReader.close();
                }

            }
        } catch (IOException e) {
            logger.error("{}",e);
        }
    }

    public void changeSearcher(String name, Date date) {
        logger.info("change index {},{}",name,date);
        DirectoryReader reader;
        DirectoryReader readerIncrement = null;
        try {
            Directory dir = FSDirectory.open(new File(wordDir + Util.getKey(name, date)));
            Directory dirInc = FSDirectory.open(new File(wordDir + Util.getIncrementalPath(name, date)));
            reader = DirectoryReader.open(dir);
            try{
                readerIncrement = DirectoryReader.open(dirInc);
            } catch (IndexNotFoundException e){

            }

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
                ddIndex.setDate(date);
                ddIndex.setName(name);
                if (ddIndex.getMajor() != null) {
                    ddIndex.getMajor().close();
                }
                ddIndex.setMajor(reader);
                if (ddIndex.getInc() != null) {
                    ddIndex.getInc().close();
                }
                ddIndex.setInc(readerIncrement);
            }
        } catch (IOException e) {
            logger.error("{}", e);
            throw new RuntimeException(e);
        }
    }
}
