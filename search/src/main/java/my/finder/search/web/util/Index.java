package my.finder.search.web.util;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

/**
 *
 */
public class Index {
    private String name;
    private String id;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    private DirectoryReader inc;

}
