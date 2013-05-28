package my.finder.search.web.util;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import java.util.Date;

/**
 *
 */
public class Index {
    private String name;
    private DirectoryReader inc;
    private Date date;
    private DirectoryReader major;

    public IndexSearcher getSearcher() {
        return searcher;
    }

    public void setSearcher(IndexSearcher searcher) {
        this.searcher = searcher;
    }

    private IndexSearcher searcher;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }



    public IndexReader getMajor() {
        return major;
    }

    public void setMajor(DirectoryReader major) {
        this.major = major;
    }

    public DirectoryReader getInc() {
        return inc;
    }

    public void setInc(DirectoryReader inc) {
        this.inc = inc;
    }



    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
