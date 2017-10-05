package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.solr.backend.SolrSearchServer;
import com.rbmhtechnology.vind.solr.backend.SolrUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static com.rbmhtechnology.vind.api.query.filter.Filter.or;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 22.06.16.
 */
public class SolrSearchServerTest {

    @Mock
    private SolrClient solrClient;

    @Mock
    private SolrPingResponse solrPingResponse;

    @Mock
    private QueryResponse response;

    private SearchServer server;

    @Before
    public void init() throws IOException, SolrServerException {
        MockitoAnnotations.initMocks(this);
        when(solrClient.ping()).thenReturn(solrPingResponse);
        when(solrPingResponse.getStatus()).thenReturn(0);
        when(solrPingResponse.getQTime()).thenReturn(10);


        when(solrClient.query(any())).thenReturn(response);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(response.getResults()).thenReturn(new SolrDocumentList());

        //we use the protected constructor to avoid schema checking
        server = new SolrSearchServer(solrClient, false);
    }

    @Test
    public void testSearch() throws Exception {

        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("asset");
        FieldDescriptor descriptor = new FieldDescriptorBuilder().setFacet(true).buildTextField("text");
        docFactoryBuilder.addField(descriptor);
        DocumentFactory documents = docFactoryBuilder.build();

        server.execute(Search.fulltext("hello world").filter(eq("text", "123")).sort("id", Sort.Direction.Desc),documents);

        ArgumentCaptor<SolrQuery> argument = ArgumentCaptor.forClass(SolrQuery.class);

        verify(solrClient).query(argument.capture());

        SolrQuery query = argument.getValue();
        assertEquals("hello world", query.getQuery());

        assertEquals(2, query.getFilterQueries().length);
        assertThat(Arrays.asList(query.getFilterQueries()),containsInAnyOrder("_type_:asset","dynamic_single_facet_string_text:\"123\""));
        assertEquals("id Desc", query.getSortField());

    }


    @Test
    public void testIndex() throws Exception {

        FieldDescriptor<String> title = new FieldDescriptorBuilder().setFullText(true).buildTextField("title");
        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder().setFacet(true).buildDateField("created");
        MultiValueFieldDescriptor.NumericFieldDescriptor<Integer> category = new FieldDescriptorBuilder().setFacet(true).buildMultivaluedNumericField("category", Integer.class);

        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("asset");
        DocumentFactory documents = docFactoryBuilder.addField(title).addField(created).addField(category).build();

        Document d1 = documents.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                .setValues(category, Arrays.asList(1, 2));

        Document d2 = documents.createDoc("2")
                .setValue(title, "Hello Austria")
                .setValue(created, ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()))
                .setValue(category, 4);

        server.index(d1);
        ArgumentCaptor<SolrInputDocument> argument = ArgumentCaptor.forClass(SolrInputDocument.class);
        verify(solrClient).add(argument.capture());

        SolrInputDocument doc = argument.getValue();
        assertEquals("_id_=1", doc.get(SolrUtils.Fieldname.ID).toString());
        assertEquals("_type_=asset", doc.get(SolrUtils.Fieldname.TYPE).toString());
        assertEquals("dynamic_multi_int_category=[1, 2]", doc.get("dynamic_multi_int_category").toString());
        assertEquals("dynamic_single_string_title=Hello World", doc.get("dynamic_single_string_title").toString());
        assertEquals("dynamic_single_date_created=Fri Jan 01 00:00:00 CET 2016", doc.get("dynamic_single_date_created").toString());

        server.commit();

        SearchResult result = server.execute(Search
                .fulltext("hello")
                .filter(or(category.between(3, 5), created.before(ZonedDateTime.now())))
                , documents);
    }
}