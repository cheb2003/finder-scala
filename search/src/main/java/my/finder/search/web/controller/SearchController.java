package my.finder.search.web.controller;

import my.finder.search.service.SearcherManager;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.Version;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.*;

/**
 *
 */
@Controller
@RequestMapping("/search")
public class SearchController {
    private Logger logger = LoggerFactory.getLogger(SearchController.class);
    @Autowired
    private SearcherManager searcherManager;

    @RequestMapping(value = "/product/xml/{country}/{indexCode}/{keyword}/{currentPage}/{size}/{sort}/{range}", produces = "text/xml;charset=utf-8")
    @ResponseBody
    public Object modifyPassword(@PathVariable String country,@PathVariable String indexCode,@PathVariable String keyword,@PathVariable Integer currentPage,@PathVariable Integer size,
                                 @PathVariable String sort,@PathVariable String ragne) {
        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 1000) {
            size = 1;
        }

        Document doc = null;
        try {
            keyword = keyword.trim();
            keyword = QueryParser.escape(keyword);
            String[] keywords = keyword.split(" ");

            /*String[] keywordSplit = keyword.split(" ");
            StringBuilder sbKeyword = new StringBuilder();
            for (String str : keywordSplit) {
                sbKeyword.append('+').append(str).append(' ');
            }*/
            QueryParser parser = new QueryParser(Version.LUCENE_40,"pName", new StandardAnalyzer(Version.LUCENE_40));
            QueryParser parserRu = new QueryParser(Version.LUCENE_40,"pNameRU", new StandardAnalyzer(Version.LUCENE_40));
            String langName = null;
            if ("ru".equals(country)) {
                langName = "pNameRU";
            }
            BooleanQuery bq = new BooleanQuery();
            BooleanQuery bqKeyRu = new BooleanQuery();
            BooleanQuery bqKeyEn = new BooleanQuery();
            if (langName != null) {
                for (String k : keywords) {
                    Term term = new Term(langName, k);
                    PrefixQuery pq = new PrefixQuery(term);
                    pq.setBoost(5.0f);
                    bqKeyRu.add(pq, BooleanClause.Occur.MUST);
                }

                /*try {
                    Query query = parserRu.parse(keyword);
                    query.setBoost(5.0f);

                } catch (ParseException e) {
                    e.printStackTrace();
                }*/
            }

            bq.add(bqKeyRu, BooleanClause.Occur.SHOULD);
            Query condition = null;
            for (String k : keywords) {
                Term term = new Term("pName", k);
                PrefixQuery pq = new PrefixQuery(term);
                //pq.setBoost(5.0f);
                bqKeyEn.add(pq, BooleanClause.Occur.MUST);
            }
            bq.add(bqKeyEn, BooleanClause.Occur.SHOULD);
            try {
                if(indexCode.equals("*")){
                    //condition = parser.parse(keyword);
                } else {
                    condition = parser.parse(keyword + "indexCode:" + indexCode);
                }
            } catch (ParseException e) {
                logger.error("{}", e);
            }

            //bq.add(condition, BooleanClause.Occur.SHOULD);

            //排序
            SortField sortField = null;
            if ("price-".equals(sort)) {
                sortField = new SortField("unitPrice", SortField.Type.DOUBLE, true);
            } else if ("price+".equals(sort)) {
                sortField = new SortField("unitPrice", SortField.Type.DOUBLE, false);
            } else if ("date-".equals(sort)) {
                sortField = new SortField("createTime", SortField.Type.DOUBLE, true);
            } else if ("date+".equals(sort)) {
                sortField = new SortField("createTime", SortField.Type.DOUBLE, false);
            }
            Sort sot;
            if (sortField == null) {
                //默认按相关度排序
                sortField = SortField.FIELD_SCORE;
                sot = new Sort(sortField);
            } else {
                sot = new Sort(sortField);
            }
            IndexSearcher searcher = searcherManager.getSearcher("dd_product");
            doc = DocumentHelper.parseText("<response></response>");
            Element root = doc.getRootElement();
            int start = (currentPage - 1) * size + 1;
            //分页
            TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            logger.info("{}",bq);
            searcher.search(bq, tsdc);
            root.addElement("totalHits").addText(String.valueOf(tsdc.getTotalHits()));
            //从0开始计算
            TopDocs topDocs = tsdc.topDocs(start - 1, size);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            /*Element productIdsEle = root.addElement("docs").addElement("productIds");
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < scoreDocs.length; i++) {
                sb.append(searcher.getIndexReader().document(scoreDocs[i].doc).get("pId")).append(',');
            }
            productIdsEle.addText(sb.substring(0, sb.length() - 1));*/
            Element docs = root.addElement("docs");
            for (int i = 0; i < scoreDocs.length; i++) {
                docToXML(docs,searcher.getIndexReader().document(scoreDocs[i].doc));
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc.asXML();
    }



    private void docToXML(Element root,org.apache.lucene.document.Document document) {
        Element docEle = root.addElement("doc");
        List<IndexableField> fields = document.getFields();
        for (IndexableField field : fields) {
            docEle.addElement(field.name()).addCDATA(field.stringValue());
        }
    }
}
