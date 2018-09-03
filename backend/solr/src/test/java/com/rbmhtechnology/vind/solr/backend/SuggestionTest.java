package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.solr.backend.SolrSearchServer;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 01.08.16.
 */
public class SuggestionTest {

    @Test
    public void testSpellcheck() {
        SolrClient client = Mockito.mock(SolrClient.class);
        SolrSearchServer server = new SolrSearchServer(client,false);

        FieldDescriptor descriptor = Mockito.mock(FieldDescriptor.class);
        when(descriptor.getType()).thenReturn(String.class);
        when(descriptor.isSuggest()).thenReturn(true);
        DocumentFactory factory = Mockito.mock(DocumentFactory.class);
        when(factory.getField(any())).thenReturn(descriptor);

        ExecutableSuggestionSearch search = Search.suggest("abc").fields("field");

        SolrQuery query = server.buildSolrQuery(search,factory,null);

        assertEquals("abc", query.get("q"));
        assertEquals("dynamic_single_suggest_analyzed_null", query.get("suggestion.field"));
        assertEquals(SolrSearchServer.SUGGESTION_DF_FIELD, query.get("suggestion.df"));
        assertEquals("10", query.get("suggestion.limit"));

        search.setLimit(100);
        query = server.buildSolrQuery(search,factory, null);
        assertEquals("100", query.get("suggestion.limit"));

        ExecutableSuggestionSearch search2 = Search.suggest("abc").fields(descriptor).clearFilter();
        query = server.buildSolrQuery(search2,factory, null);
        assertEquals("10", query.get("suggestion.limit"));

    }

}
