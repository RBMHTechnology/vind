package com.rbmhtechnology.vind.demo.step3.service;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.monitoring.log.writer.LogWriter;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.monitoring.MonitoringSearchServer;
import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;

import java.time.ZonedDateTime;
import java.util.Arrays;

import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class SearchService implements AutoCloseable{

    private SearchServer server = SearchServer.getInstance();
    private final MonitoringWriter writer = new LogWriter();
    private final MonitoringSearchServer monitoringSearchServer = new MonitoringSearchServer(server, writer);

    private SingleValueFieldDescriptor.TextFieldDescriptor<String> title;
    private SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> created;
    private MultiValueFieldDescriptor.TextFieldDescriptor<String> category;
    private SingleValueFieldDescriptor.NumericFieldDescriptor<Integer> ranking;

    private DocumentFactory newsItems;

    public SearchService() {

        //a simple fulltext field named 'title'
        this.title = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildTextField("title");

        //a single value date field
        this.created = new FieldDescriptorBuilder()
                .buildDateField("created");

        //a multivalue text field used for fulltext and facet.
        //we also added a boost
        this.category = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .setBoost(1.2f)
                .buildMultivaluedTextField("category");

        this.ranking = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildNumericField("ranking", Integer.class);

        //all fields are added to the document factory
        newsItems = new DocumentFactoryBuilder("newsItem")
                .addField(title)
                .addField(created)
                .addField(category)
                .addField(ranking)
                .build();
    }

    public void index() {
        server.index(createNewsItem("1", "New Vind instance needed", ZonedDateTime.now().minusMonths(3), 1, "coding"));
        server.index(createNewsItem("2", "Vind instance available", ZonedDateTime.now(), 2, "coding", "release"));
        server.commit();
    }

    public SearchResult search(String query, int raking) {
        FulltextSearch search = Search.fulltext(query);

        //ranking field is of type Integer, so only Integers are allowed here
        search.filter(eq(ranking,raking));

        //for the execution we now use the factory as parameter
        return monitoringSearchServer.execute(search, newsItems);
    }

    @Override
    public void close() {
        server.close();
    }

    private Document createNewsItem(String _id, String _title, ZonedDateTime _created, int _ranking, String ... _categories) {
        Document document = newsItems.createDoc(_id);
        document.setValue(title, _title);
        document.setValue(created, _created);
        document.setValues(category, Arrays.asList(_categories));
        document.setValue(ranking, _ranking);
        return document;
    }

}
