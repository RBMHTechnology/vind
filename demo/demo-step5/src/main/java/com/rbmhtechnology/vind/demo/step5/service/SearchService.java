package com.rbmhtechnology.vind.demo.step5.service;

import com.rbmhtechnology.vind.demo.step5.guardian.GuardianNewsItem;
import com.rbmhtechnology.vind.demo.step5.guardian.GuardianNewsIterator;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.monitoring.log.writer.LogWriter;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.monitoring.MonitoringSearchServer;
import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Arrays;

import static com.rbmhtechnology.vind.api.query.facet.Facets.range;
import static com.rbmhtechnology.vind.api.query.filter.Filter.eq;
import static com.rbmhtechnology.vind.api.query.sort.Sort.SpecialSort.scoredDate;
import static com.rbmhtechnology.vind.api.query.sort.Sort.desc;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class SearchService implements AutoCloseable {

    public enum Sort {
        date,
        score,
        scoredate;

        public static Sort pares(String s) {
            try {
                return Sort.valueOf(s);
            } catch (Exception e) {
                return scoredate;
            }
        }
    }

    private String guardianApiKey;
    private SearchServer server;
    private final MonitoringWriter writer = new LogWriter();
    private final MonitoringSearchServer monitoringSearchServer;


    private SingleValueFieldDescriptor.TextFieldDescriptor<String> title;
    private SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> publicationDate;
    private MultiValueFieldDescriptor.TextFieldDescriptor<String> category;
    private SingleValueFieldDescriptor.TextFieldDescriptor<String> kind;
    private SingleValueFieldDescriptor.TextFieldDescriptor<String> url;

    private DocumentFactory newsItems;

    public SearchService(final String guardianApiKey) {

        this.guardianApiKey = guardianApiKey;

        this.server = SearchServer.getInstance();

        this.monitoringSearchServer = new MonitoringSearchServer(server, writer);


        this.title = new FieldDescriptorBuilder()
                .setFullText(true)
                .buildTextField("title");

        this.publicationDate = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .buildDateField("created");

        this.category = new FieldDescriptorBuilder()
                .setFacet(true)
                .setFullText(true)
                .setBoost(1.2f)
                .buildMultivaluedTextField("category");

        this.kind = new FieldDescriptorBuilder()
                .setFacet(true)
                .buildTextField("kind");

        this.url = new FieldDescriptorBuilder()
                .buildTextField("url");

        newsItems = new DocumentFactoryBuilder("newsItem")
                .addField(title)
                .addField(publicationDate)
                .addField(category)
                .addField(kind)
                .addField(url)
                .build();
    }

    public boolean index() {

        GuardianNewsIterator iterator = new GuardianNewsIterator(this.guardianApiKey);

        while (iterator.hasNext()) {
            for (GuardianNewsItem item : iterator.getNext()) {
                Document document = newsItems.createDoc(item.getId());
                document.setValue(title, item.getWebTitle());
                document.setValue(publicationDate, item.getWebPublicationDate());
                document.setValue(category, item.getSectionName());
                document.setValue(kind, item.getType());
                document.setValue(url, item.getWebUrl());

                server.index(document);
            }
            server.commit();
        }

        return true;
    }

    public Object search(String query, String... categories) {


        FulltextSearch search = Search.fulltext(query);

        if (categories != null) Arrays.stream(categories).map(c -> eq(category, c)).forEach(search::filter);

        return monitoringSearchServer.execute(search, newsItems);
    }

    public Object suggest(String query, String... categories) {

        ExecutableSuggestionSearch search = Search.suggest(query).fields(kind, category);

        if (categories != null) Arrays.stream(categories).map(c -> eq(category, c)).forEach(search::filter);

        return monitoringSearchServer.execute(search, newsItems);
    }

    public Object search(String query, int page, Sort sort) {

        FulltextSearch search = Search.fulltext(query);

        switch (sort) {
            case date:
                search.sort(desc(publicationDate));
                break;
            case score:
                break;
            case scoredate:
                search.sort(desc(scoredDate(publicationDate)));
        }

        search.facet(category, kind);

        search.page(page);

        search.facet(range("published", publicationDate, ZonedDateTime.now().minus(Duration.ofDays(1)), ZonedDateTime.now(), Duration.ofHours(1)));

        search.filter(publicationDate.between(
                        ZonedDateTime.now().minusDays(7),
                        ZonedDateTime.now()
                )
        );

        return monitoringSearchServer.execute(search, newsItems);

    }

    @Override
    public void close() {
        server.close();
    }
}
