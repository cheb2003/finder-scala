package my.finder.search.web.controller;

import my.finder.index.Analyzer.MyAnalyzer;
import my.finder.search.service.SearcherManager;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
@Controller
@RequestMapping("/search")
public class SearchController {
    private Logger logger = LoggerFactory.getLogger(SearchController.class);
    private ObjectMapper objectMapper = new ObjectMapper();
    private String regEx = "[`\\-~!@#$%^&*()+_=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
    private Pattern p = Pattern.compile(regEx);
    @Value("#{conf.workDir}")
    private String workDir;
    @Autowired
    private SearcherManager searcherManager;

    @RequestMapping(value = "/product/json", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public String searchJson(HttpServletRequest request) {
        return search(request, "json");
    }

    @RequestMapping(value = "/product/xml", method = RequestMethod.POST, produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String searchXml(HttpServletRequest request) {
        return search(request, "xml");
    }

    @RequestMapping(value = "/product/shop/json", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public String searchByShopJSON(HttpServletRequest request) {
        return searchByShopFormat(request, "json");
    }

    @RequestMapping(value = "/queryKeyword", produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String queryKeyword(HttpServletRequest request) throws Exception {
        String[] pNames = StringUtils.defaultIfBlank(request.getParameter("pName"), "").toLowerCase().split(" ");
        String[] pNameRUs = StringUtils.defaultIfBlank(request.getParameter("pNameRU"), "").toLowerCase().split(" ");
        String[] pNameCNs = StringUtils.defaultIfBlank(request.getParameter("pNameCN"), "").toLowerCase().split(" ");
        String[] pNameBRs = StringUtils.defaultIfBlank(request.getParameter("pNameBR"), "").toLowerCase().split(" ");
        String[] skus = StringUtils.defaultIfBlank(request.getParameter("sku"), "").toLowerCase().split(" ");
        String[] segmentWordRus = StringUtils.defaultIfBlank(request.getParameter("segmentWordRu"), "").toLowerCase().split(" ");
        String[] segmentWordBrs = StringUtils.defaultIfBlank(request.getParameter("segmentWordBr"), "").toLowerCase().split(" ");
        String[] segmentWordEns = StringUtils.defaultIfBlank(request.getParameter("segmentWordEn"), "").toLowerCase().split(" ");
        String[] sourceKeywords = StringUtils.defaultIfBlank(request.getParameter("sourceKeyword"), "").toLowerCase().split(" ");
        String[] sourceKeywordCNs = StringUtils.defaultIfBlank(request.getParameter("sourceKeywordCN"), "").toLowerCase().split(" ");
        String[] businessBrands = StringUtils.defaultIfBlank(request.getParameter("businessBrands"), "").toLowerCase().split(" ");
        String[] i = StringUtils.defaultIfBlank(request.getParameter("i"), "").toLowerCase().split(" ");

        BooleanQuery bq = new BooleanQuery();
        BooleanQuery bqKeyEn = new BooleanQuery();
        BooleanQuery bqKeyRu = new BooleanQuery();
        BooleanQuery bqKeyBr = new BooleanQuery();
        BooleanQuery bqKeyCn = new BooleanQuery();
        if (pNames.length > 0) {
            for (String k : pNames) {
                if (k.trim() != "") {
                    Term term = new Term("pName", k);
                    PrefixQuery pq = new PrefixQuery(term);
                    bqKeyEn.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqKeyEn, BooleanClause.Occur.SHOULD);
        }

        if (pNameCNs.length > 0) {
            for (String k : pNameCNs) {
                if (k.trim() != "") {
                    Term term = new Term("pNameCN", k);
                    PrefixQuery pq = new PrefixQuery(term);
                    bqKeyCn.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqKeyCn, BooleanClause.Occur.SHOULD);
        }

        if (pNameRUs.length > 0) {
            for (String k : pNameRUs) {
                if (k.trim() != "") {
                    Term term = new Term("pNameRU", k);
                    PrefixQuery pq = new PrefixQuery(term);
                    bqKeyRu.add(pq, BooleanClause.Occur.MUST);
                }

            }
            bq.add(bqKeyRu, BooleanClause.Occur.SHOULD);
        }

        if (pNameBRs.length > 0) {
            for (String k : pNameBRs) {
                if (k.trim() != "") {
                    Term term = new Term("pNameBR", k);
                    PrefixQuery pq = new PrefixQuery(term);
                    bqKeyBr.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqKeyBr, BooleanClause.Occur.SHOULD);
        }


        if (segmentWordEns.length > 0) {
            BooleanQuery bqSegmentWordEns = new BooleanQuery();
            for (String k : segmentWordEns) {
                if (k.trim() != "") {
                    Term term = new Term("segmentWordEn", k);
                    TermQuery pq = new TermQuery(term);
                    bqSegmentWordEns.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqSegmentWordEns, BooleanClause.Occur.SHOULD);
        }

        if (segmentWordRus.length > 0) {
            BooleanQuery bqSegmentWordRus = new BooleanQuery();
            for (String k : segmentWordRus) {
                if (k.trim() != "") {
                    Term term = new Term("segmentWordRu", k);
                    TermQuery pq = new TermQuery(term);
                    bqSegmentWordRus.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqSegmentWordRus, BooleanClause.Occur.SHOULD);
        }

        if (segmentWordBrs.length > 0) {
            BooleanQuery bqSegmentWordBrs = new BooleanQuery();
            for (String k : segmentWordBrs) {
                if (k.trim() != "") {
                    Term term = new Term("segmentWordBr", k);
                    TermQuery pq = new TermQuery(term);
                    bqSegmentWordBrs.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqSegmentWordBrs, BooleanClause.Occur.SHOULD);
        }

        if (sourceKeywords.length > 0) {
            BooleanQuery bqSourceKeywords = new BooleanQuery();
            for (String k : sourceKeywords) {
                if (k.trim() != "") {
                    Term term = new Term("sourceKeyword", k);
                    TermQuery pq = new TermQuery(term);
                    bqSourceKeywords.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqSourceKeywords, BooleanClause.Occur.SHOULD);
        }

        if (sourceKeywordCNs.length > 0) {
            BooleanQuery bqSourceKeywordCns = new BooleanQuery();
            for (String k : sourceKeywordCNs) {
                if (k.trim() != "") {
                    Term term = new Term("sourceKeywordCN", k);
                    TermQuery pq = new TermQuery(term);
                    bqSourceKeywordCns.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqSourceKeywordCns, BooleanClause.Occur.SHOULD);
        }

        if (businessBrands.length > 0) {
            BooleanQuery bqBusinessBrands = new BooleanQuery();
            for (String k : businessBrands) {
                if (k.trim() != "") {
                    Term term = new Term("businessBrand", k);
                    TermQuery pq = new TermQuery(term);
                    bqBusinessBrands.add(pq, BooleanClause.Occur.MUST);
                }
            }
            bq.add(bqBusinessBrands, BooleanClause.Occur.SHOULD);
        }

        Directory dir = FSDirectory.open(new File(workDir));
        DirectoryReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        System.out.println(bq);
        TopDocs topDocs = searcher.search(bq, 1);
        //ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Document doc = DocumentHelper.parseText("<root/>");
        Element total = doc.getRootElement().addElement("total");
        total.addText(String.valueOf(topDocs.totalHits));

    /*for (i <- 0 until scoreDocs.length) {
      val indexDoc = searcher.getIndexReader().document(scoreDocs(i).doc)
      docToXML(nodes,indexDoc)
    }*/
        reader.close();
        return doc.asXML();
    }

    @RequestMapping(value = "/product/shop/xml", method = RequestMethod.POST, produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String searchByShopXML(HttpServletRequest request) {
        return searchByShopFormat(request, "xml");
    }

    @RequestMapping(value = "/product/category/json", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public String searchByCategoryJSON(HttpServletRequest request) {
        return searchByCategoryFormat(request, "json");
    }

    @RequestMapping(value = "/product/category/xml", method = RequestMethod.POST, produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String searchByCategoryXML(HttpServletRequest request) {
        return searchByCategoryFormat(request, "xml");
    }

    @RequestMapping(value = "/product/newarrivals/xml", method = RequestMethod.POST, produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String newArrivalsXML(HttpServletRequest request) {
        return searchByNewArrivalsFormat(request, "xml");
    }

    @RequestMapping(value = "/product/newarrivals/json", method = RequestMethod.POST, produces = "application/json;charset=utf-8")
    @ResponseBody
    public String newArrivalsJSON(HttpServletRequest request) {
        return searchByNewArrivalsFormat(request, "json");
    }

    @RequestMapping(value = "/test/sourcekeyword", produces = "text/xml;charset=utf-8")
    @ResponseBody
    public String testSourceKeyword(HttpServletRequest request) throws IOException, DocumentException {
        String keyword = StringUtils.defaultIfBlank(request.getParameter("keyword"), "").toLowerCase();
        String country = StringUtils.defaultIfBlank(request.getParameter("country"), "").toLowerCase();
        String test = StringUtils.defaultIfBlank(request.getParameter("test"), "").toLowerCase();
        String testkey = StringUtils.defaultIfBlank(request.getParameter("testkey"), "").toLowerCase();
        String langName = null;
        String[] keywords = keyword.split(" ");
        BooleanQuery bq = new BooleanQuery();
        BooleanQuery bqLang = new BooleanQuery();
        BooleanQuery bqKeyRu = new BooleanQuery();
        BooleanQuery bqKeyEn = new BooleanQuery();
        if ("ru".equals(country)) {
            langName = "pNameRU";
        }
        if ("br".equals(country)) {
            langName = "pNameBR";
        }
        if (testkey.equals("1")) {
            if (langName != null) {
                for (String k : keywords) {
                    Term term = new Term(langName, k);
                    PrefixQuery pq = new PrefixQuery(term);
                    pq.setBoost(5.0f);
                    bqKeyRu.add(pq, BooleanClause.Occur.MUST);
                }
                bqLang.add(bqKeyRu, BooleanClause.Occur.SHOULD);
            }
            for (String k : keywords) {
                Term term = new Term("pName", k);
                PrefixQuery pq = new PrefixQuery(term);
                bqKeyEn.add(pq, BooleanClause.Occur.MUST);
            }

            bqLang.add(bqKeyEn, BooleanClause.Occur.SHOULD);
            bq.add(bqLang, BooleanClause.Occur.SHOULD);
        }

        if (test.equals("1")) {
            BooleanQuery bqSourceKeyword = new BooleanQuery();
            for (String k : keywords) {
                Term term = new Term("sourceKeyword", k);
                TermQuery pq = new TermQuery(term);
                pq.setBoost(20.0f);
                bqSourceKeyword.add(pq, BooleanClause.Occur.MUST);
            }
            bq.add(bqSourceKeyword, BooleanClause.Occur.SHOULD);
        }
        IndexSearcher searcher = searcherManager.getSearcher("dd_product");
        System.out.println(bq);
        TopDocs topDocs = searcher.search(bq, 1);
        Document doc = DocumentHelper.parseText("<root/>");
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        List<Long> ids = new ArrayList<Long>();
        Element docs = doc.getRootElement().addElement("docs");
        doc.getRootElement().addElement("total").addText(String.valueOf(topDocs.totalHits));
        for (int i = 0; i < scoreDocs.length; i++) {
            org.apache.lucene.document.Document indexDoc = searcher.getIndexReader().document(scoreDocs[i].doc);
            ids.add(Long.valueOf(indexDoc.get("pId")));
            docToXML(docs, indexDoc);
        }
        return doc.asXML();
    }

    private String searchByNewArrivalsFormat(HttpServletRequest request, String format) {
        Map<String, Object> result = new HashMap<String, Object>();

        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"), "");
        String fromTime = StringUtils.defaultIfBlank(request.getParameter("fromTime"), "");
        String toTime = StringUtils.defaultIfBlank(request.getParameter("toTime"), "");
        String indexCode = StringUtils.defaultIfBlank(request.getParameter("indexCode"), "");

        Integer currentPage = null;
        try {
            currentPage = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("currentPage"), "1"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse currentPage failed,value:%s", currentPage));
        }
        Integer size = null;
        try {
            size = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("size"), "20"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse size failed,value:%s", size));
        }


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }

        Query condition;
        QueryParser parser = new QueryParser(Version.LUCENE_43, "pName", new MyAnalyzer());
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
            //skuPriority(bq);
            logger.info("{}", bq);
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
                    docToXML(docs, indexDoc);
                }
            }
            result.put("productIds", ids);
            if ("xml".equals(format)) {
                return doc.asXML();
            }
            if ("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return empty();
    }

    @RequestMapping(value = "/product/test/xml", method = RequestMethod.POST, produces = "text/plain;charset=utf-8")
    @ResponseBody
    private String testProduct(HttpServletRequest request, String format) {
        format = "xml";
        Map<String, Object> result = new HashMap<String, Object>();
        //String shop = StringUtils.defaultIfBlank(request.getParameter("shop"), "");
        String search = StringUtils.defaultIfBlank(request.getParameter("search"), "");
        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"), "");
        /*if ("".equals(shop)) {
            return empty();
        }
        shop = "\"" + shop + "\"";*/
        Integer currentPage = null;
        try {
            currentPage = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("currentPage"), "1"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse currentPage failed,value:%s", currentPage));
        }
        Integer size = null;
        try {
            size = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("size"), "20"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse size failed,value:%s", size));
        }


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }
        QueryParser parser = new QueryParser(Version.LUCENE_43, "sku", new KeywordAnalyzer());
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

            logger.info("-----------------{}", topDocs1.totalHits);
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
            logger.error("{}", e);
        }
        return empty();
    }


    private String search(HttpServletRequest request, String format) {
        Matcher m = p.matcher(StringUtils.defaultIfBlank(request.getParameter("keyword"), "").toLowerCase());
        String keyword = m.replaceAll("").trim().replaceAll("~!@#$%^&*()_+","").replaceAll("\\s+", " ");
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
            return getErrorJson(String.format("parse currentPage failed,value:%s", currentPage));
        }
        Integer size = null;
        try {
            size = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("size"), "20"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse size failed,value:%s", size));
        }


        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"), "");

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

            QueryParser parser = new QueryParser(Version.LUCENE_43, "pName", new MyAnalyzer());
            String langName = null;
            String langSegmentWord = "segmentWordEn";
            if ("ru".equals(country)) {
                langName = "pNameRU";
                langSegmentWord = "segmentWordRu";
            }
            if ("br".equals(country)) {
                langName = "pNameBR";
                langSegmentWord = "segmentWordBr";
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
            //查品类
            BooleanQuery bqTypes = new BooleanQuery();
            for (String k : keywords) {
                Term term = new Term("pTypeName", k);
                TermQuery pq = new TermQuery(term);
                pq.setBoost(30.0f);
                bqTypes.add(pq, BooleanClause.Occur.MUST);
            }
            bq.add(bqTypes, BooleanClause.Occur.SHOULD);
            //查分词
            BooleanQuery bqSegmentWord = new BooleanQuery();
            for (String k : keywords) {
                Term term = new Term(langSegmentWord, k);
                PrefixQuery pq = new PrefixQuery(term);
                pq.setBoost(10.0f);
                bqSegmentWord.add(pq, BooleanClause.Occur.MUST);
            }
            bq.add(bqSegmentWord, BooleanClause.Occur.SHOULD);

            //查原始关键字
            BooleanQuery bqSourceKeyword = new BooleanQuery();
            for (String k : keywords) {
                Term term = new Term("sourceKeyword", k);
                TermQuery pq = new TermQuery(term);
                pq.setBoost(20.0f);
                bqSourceKeyword.add(pq, BooleanClause.Occur.MUST);
            }
            bq.add(bqSourceKeyword, BooleanClause.Occur.SHOULD);

            Analyzer analyzer = new EnglishAnalyzer(Version.LUCENE_43);
            QueryParser qp = new QueryParser(Version.LUCENE_43, "pName", analyzer);
            Query qKeyword = qp.parse(keyword.replace(" ", " AND "));
            Query condition;
            /*for (String k : keywords) {
                Term term = new Term("pName", k);
                PrefixQuery pq = new PrefixQuery(term);
                bqKeyEn.add(pq, BooleanClause.Occur.MUST);
            }*/
            bqLang.add(qKeyword, BooleanClause.Occur.SHOULD);
            bq.add(bqLang, BooleanClause.Occur.SHOULD);
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
            BooleanQuery bqSourceKey = new BooleanQuery();
            for (String k : keywords) {
                Term sourceKeywordTerm = new Term("sourceKeyword", k);
                TermQuery sourceKeywordTermPq = new TermQuery(sourceKeywordTerm);
                bqSourceKey.add(sourceKeywordTermPq, BooleanClause.Occur.MUST);
            }
            //logger.info("search sourceKeywordCN :{}",bqSourceKey);
            TopDocs sourceKeywordTopDocs = searcher.search(bqSourceKey, 1);
            ScoreDoc[] sourceKeywordScoreDocs = sourceKeywordTopDocs.scoreDocs;
            String sourceKeywordCN = null;
            if (sourceKeywordScoreDocs.length > 0) {
                org.apache.lucene.document.Document indexDoc = searcher.getIndexReader().document(sourceKeywordScoreDocs[0].doc);
                sourceKeywordCN = indexDoc.get("sourceKeywordCN");
            }

            if (sourceKeywordCN != null) {
                BooleanQuery bqSourceKeywordCN = new BooleanQuery();
                String[] strs = sourceKeywordCN.split(" ");
                for (String k : strs) {
                    Term term = new Term("pNameCN", k);
                    TermQuery pq = new TermQuery(term);
                    bqSourceKeywordCN.add(pq, BooleanClause.Occur.MUST);
                }
                bq.add(bqSourceKeywordCN, BooleanClause.Occur.SHOULD);
            }


            if (searcher == null) {
                return empty();
            }
            int start = (currentPage - 1) * size + 1;
            //分页
            TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            //skuPriority(bq);
            logger.info("{}", bq);
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
                    docToXML(docs, indexDoc);
                }
            }
            result.put("productIds", ids);
            if ("xml".equals(format)) {
                return doc.asXML();
            }
            if ("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}", e);
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
            SortField sf = new SortField("skuOrder", SortField.Type.INT, false);
            sot = new Sort(sortField,sf);
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
                    NumericRangeQuery nrq = NumericRangeQuery.newDoubleRange("unitPrice", Double.valueOf(parts[1]), Double.valueOf(parts[2]), true, true);
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
        String indexCode = StringUtils.defaultIfBlank(request.getParameter("indexcode"), "");
        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"), "");
        if ("".equals(shop)) {
            return empty();
        }
        shop = "\"" + shop + "\"";
        Integer currentPage = null;
        try {
            currentPage = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("currentPage"), "1"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse currentPage failed,value:%s", currentPage));
        }
        Integer size = null;
        try {
            size = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("size"), "20"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse size failed,value:%s", size));
        }


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }
        QueryParser parser = new QueryParser(Version.LUCENE_43, "businessName", new KeywordAnalyzer());
        IndexSearcher searcher = searcherManager.getSearcher("dd_product");
        if (searcher == null) {
            return empty();
        }
        int start = (currentPage - 1) * size + 1;
        //排序
        Sort sot = sorts(sort);
        BooleanQuery bq = new BooleanQuery();
        if (!"".equals(indexCode)) {
            try {
                bq.add(parser.parse("indexCode:" + indexCode + "*"), BooleanClause.Occur.MUST);
            } catch (Exception e) {
                return getErrorJson("parse indexCode query failed,value:%s", indexCode);
            }
        }
        try {
            bq.add(parser.parse(shop), BooleanClause.Occur.MUST);
        } catch (Exception e) {
            return getErrorJson("parse shop name query failed,value:%s", indexCode);
        }
        //分页
        try {
            TopFieldCollector tsdc = TopFieldCollector.create(sot, start + size, false, false, false, false);
            logger.info("{}", bq);
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
                    docToXML(docs, indexDoc);
                }
            }
            result.put("productIds", ids);
            if ("xml".equals(format)) {
                return doc.asXML();
            }
            if ("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return empty();
    }

    private String searchByCategoryFormat(HttpServletRequest request, String format) {
        Map<String, Object> result = new HashMap<String, Object>();
        String indexCode = StringUtils.defaultIfBlank(request.getParameter("indexCode"), "");
        String sort = StringUtils.defaultIfBlank(request.getParameter("sort"), "");
        if ("".equals(indexCode)) {
            return empty();
        }

        Integer currentPage = null;
        try {
            currentPage = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("currentPage"), "1"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse currentPage failed,value:%s", currentPage));
        }
        Integer size = null;
        try {
            size = Integer.valueOf(StringUtils.defaultIfBlank(request.getParameter("size"), "20"));
        } catch (Exception e) {
            return getErrorJson(String.format("parse size failed,value:%s", size));
        }


        if (currentPage < 1) {
            currentPage = 1;
        }

        if (size < 1 || size > 100) {
            size = 1;
        }
        QueryParser parser = new QueryParser(Version.LUCENE_43, "indexCode", new MyAnalyzer());
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
            BooleanQuery bq = new BooleanQuery();
            bq.add(q, BooleanClause.Occur.MUST);
            //skuPriority(bq);
            logger.info("{}", bq);
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
                    docToXML(docs, indexDoc);
                }
            }
            result.put("productIds", ids);
            if ("xml".equals(format)) {
                return doc.asXML();
            }
            if ("json".equals(format)) {
                return objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            logger.error("{}", e);
        }
        return empty();
    }

    private String empty() {
        return "{\"totalHits\":0,\"productIds\":[]}";
    }


    private void docToXML(Element root, org.apache.lucene.document.Document document) {
        Element docEle = root.addElement("doc");
        List<IndexableField> fields = document.getFields();
        for (IndexableField field : fields) {
            docEle.addElement(field.name()).addCDATA(field.stringValue());
        }
    }

    private String getErrorJson(String error, Object... parmas) {
        error = String.format(error, parmas);
        logger.error(error);
        Map<String, String> map = new HashMap<String, String>();
        map.put("error", error);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (IOException e) {
            logger.error("{}", e);
        }
        return "";
    }

    /*private void skuPriority(BooleanQuery bq) {
        Term t = new Term("sku", "A");
        PrefixQuery pq = new PrefixQuery(t);
        pq.setBoost(300f);
        bq.add(pq, BooleanClause.Occur.SHOULD);

        Term t1 = new Term("sku", "X");
        PrefixQuery pq1 = new PrefixQuery(t1);
        pq1.setBoost(200f);
        bq.add(pq1, BooleanClause.Occur.SHOULD);

        Term t2 = new Term("sku", "T");
        PrefixQuery pq2 = new PrefixQuery(t2);
        pq2.setBoost(100f);
        bq.add(pq2, BooleanClause.Occur.SHOULD);
    }*/


}
