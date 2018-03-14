package com.rbmhtechnology.vind.demo.step4.service;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.monitoring.log.writer.LogWriter;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.monitoring.MonitoringSearchServer;
import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static com.rbmhtechnology.vind.api.query.facet.Facets.range;
import static com.rbmhtechnology.vind.api.query.sort.Sort.SpecialSort.scoredDate;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class SearchService implements AutoCloseable {

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
                .setFacet(true)
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

    public SearchResult news(String query) {

        FulltextSearch search = Search.fulltext(query);

        //special sort filter allows to combine a date with scoring, so
        //that best fitting and latest documents are ranked to top
        search.sort(desc(scoredDate(created)));

        //complex facets (range in this case) and simple ones can be issued in one query
        search.facet(category);
        search.facet(range("dates", created, ZonedDateTime.now().minus(Duration.ofDays(100)), ZonedDateTime.now(), Duration.ofDays(10)));

        //specific field types allow special filters, in this case a between datetime filter
        search.filter(created.between(
                        ZonedDateTime.now().minusDays(7),
                        ZonedDateTime.now()
                )
        );

        //also numeric fields support special filters
        search.filter(ranking.greaterThan(1));

        //search.facet(stats("rankStats",ranking).mean().max());

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
