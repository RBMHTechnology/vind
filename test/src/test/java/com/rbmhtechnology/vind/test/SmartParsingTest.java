package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static com.rbmhtechnology.vind.test.Backend.Elastic;
import static com.rbmhtechnology.vind.test.Backend.Solr;

public class SmartParsingTest {

    @Rule
    public TestBackend backend = new TestBackend();

    private final static FieldDescriptor<String> titleField = new FieldDescriptorBuilder()
            .setFullText(true)
            .setFacet(true)
            .buildTextField("title");

    private final static SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> createdField = new FieldDescriptorBuilder()
            .buildDateField("created");

    private final static MultiValueFieldDescriptor<String> categoryField = new FieldDescriptorBuilder()
            .setFacet(true)
           .buildMultivaluedTextField("category");

    private final DocumentFactory factory = new DocumentFactoryBuilder("document")
            .addField(titleField, createdField, categoryField)
            .build();

    @Test
    @RunWithBackend({Solr,Elastic})
    public void testSmartParser() {
        SearchServer server = backend.getSearchServer();

        server.index(getDoc("1", "Title", ZonedDateTime.now(), "Cat 1"));
        server.index(getDoc("2", "Title two", ZonedDateTime.now(), "Cat 1" ,"Cat 2"));
        server.index(getDoc("3", "Title three", ZonedDateTime.now().minusYears(1), "Cat 3"));

        server.commit();

        Assert.assertEquals(3, server.execute(Search.fulltext("title").smartParsing(true), factory).getNumOfResults());
        Assert.assertEquals(1, server.execute(Search.fulltext("category:\"Cat 2\"").smartParsing(true), factory).getNumOfResults());
    }

    @Test
    @RunWithBackend({Solr,Elastic})
    public void testSmartParserFailingNot() {
        SearchServer server = backend.getSearchServer();

        server.index(getDoc("1", "CoverageDBProject", ZonedDateTime.now(), "Cat 1"));
        server.index(getDoc("2", "Event", ZonedDateTime.now(), "Media Planet Event" ,"Cat 2"));
        server.index(getDoc("3", "Title three", ZonedDateTime.now().minusYears(1), "Cat 3"));

        server.commit();

        final FulltextSearch search = Search.fulltext("title:(CoverageDBProject OR Event) AND NOT (category:\"Media Planet Event\")").smartParsing(true);
        final SearchResult result = server.execute(search, factory);
        Assert.assertEquals(1, result.getNumOfResults());
    }


    public Document getDoc(String id, String title, ZonedDateTime created, String ... cats) {
        Document document = factory.createDoc(id)
                .setValue(titleField, title)
                .setValue(createdField, created);
        Arrays.stream(cats).forEach(c -> document.addValue(categoryField, c));
        return document;
    }

}
