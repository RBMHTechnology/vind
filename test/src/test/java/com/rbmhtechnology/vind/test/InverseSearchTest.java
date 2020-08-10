package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearch;
import com.rbmhtechnology.vind.api.result.IndexResult;
import com.rbmhtechnology.vind.api.result.InverseSearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Rule;
import org.junit.Test;

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
        server.index(d1);

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
}
