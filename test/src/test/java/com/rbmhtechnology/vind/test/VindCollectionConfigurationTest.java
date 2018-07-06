package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VindCollectionConfigurationTest {

    @Rule
    public TestSearchServer testSearchServer = new TestSearchServer();

    private DocumentFactory doc;
    private SingleValueFieldDescriptor<String> value;

    private SearchServer server;

    @Before
    public void before() {

        server = testSearchServer.getSearchServer();

        server.clearIndex();
        server.commit();

        value = new FieldDescriptorBuilder<String>()
                .setFullText(true)
                .setSuggest(true)
                .buildTextField("value");


        doc = new DocumentFactoryBuilder("doc").addField(value).build();

    }

    @Test
    @Ignore
    public void testApostrophes() {
        server.index(doc.createDoc("1").setValue(value, "Neymar Jr's five one"));
        server.index(doc.createDoc("2").setValue(value, "Neymar Jr’s five two"));

        server.commit();

        SearchResult search_without_alt = server.execute(Search.fulltext("Jr's"), doc);

        assertEquals(2, search_without_alt.getNumOfResults());

        SearchResult search_with_alt = server.execute(Search.fulltext("Jr’s"), doc);

        assertEquals(2, search_with_alt.getNumOfResults());

        SuggestionResult suggestion_without_alt = server.execute(Search.suggest("Jr's").fields(value), doc);

        assertEquals(2, suggestion_without_alt.get(value).getValues().size());

        SuggestionResult suggestion_with_alt = server.execute(Search.suggest("Jr’s").fields(value), doc);

        assertEquals(2, suggestion_with_alt.get(value).getValues().size());

        SuggestionResult full_suggestion_with_alt = server.execute(Search.suggest("Jr’s").fields(value), doc);

        assertEquals(2, full_suggestion_with_alt.get(value).getValues().size());
    }

}
