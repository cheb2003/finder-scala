package com.dd.finder.search.web.controller;

import com.dd.finder.search.service.SearcherManager;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexableField;
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

    @RequestMapping(value = "/product/xml/{country}/{indexCode}/{keyword}/{type}/{attributes}/{currentPage}/{size}/{sort}", method = RequestMethod.GET,produces = "text/xml;charset=utf-8")
    @ResponseBody
    public Object modifyPassword(@PathVariable String country,@PathVariable String indexCode, @PathVariable String keyword,@PathVariable String attributes,
                                 @PathVariable Integer currentPage,@PathVariable Integer size,
                                 @PathVariable String sort) {



        Document doc = null;
        try {
            keyword = keyword.trim();
            keyword = QueryParser.escape(keyword);
            String[] keywordSplit = keyword.split(" ");
            StringBuilder sbKeyword = new StringBuilder();
            for (String str : keywordSplit) {
                sbKeyword.append('+').append(str).append(' ');
            }
            QueryParser parser = new QueryParser(Version.LUCENE_40,
                    "productaliasname_nvarchar", new StandardAnalyzer(
                    Version.LUCENE_40));
            Query condition = null;
            try{
                condition = parser.parse("-ec_product001.productcountryinfoforcreator_nvarchar:344_0 " + sbKeyword.toString());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            BooleanQuery bq = new BooleanQuery();
            bq.add(condition, BooleanClause.Occur.MUST);
            //添加属性
            attributes = attributes.toLowerCase();
            if(!"default".equals(attributes)){
                String[] attrs = attributes.split(",");

                QueryParser parserExtends = new QueryParser(Version.LUCENE_40,
                        "attributes_nvarchar", new StandardAnalyzer(Version.LUCENE_40));
                for (String attr : attrs) {
                    attr = QueryParser.escape(attr);
                    Query qAttributes = null;
                    try {
                        qAttributes = parserExtends
                                .parse("\"" + attr + "\"");
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    bq.add(qAttributes, BooleanClause.Occur.MUST);
                }
            }

            //查询属性
            //排序
            SortField sortField = null;
            if ("price+".equals(sort)) {
                sortField = new SortField("discountprice_money", SortField.Type.DOUBLE, true);
            } else if ("price-".equals(sort)) {
                sortField = new SortField("discountprice_money", SortField.Type.DOUBLE, false);
            } else if ("date+".equals(sort)) {
                sortField = new SortField("creattime_datetime", SortField.Type.DOUBLE, true);
            } else if ("date-".equals(sort)) {
                sortField = new SortField("creattime_datetime", SortField.Type.DOUBLE, false);
            } else if ("pop+".equals(sort)) {
                sortField = new SortField("productscore_float", SortField.Type.DOUBLE, true);
            } else if ("pop-".equals(sort)) {
                sortField = new SortField("productscore_float", SortField.Type.DOUBLE, false);
            } else if ("pop+".equals(sort)) {
                sortField = new SortField("productscore_float", SortField.Type.DOUBLE, true);
            } else if ("review+".equals(sort)) {
                sortField = new SortField("usercommentcount_int", SortField.Type.INT, true);
            } else if ("review-".equals(sort)) {
                sortField = new SortField("usercommentcount_int", SortField.Type.INT, false);
            } else if ("diggs+".equals(sort)) {
                sortField = new SortField("diggs_int", SortField.Type.INT, true);
            } else if ("diggs-".equals(sort)) {
                sortField = new SortField("diggs_int", SortField.Type.INT, false);
            }
            Sort sot = null;
            if (sortField == null) {
                sortField = SortField.FIELD_SCORE;
                SortField www = new SortField("productscore_float", SortField.Type.DOUBLE,true);
                sot = new Sort(sortField,www);
            }


            IndexSearcher searcher = searcherManager.getSearcher("dd_product");
            IndexSearcher attributesSearcher = searcherManager.getSearcher("dd_attributes");
            doc = DocumentHelper.parseText("<response></response>");
            Element root = doc.getRootElement();
            int start = (currentPage - 1) * size + 1;
            //分页
            TopFieldCollector tsdc = TopFieldCollector.create(sot,start + size,false,false,false,false);

            searcher.search(condition,tsdc);
            root.addElement("totalHits").addText(String.valueOf(tsdc.getTotalHits()));
            //从0开始计算
            TopDocs topDocs = tsdc.topDocs(start - 1, size);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            Element docsEle = root.addElement("docs");
            for (int i = 0; i < scoreDocs.length; i++) {
                docToXML(docsEle,searcher.getIndexReader().document(scoreDocs[i].doc));
            }
            Element categorysEle = root.addElement("categorys");
            Element categoryEle = categorysEle.addElement("category");
            categoryEle.addAttribute("name", "category1");
            categoryEle.addAttribute("code", "0001");
            Element attributesEle = root.addElement("attributes");
            Element categoryAttributeEle = attributesEle.addElement("category");
            categoryAttributeEle.addAttribute("name", "color");
            Element attribute = categoryAttributeEle.addElement("attribute");
            attribute.addAttribute("name","red");
            //获取attributes搜索参数
            /*topDocs = tsdc.topDocs();
            scoreDocs = topDocs.scoreDocs;
            Set<String> field = new HashSet<String>();
            field.add("productid_int");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < scoreDocs.length; i++) {
                BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
                sb.append("+(");
                sb.append(searcher.getIndexReader().document(scoreDocs[i].doc,field)).append(' ');
                sb.append(")");
            }
            try {
                Query q = parser.parse(sb.toString());
                TopDocs attrTopDocs = attributesSearcher.search(q, Integer.MAX_VALUE);
                ScoreDoc[] attrScoreDocs = attrTopDocs.scoreDocs;
                Element attributesEle = root.addElement("attributes");
                Map<String, List<org.apache.lucene.document.Document>> attrMap = new HashMap<String, List<org.apache.lucene.document.Document>>();
                for (int i = 0; i < attrScoreDocs.length; i++) {

                }
               // attrToXML(attributesEle, attributesSearcher.getIndexReader().document(scoreDocs[i].doc));
            } catch (ParseException e) {
                e.printStackTrace();
            }*/
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc.asXML();
    }

    private void attrToXML(Element attributesEle, org.apache.lucene.document.Document document) {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void docToXML(Element root,org.apache.lucene.document.Document document) {
        Element docEle = root.addElement("doc");
        List<IndexableField> fields = document.getFields();
        for (IndexableField field : fields) {
            docEle.addElement(field.name()).addCDATA(field.stringValue());
        }
    }
}
