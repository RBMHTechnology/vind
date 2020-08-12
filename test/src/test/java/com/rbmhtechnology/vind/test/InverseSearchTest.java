package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.filter.parser.FilterLuceneParser;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearch;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.api.result.InverseSearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.rbmhtechnology.vind.test.Backend.Elastic;
import static org.junit.Assert.assertEquals;


/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 15.06.16.
 */
public class InverseSearchTest {

    @Rule
    public TestBackend testBackend = new TestBackend();

    @Test
    @RunWithBackend(Elastic)
    public void testInverseSearch() {
        final SearchServer server = testBackend.getSearchServer();
        final SingleValueFieldDescriptor.TextFieldDescriptor title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("title");

        final SingleValueFieldDescriptor.TextFieldDescriptor volume = new FieldDescriptorBuilder()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("volume");

        final DocumentFactory testDocsFactory = new DocumentFactoryBuilder("TestDocument")
                .addField(title)
                .addInverseSearchMetaField(volume)
                .build();

        // Reverse Search
        Document d1 = testDocsFactory.createDoc("1")
                .setValue(title, "Hello World");

        final InverseSearchQuery inverseSearchQuery =
                testDocsFactory.createInverseSearchQuery("testQuery1", title.equals("Hello World"))
                    .setValue(volume,"volume1");
        final IndexResult indexResult =
                server.addInverseSearchQuery(inverseSearchQuery);

        InverseSearch inverseSearch = Search.inverseSearch(d1).setQueryFilter(volume.equals("volume1"));
        InverseSearchResult result = server.execute(inverseSearch, testDocsFactory);
        assertEquals(1, result.getNumOfResults());

        inverseSearch = Search.inverseSearch(d1).setQueryFilter(volume.equals("v1"));
        result = server.execute(inverseSearch, testDocsFactory);
        assertEquals(0, result.getNumOfResults());

    }

    @Test
    @RunWithBackend(Elastic)
    public void testInverseSearchIntegration() throws IOException {
        final SearchServer server = testBackend.getSearchServer();
        final MultiValueFieldDescriptor.TextFieldDescriptor<String> metadata = new FieldDescriptorBuilder<>()
                .setFullText(true)
                .setFacet(true)
                .buildMultivaluedTextField("customMetadata");
        final SingleValueFieldDescriptor.TextFieldDescriptor<String> tenant = new FieldDescriptorBuilder<>()
                .setFullText(true)
                .setFacet(true)
                .buildTextField("tenant");
        final DocumentFactory testDocsFactory = new DocumentFactoryBuilder("TestDocument")
                .addField(metadata)
                .addInverseSearchMetaField(tenant)
                .build();
        Document resource1 = testDocsFactory.createDoc("1")
                .setValues(metadata, "meta=data", "meta2=data2");
        Document resource2 = testDocsFactory.createDoc("2")
                .setValues(metadata, "meta2=data2", "meta3=data3");
        final FilterLuceneParser filterLuceneParser = new FilterLuceneParser();
        Filter ruleFilter = filterLuceneParser.parse("customMetadata:{\"meta=data\" OR \"meta2=data2\"}", testDocsFactory);
        final InverseSearchQuery rule1 =
                testDocsFactory.createInverseSearchQuery("rule1", ruleFilter).setValue(tenant,"t1");
        server.addInverseSearchQuery(rule1);
        Filter ruleFilter2 = filterLuceneParser.parse("customMetadata:{\"meta=data\" AND \"meta2=data2\"}", testDocsFactory);
        final InverseSearchQuery rule2 =
                testDocsFactory.createInverseSearchQuery("rule2", ruleFilter2).setValue(tenant,"t1");
        server.addInverseSearchQuery(rule2);
        InverseSearch inverseSearch = Search.inverseSearch(resource1).setQueryFilter(tenant.equals("t1"));
        InverseSearchResult result = server.execute(inverseSearch, testDocsFactory);
        assertEquals(2, result.getNumOfResults());
        inverseSearch = Search.inverseSearch(resource2).setQueryFilter(tenant.equals("t1"));
        result = server.execute(inverseSearch, testDocsFactory);
        assertEquals(1, result.getNumOfResults());
    }
}
