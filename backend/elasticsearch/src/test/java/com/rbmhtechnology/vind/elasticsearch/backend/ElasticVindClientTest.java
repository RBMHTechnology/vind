package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.elasticsearch.backend.util.FieldUtil;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.DisMaxQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ElasticVindClientTest  extends ElasticBaseTest{

    @Test
    public void testPing() throws IOException {
        assertTrue(client.ping());
    }

    @Test
    public void testAdd() throws IOException {

        final Map<String, Object> doc = new HashMap<>();
        doc.put("dynamic_string_title", "The last ascent of man");
        doc.put(FieldUtil.ID, "AA-2X3451");
        doc.put(FieldUtil.TYPE, "TestDoc");
        final BulkResponse indexResult = client.add(doc);
        assertNotNull(indexResult);
        assertEquals("CREATED", indexResult.getItems()[0].status().name());
    }

    @Test
    public void testAddMultipleDocuments() throws IOException {

        final Map<String, Object> doc1 = new HashMap<>();
        doc1.put("dynamic_string_title", "Dawn of humanity: the COVID-19 chronicles");
        doc1.put(FieldUtil.ID, "AA-2X6891");
        doc1.put(FieldUtil.TYPE, "TestDoc");

        final Map<String, Object> doc2 = new HashMap<>();
        doc2.put("dynamic_string_title", "The last ascend of man");
        doc2.put(FieldUtil.ID, "AA-2X3451");
        doc2.put(FieldUtil.TYPE, "TestDoc");
        final BulkResponse indexResult = client.add(Arrays.asList(doc1,doc2));
        assertNotNull(indexResult);
        assertFalse(indexResult.hasFailures());
    }

    @Test
    public void testAddBinaryField() throws IOException {

        final Map<String, Object> doc = new HashMap<>();
        doc.put("dynamic_binary_raw_data", Base64.getEncoder().encode("The last ascent of man".getBytes()));
        doc.put(FieldUtil.ID, "AA-2X3451");
        doc.put(FieldUtil.TYPE, "TestDoc");
        final BulkResponse indexResult = client.add(doc);
        assertNotNull(indexResult);
        assertEquals("OK", indexResult.getItems()[0].status().name());
    }
    @Test
    public void testPercolatorQuery() throws IOException {

        final BulkResponse indexResult = client.addPercolateQuery(getBasicTestSearch().query());
        assertNotNull(indexResult);
        assertEquals("CREATED", indexResult.getItems()[0].status().name());

        final Map<String, Object> matchingDoc = new HashMap<>();
        matchingDoc.put(FieldUtil.ID, "AA-2X3451");
        matchingDoc.put(FieldUtil.TYPE, "TestDoc");

        SearchResponse percolatorResponse = client.getPercolateQuery(matchingDoc);
        assertNotNull(percolatorResponse);
        assertEquals(1, percolatorResponse.getHits().getTotalHits().value);

        final Map<String, Object> notMatchingDoc = new HashMap<>();
        notMatchingDoc.put(FieldUtil.ID, "AA-2X3451");
        notMatchingDoc.put(FieldUtil.TYPE, "notMatching");
        percolatorResponse = client.getPercolateQuery(notMatchingDoc);
        assertNotNull(percolatorResponse);
        assertEquals(0, percolatorResponse.getHits().getTotalHits().value);

    }

    private SearchSourceBuilder getBasicTestSearch() {
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final BoolQueryBuilder baseQuery = QueryBuilders.boolQuery();

        //build full text disMax query
        final QueryStringQueryBuilder fullTextStringQuery = QueryBuilders.queryStringQuery("*")
                .minimumShouldMatch("1"); //mm

        final DisMaxQueryBuilder query = QueryBuilders.disMaxQuery()
                .add(fullTextStringQuery);

        baseQuery.must(query);
        baseQuery.filter( termQuery("_type_", "TestDoc"));
        searchSource.query(baseQuery);
        return searchSource;
    }
}
