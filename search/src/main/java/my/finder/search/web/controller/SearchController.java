package my.finder.search.web.controller;

import my.finder.index.Analyzer.MyAnalyzer;
import my.finder.search.service.SearcherManager;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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

    @RequestMapping(value = "/product/json", method = RequestMethod.POST,  produces = "application/json;charset=utf-8")
    @ResponseBody
    public String searchJson(HttpServletRequest request) {
        return search(request,"json");
    }
    @RequestMapping(value = "/product/xml", method = RequestMethod.POST,produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String searchXml(HttpServletRequest request) {
        return search(request,"xml");
    }

    @RequestMapping(value = "/product/shop/json",method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public String searchByShopJSON(HttpServletRequest request) {
        return searchByShopFormat(request,"json");
    }
    @RequestMapping(value = "/product/shop/xml",method = RequestMethod.POST, produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String searchByShopXML(HttpServletRequest request) {
        return searchByShopFormat(request,"xml");
    }

    @RequestMapping(value = "/product/category/json",method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public String searchByCategoryJSON(HttpServletRequest request) {
        return searchByCategoryFormat(request,"json");
    }
    @RequestMapping(value = "/product/category/xml",method = RequestMethod.POST, produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String searchByCategoryXML(HttpServletRequest request) {
        return searchByCategoryFormat(request,"xml");
    }

    @RequestMapping(value = "/product/newarrivals/xml",method = RequestMethod.POST, produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String newArrivalsXML(HttpServletRequest request) {
        return searchByNewArrivalsFormat(request, "xml");
    }
    @RequestMapping(value = "/product/newarrivals/json",method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public String newArrivalsJSON(HttpServletRequest request) {
        return searchByNewArrivalsFormat(request,"json");
    }

    private String searchByNewArrivalsFormat(HttpServletRequest request, String format) {
        Map<String, Object> result = new HashMap<String, Object>();

        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"),"");
        String fromTime = StringUtils.defaultIfBlank(request.getParameter("fromTime"),"");
        String toTime = StringUtils.defaultIfBlank(request.getParameter("toTime"),"");
        String indexCode = StringUtils.defaultIfBlank(request.getParameter("indexCode"),"");

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


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }

        Query condition;
        QueryParser parser = new QueryParser(Version.LUCENE_40,"pName", new MyAnalyzer());
        BooleanQuery bq = new BooleanQuery();
        if (!"".equals(indexCode)) {
            try {
                condition = parser.parse("indexCode:" + indexCode + "*");
                bq.add(condition, BooleanClause.Occur.MUST);
            } catch (Exception e) {
                return getErrorJson("parse indexCode query failed,value:%s", indexCode);
            }

        }
        TermRangeQuery q = new TermRangeQuery("createTime", new BytesRef(fromTime), new BytesRef(toTime), true, true);
        bq.add(q, BooleanClause.Occur.MUST);
        IndexSearcher searcher = searcherManager.getSearcher("dd_product");
        if (searcher == null) {
            return empty();
        }
        int start = (currentPage - 1) * size + 1;
        //排序
        Sort sot = sorts(sort);
        //分页
        try {
            TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            logger.info("{}",bq);
            searcher.search(bq, tsdc);
            result.put("totalHits", tsdc.getTotalHits());

            //从0开始计算
            TopDocs topDocs = tsdc.topDocs(start - 1, size);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            List<Long> ids = new ArrayList<Long>();
            Document doc = DocumentHelper.parseText("<root/>");
            Element docs = doc.getRootElement().addElement("docs");
            for (int i = 0; i < scoreDocs.length; i++) {
                org.apache.lucene.document.Document indexDoc = searcher.getIndexReader().document(scoreDocs[i].doc);
                ids.add(Long.valueOf(indexDoc.get("pId")));
                if ("xml".equals(format)) {
                    docToXML(docs,indexDoc);
                }
            }
            result.put("productIds", ids);
            if("xml".equals(format)) {
                return doc.asXML();
            }
            if("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}",e);
        }
        return empty();
    }

    @RequestMapping(value = "/product/test/xml",method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
    @ResponseBody
    private String testProduct(HttpServletRequest request, String format) {
        format = "xml";
        Map<String, Object> result = new HashMap<String, Object>();
        //String shop = StringUtils.defaultIfBlank(request.getParameter("shop"), "");
        String search = StringUtils.defaultIfBlank(request.getParameter("search"), "");
        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"),"");
        /*if ("".equals(shop)) {
            return empty();
        }
        shop = "\"" + shop + "\"";*/
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


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }
        QueryParser parser = new QueryParser(Version.LUCENE_40,"sku", new KeywordAnalyzer());
        IndexSearcher searcher = searcherManager.getSearcher("dd_product");
        if (searcher == null) {
            return empty();
        }
        int start = (currentPage - 1) * size + 1;
        //排序
        Sort sot = sorts(sort);
        //分页
        try {
            //TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            //Query q = parser.parse(search);
            /*Term t = new Term("sku", search);
            TermQuery tq = new TermQuery(t);*/
            Query q = parser.parse(search);
            logger.info("{}", q);
            TopDocs topDocs1 = searcher.search(q, Integer.MAX_VALUE);
            result.put("totalHits", topDocs1.totalHits);

            logger.info("-----------------{}",topDocs1.totalHits) ;
            return String.valueOf(topDocs1.totalHits);
            //从0开始计算
            //TopDocs topDocs = topDocs1.topDocs(start - 1, size);
            /*ScoreDoc[] scoreDocs = topDocs1.scoreDocs;
            List<Long> ids = new ArrayList<Long>();
            Document doc = DocumentHelper.parseText("<root/>");
            Element docs = doc.getRootElement().addElement("docs");
            for (int i = 0; i < scoreDocs.length; i++) {
                org.apache.lucene.document.Document indexDoc = searcher.getIndexReader().document(scoreDocs[i].doc);
                ids.add(Long.valueOf(indexDoc.get("pId")));
                if ("xml".equals(format)) {
                    docToXML(docs, indexDoc);
                }
            }

            result.put("productIds", ids);*/
            /*if ("xml".equals(format)) {
                return doc.asXML();
            }
            if ("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }*/
        } catch (Exception e) {
            logger.error("{}",e);
        }
        return empty();
    }



    private String search(HttpServletRequest request,String format) {
        String keyword = StringUtils.defaultIfBlank(request.getParameter("keyword"), "").toLowerCase();
        if ("".equals(keyword)) {
            return empty();
        }
        Map<String, Object> result = new HashMap<String, Object>();
        String country = StringUtils.defaultIfBlank(request.getParameter("country"), "");
        String indexCode = StringUtils.defaultIfBlank(request.getParameter("indexCode"), "");
        String[] ranges = StringUtils.defaultIfBlank(request.getParameter("range"), "").split(",");
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

            QueryParser parser = new QueryParser(Version.LUCENE_40,"pName", new MyAnalyzer());
            String langName = null;
            if ("ru".equals(country)) {
                langName = "pNameRU";
            }
            if ("br".equals(country)) {
                langName = "pNameBR";
            }
            BooleanQuery bq = new BooleanQuery();
            BooleanQuery bqLang = new BooleanQuery();
            BooleanQuery bqKeyRu = new BooleanQuery();
            BooleanQuery bqKeyEn = new BooleanQuery();
            if (langName != null) {
                for (String k : keywords) {
                    Term term = new Term(langName, k);
                    PrefixQuery pq = new PrefixQuery(term);
                    pq.setBoost(5.0f);
                    bqKeyRu.add(pq, BooleanClause.Occur.MUST);
                }
                bqLang.add(bqKeyRu, BooleanClause.Occur.SHOULD);
            }


            Query condition;
            for (String k : keywords) {
                Term term = new Term("pName", k);
                PrefixQuery pq = new PrefixQuery(term);
                bqKeyEn.add(pq, BooleanClause.Occur.MUST);
            }

            bqLang.add(bqKeyEn, BooleanClause.Occur.SHOULD);
            bq.add(bqLang, BooleanClause.Occur.MUST);
            if (!"".equals(indexCode)) {
                try {
                    condition = parser.parse("indexCode:" + indexCode + "*");
                    bq.add(condition, BooleanClause.Occur.MUST);
                } catch (Exception e) {
                    return getErrorJson("parse indexCode query failed,value:%s", indexCode);
                }

            }
            ranges(ranges, bq);
            Sort sot = sorts(sort);

            IndexSearcher searcher = searcherManager.getSearcher("dd_product");
            if (searcher == null) {
                return empty();
            }
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
            Document doc = DocumentHelper.parseText("<root/>");
            Element docs = doc.getRootElement().addElement("docs");
            for (int i = 0; i < scoreDocs.length; i++) {
                org.apache.lucene.document.Document indexDoc = searcher.getIndexReader().document(scoreDocs[i].doc);
                ids.add(Long.valueOf(indexDoc.get("pId")));
                if ("xml".equals(format)) {
                    docToXML(docs,indexDoc);
                }
            }
            result.put("productIds", ids);
            if("xml".equals(format)) {
                return doc.asXML();
            }
            if("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}",e);
        }
        return empty();
    }

    private Sort sorts(String sort) {
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
        return sot;
    }

    private void ranges(String[] ranges, BooleanQuery bq) {
        //范围查询

        for (String range : ranges) {
            String[] parts = range.split(":");
            if (parts.length == 3) {
                if (parts[0].equals("price")) {
                    NumericRangeQuery nrq = NumericRangeQuery.newDoubleRange("unitPrice",Double.valueOf(parts[1]),Double.valueOf(parts[2]),true,true);
                    bq.add(nrq, BooleanClause.Occur.MUST);
                }
                if (parts[0].equals("createTime")) {
                    TermRangeQuery query = new TermRangeQuery("createTime", new BytesRef(parts[1]), new BytesRef(parts[2]), true, true);
                    bq.add(query, BooleanClause.Occur.MUST);
                }
            }

        }
    }



    private String searchByShopFormat(HttpServletRequest request, String format) {
        Map<String, Object> result = new HashMap<String, Object>();
        String shop = StringUtils.defaultIfBlank(request.getParameter("shop"), "").toLowerCase();
        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"),"");
        if ("".equals(shop)) {
            return empty();
        }
        shop = "\"" + shop + "\"";
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


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }
        QueryParser parser = new QueryParser(Version.LUCENE_40,"businessName", new KeywordAnalyzer());
        IndexSearcher searcher = searcherManager.getSearcher("dd_product");
        if (searcher == null) {
            return empty();
        }
        int start = (currentPage - 1) * size + 1;
        //排序
        Sort sot = sorts(sort);
        //分页
        try {
            TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            Query q = parser.parse(shop);
            logger.info("{}",q);
            searcher.search(q, tsdc);
            result.put("totalHits", tsdc.getTotalHits());

            //从0开始计算
            TopDocs topDocs = tsdc.topDocs(start - 1, size);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            List<Long> ids = new ArrayList<Long>();
            Document doc = DocumentHelper.parseText("<root/>");
            Element docs = doc.getRootElement().addElement("docs");
            for (int i = 0; i < scoreDocs.length; i++) {
                org.apache.lucene.document.Document indexDoc = searcher.getIndexReader().document(scoreDocs[i].doc);
                ids.add(Long.valueOf(indexDoc.get("pId")));
                if ("xml".equals(format)) {
                    docToXML(docs,indexDoc);
                }
            }
            result.put("productIds", ids);
            if("xml".equals(format)) {
                return doc.asXML();
            }
            if("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}",e);
        }
        return empty();
    }

    private String searchByCategoryFormat(HttpServletRequest request, String format) {
        Map<String, Object> result = new HashMap<String, Object>();
        String indexCode = StringUtils.defaultIfBlank(request.getParameter("indexCode"), "");
        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"),"");
        if ("".equals(indexCode)) {
            return empty();
        }

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


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }
        QueryParser parser = new QueryParser(Version.LUCENE_40,"indexCode", new MyAnalyzer());
        IndexSearcher searcher = searcherManager.getSearcher("dd_product");
        if (searcher == null) {
            return empty();
        }
        int start = (currentPage - 1) * size + 1;
        //排序
        Sort sot = sorts(sort);
        //分页
        try {
            TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            Query q = parser.parse(indexCode + "*");
            logger.info("{}",q);
            searcher.search(q, tsdc);
            result.put("totalHits", tsdc.getTotalHits());

            //从0开始计算
            TopDocs topDocs = tsdc.topDocs(start - 1, size);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            List<Long> ids = new ArrayList<Long>();
            Document doc = DocumentHelper.parseText("<root/>");
            Element docs = doc.getRootElement().addElement("docs");
            for (int i = 0; i < scoreDocs.length; i++) {
                org.apache.lucene.document.Document indexDoc = searcher.getIndexReader().document(scoreDocs[i].doc);
                ids.add(Long.valueOf(indexDoc.get("pId")));
                if ("xml".equals(format)) {
                    docToXML(docs,indexDoc);
                }
            }
            result.put("productIds", ids);
            if("xml".equals(format)) {
                return doc.asXML();
            }
            if("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}",e);
        }
        return empty();
    }
    private String empty() {
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
