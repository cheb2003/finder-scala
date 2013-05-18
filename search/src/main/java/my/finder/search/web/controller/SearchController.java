package my.finder.search.web.controller;

import my.finder.search.service.SearcherManager;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

/**
 *
 */
@Controller
@RequestMapping("/search")
public class SearchController {
    private Logger logger = LoggerFactory.getLogger(SearchController.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private SearcherManager searcherManager;

    @RequestMapping(value = "/product/json/", produces = "text/plain;charset=utf-8")
    @ResponseBody
    public String modifyPassword(HttpServletRequest request) {
        String error = null;
        Map<String, Object> result = new HashMap<String, Object>();
        String country = StringUtils.defaultIfBlank(request.getParameter("country"), "");
        String indexCode = StringUtils.defaultIfBlank(request.getParameter("indexCode"), "");
        String keyword = StringUtils.defaultIfBlank(request.getParameter("keyword"), "");
        Integer currentPage = null;
        try {
            currentPage = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("currentPage"), "1"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse currentPage failed,value:%s",currentPage));
        }
        Integer size = null;
        try {
            size = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("size"), "20"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse size failed,value:%s",size));
        }


        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"),"");

        if (currentPage < 1) {
            currentPage = 1;
        }

            if (size < 1 || size > 100) {
            size = 1;
        }

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
            }

            bq.add(bqKeyRu, BooleanClause.Occur.SHOULD);
            Query condition = null;
            for (String k : keywords) {
                Term term = new Term("pName", k);
                PrefixQuery pq = new PrefixQuery(term);
                bqKeyEn.add(pq, BooleanClause.Occur.MUST);
            }
            bq.add(bqKeyEn, BooleanClause.Occur.SHOULD);
            if (!"".equals(indexCode)) {
                try {
                    condition = parser.parse("indexCode:" + indexCode + "*");
                } catch (Exception e) {
                    return getErrorJson("parse indexCode query failed,value:%s", indexCode);
                }

            }
            //范围查询
            String[] ranges = StringUtils.defaultIfBlank(request.getParameter("range"), "").split(",");
            for (String range : ranges) {
                String[] parts = range.split(":");
                if (parts.length == 3) {
                    if (parts[0].equals("unitPrice")) {
                        NumericRangeQuery nrq = NumericRangeQuery.newDoubleRange("unitPrice",Double.valueOf(parts[1]),Double.valueOf(parts[2]),true,true);
                        bq.add(nrq, BooleanClause.Occur.MUST);
                    }
                    if (parts[0].equals("createTime")) {
                        TermRangeQuery query = new TermRangeQuery("createTime", new BytesRef(parts[1]), new BytesRef(parts[2]), true, true);
                        bq.add(query, BooleanClause.Occur.MUST);
                    }
                }

            }
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
            int start = (currentPage - 1) * size + 1;
            //分页
            TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            logger.info("{}",bq);
            searcher.search(bq, tsdc);
            result.put("totalHits", tsdc.getTotalHits());

            //从0开始计算
            TopDocs topDocs = tsdc.topDocs(start - 1, size);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            List<Long> ids = new ArrayList<Long>();
            for (int i = 0; i < scoreDocs.length; i++) {
                ids.add(Long.valueOf(searcher.getIndexReader().document(scoreDocs[i].doc).get("pId")));
            }
            result.put("productIds", ids);

            return objectMapper.writeValueAsString(result);
        } catch (IOException e) {
            logger.error("{}",e);
        }
        return "{\"totalHits\":0,\"productIds\":[]}";
    }



    private void docToXML(Element root,org.apache.lucene.document.Document document) {
        Element docEle = root.addElement("doc");
        List<IndexableField> fields = document.getFields();
        for (IndexableField field : fields) {
            docEle.addElement(field.name()).addCDATA(field.stringValue());
        }
    }

    private String getErrorJson(String error,Object ... parmas){
        error = String.format(error,parmas);
        logger.error(error);
        Map<String,String> map = new HashMap<String,String>();
        map.put("error", error);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (IOException e) {
            logger.error("{}",e);
        }
        return "";
    }
}
