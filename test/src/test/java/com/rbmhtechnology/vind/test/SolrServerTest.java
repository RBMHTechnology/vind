package com.rbmhtechnology.vind.test;

/*
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.utils.mam.FacetMapper;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
*/
public class SolrServerTest {
/*
    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

    @Test
    public void testRuntimeLib() throws SolrServerException, IOException {
        SearchServer server = testSearchServer.getSearchServer();

        SolrClient client = (SolrClient) server.getBackend();

        SolrInputDocument document = new SolrInputDocument();
        document.setField("_id_", "1");
        document.setField("_type_", "doc");
        document.setField("dynamic_multi_facet_string_f1", "test");
        document.setField("dynamic_multi_facet_string_f2", "hello");

        client.add(document);
        client.commit();

        SolrQuery query = new SolrQuery("t");
        query.setRequestHandler("/suggester");
        query.set("suggestion.df", "facets");
        query.set("suggestion.field", "dynamic_multi_facet_string_f1");

        QueryResponse response = client.query(query);

        assertEquals(1, ((NamedList) response.getResponse().get("suggestions")).get("suggestion_count"));
    }

    //MBDN-442
    @Test public void string2FacetTest() {

        SingleValueFieldDescriptor<Float> numberField = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("number", Float.class);

        FieldDescriptor<String> entityID = new FieldDescriptorBuilder()
                .buildTextField("entityID");

        SingleValueFieldDescriptor<Date> dateField = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildUtilDateField("date");


        DocumentFactory assets = new DocumentFactoryBuilder("asset")
                .addField(numberField)
                .addField(dateField)
                .addField(entityID)
                .build();

        Document d1 = assets.createDoc("1")
                .setValue(numberField, 24f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());

        Document d2 = assets.createDoc("2")
                .setValue(numberField, 2f)
                .setValue(entityID, "123")
                .setValue(dateField, new Date());

        SearchServer server = testSearchServer.getSearchServer();

        server.index(d1);
        server.index(d2);
        server.commit();

        HashMap<String, String> numberIntervals = new HashMap<>();

        numberIntervals.put("low","[* TO 20]");
        numberIntervals.put("high", "[20 TO *]");

        HashMap<String, String> dateIntervals = new HashMap<>();
        dateIntervals.put("after", "[NOW+23DAYS/DAY TO *]");
        dateIntervals.put("before", "[* TO NOW+23DAYS/DAY]");

        FulltextSearch searchAll = Search.fulltext().facet(FacetMapper.stringQuery2FacetMapper(numberField, "numberFacets", numberIntervals))
                .facet(FacetMapper.stringQuery2FacetMapper(dateField, "dateFacets", dateIntervals));

        final SearchResult searchResult = server.execute(searchAll, assets);
        assertEquals("No of interval number facets", 2, searchResult.getFacetResults().getIntervalFacet("numberFacets").getValues().size());
        assertEquals("No of interval date facets", 2, searchResult.getFacetResults().getIntervalFacet("dateFacets").getValues().size());
    }
 */
}
