package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.SolrParams;
import org.hamcrest.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

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

    @Mock
    private UpdateResponse iResponse;

    private SearchServer server;

    @Before
    public void init() throws IOException, SolrServerException {
        MockitoAnnotations.initMocks(this);
        when(solrClient.ping()).thenReturn(solrPingResponse);
        when(solrPingResponse.getStatus()).thenReturn(0);
        when(solrPingResponse.getQTime()).thenReturn(10);


        when(solrClient.query(any(), any(SolrRequest.METHOD.class))).thenReturn(response);
        when(response.getResults()).thenReturn(new SolrDocumentList());
        when(response.getResults()).thenReturn(new SolrDocumentList());

        when(solrClient.add(org.mockito.Matchers.<Collection<SolrInputDocument>>any())).thenReturn(iResponse);
        when(solrClient.add(any(SolrInputDocument.class))).thenReturn(iResponse);
        when(iResponse.getQTime()).thenReturn(10);
        when(iResponse.getElapsedTime()).thenReturn(15l);

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

        verify(solrClient).query(argument.capture(), any(SolrRequest.METHOD.class));

        SolrQuery query = argument.getValue();
        assertEquals("hello world", query.getQuery());

        assertEquals(2, query.getFilterQueries().length);
        assertThat(Arrays.asList(query.getFilterQueries()),containsInAnyOrder("_type_:asset","dynamic_single_facet_string_text:\"123\""));
        assertEquals("id Desc", query.getSortField());

    }


    @Test
    public void testIndex() throws Exception {

        FieldDescriptor<String> title = new FieldDescriptorBuilder<>().setFullText(true).buildTextField("title");
        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created = new FieldDescriptorBuilder<>().setFacet(true).buildDateField("created");
        MultiValueFieldDescriptor.NumericFieldDescriptor<Integer> category = new FieldDescriptorBuilder<>().setFacet(true).buildMultivaluedNumericField("category", Integer.class);

        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder("asset");
        DocumentFactory documents = docFactoryBuilder.addField(title).addField(created).addField(category).build();

        final ZonedDateTime creationDate = ZonedDateTime.of(2016, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        Document d1 = documents.createDoc("1")
                .setValue(title, "Hello World")
                .setValue(created, creationDate)
                .setValues(category, Arrays.asList(1, 2));

        Document d2 = documents.createDoc("2")
                .setValue(title, "Hello Austria")
                .setValue(created, creationDate)
                .setValue(category, 4);

        server.index(d1);
        ArgumentCaptor<ArrayList<SolrInputDocument>> argument = ArgumentCaptor.forClass((Class)ArrayList.class);
        verify(solrClient).add(argument.capture());

        ArrayList<SolrInputDocument> docs = argument.getValue();
        SolrInputDocument doc = docs.get(0);
        assertThat(doc.get(SolrUtils.Fieldname.ID), solrInputField(SolrUtils.Fieldname.ID, "1"));
        assertThat(doc.get(SolrUtils.Fieldname.TYPE), solrInputField(SolrUtils.Fieldname.TYPE, "asset"));
        assertThat(doc.get("dynamic_multi_int_category"), solrInputField("dynamic_multi_int_category", Matchers.containsInAnyOrder(1,2)));
        assertThat(doc.get("dynamic_single_string_title"), solrInputField("dynamic_single_string_title", "Hello World"));
        assertThat(doc.get("dynamic_single_date_created"), solrInputField("dynamic_single_date_created", Date.from(creationDate.toInstant())));

        server.commit();

        SearchResult result = server.execute(Search
                .fulltext("hello")
                .filter(or(category.between(3, 5), created.before(ZonedDateTime.now())))
                , documents);
    }
    
    public static <T> Matcher<SolrInputField> solrInputField(String fieldName, T value) {
        return new TypeSafeMatcher<SolrInputField>() {
            @Override
            protected boolean matchesSafely(SolrInputField item) {
                // check the name
                if (!StringUtils.equals(fieldName, item.getName())) {
                    return false;
                }

                // check value for null
                final Object itemValue = item.getFirstValue();
                if (itemValue == null) {
                    return value == null;
                }

                // check the type
                if (!value.getClass().isAssignableFrom(itemValue.getClass())) {
                    return false;
                }

                // compare the values
                return value.equals(itemValue);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("SolrInputField(")
                        .appendValue(fieldName)
                        .appendText("=").appendValue(value)
                        .appendText(")");
            }
        };
    }

    public static <T> Matcher<SolrInputField> solrInputField(String fieldName, Matcher<T> valueMatcher) {
        return new TypeSafeMatcher<SolrInputField>() {
            @Override
            protected boolean matchesSafely(SolrInputField item) {
                return StringUtils.equals(fieldName, item.getName()) && valueMatcher.matches(item.getValue());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("SolrInputField(")
                        .appendValue(fieldName)
                        .appendText(" value matching ")
                        .appendDescriptionOf(valueMatcher)
                        .appendText(")");
            }
        };
    }

}