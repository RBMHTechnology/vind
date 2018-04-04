package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.solr.backend.SolrSearchServer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;
import org.mockito.Mockito;

import static com.rbmhtechnology.vind.api.query.facet.Facets.interval;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 28.07.16.
 */
public class IntervalFacetsTest {

    @Test
    public void testQueryBuilder() {

        SolrClient client = Mockito.mock(SolrClient.class);
        SolrSearchServer server = new SolrSearchServer(client,false);

        FieldDescriptor<Integer> descriptor = Mockito.mock(FieldDescriptor.class);
        when(descriptor.getType()).thenReturn(Integer.class);
        when(descriptor.isFacet()).thenReturn(true);
        when(descriptor.getName()).thenReturn("fieldName");
        DocumentFactory factory = Mockito.mock(DocumentFactory.class);

        FulltextSearch search = Search.fulltext().facet(interval("quality", descriptor, Interval.numericInterval("low", 0, 2, true, false), Interval.numericInterval("high", 3, 4)));

        SolrQuery query = server.buildSolrQuery(search,factory);

        assertEquals("{!key=quality}dynamic_single_facet_int_fieldName", query.get("facet.interval"));

        //assertThat(Arrays.asList(query.getParams("f.dynamic_single_int_null.facet.interval.set")), contains("{!key=low}[0,2)", "{!key=high}[3,4]")); TODO fix hamcrest dependency
    }



}
