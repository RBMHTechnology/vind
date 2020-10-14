package com.rbmhtechnology.vind.demo.step5.service;

import com.rbmhtechnology.vind.api.MasterSlaveSearchServer;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.api.result.facet.FacetValue;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
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
import java.util.HashMap;

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
    //private final MonitoringSearchServer monitoringSearchServer;


    private SingleValueFieldDescriptor.TextFieldDescriptor<String> title;
    private SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> publicationDate;
    private MultiValueFieldDescriptor.TextFieldDescriptor<String> category;
    private SingleValueFieldDescriptor.TextFieldDescriptor<String> kind;
    private SingleValueFieldDescriptor.TextFieldDescriptor<String> url;

    private DocumentFactory newsItems;

    public SearchService(final String guardianApiKey) {

        this.guardianApiKey = guardianApiKey;

        this.server = getCombinedServer();

        //this.monitoringSearchServer = new MonitoringSearchServer(server, writer);

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
                .setSuggest(true)
                .buildMultivaluedTextField("category");

        this.kind = new FieldDescriptorBuilder()
                .setFacet(true)
                .setSuggest(true)
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

    private SearchServer getSolrServer() {
        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider");
        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:8983/solr");
        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, "vind_demo");
        SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, false);
        return SearchServer.getInstance();
    }

    private SearchServer getCombinedServer() {
        return new MasterSlaveSearchServer(getSolrServer(), getElasticServer());
    }

    private SearchServer getElasticServer() {
        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.elasticsearch.backend.ElasticServerProvider");
        SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:9200");
        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION_AUTOCREATE, true);
        SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, "vind_demo");
        return SearchServer.getInstance();
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

    public Object search(String query, boolean smart, String... categories) {


        FulltextSearch search = Search.fulltext(query).smartParsing(smart);

        if (categories != null) Arrays.stream(categories).map(c -> eq(category, c)).forEach(search::filter);

        return server.execute(search, newsItems);
    }

    public Object suggest(String query, String... categories) {

        ExecutableSuggestionSearch search = Search.suggest(query).fields(kind, category);

        if (categories != null) Arrays.stream(categories).map(c -> eq(category, c)).forEach(search::filter);

        return transformSuggestions(server.execute(search, newsItems));
    }

    private Object transformSuggestions(SuggestionResult result) {
        HashMap <String,Object> values = new HashMap<>();
        result.getSuggestedFields().forEach((FieldDescriptor f) -> {
            values.put(f.getName(), result.get(f).getValues());
        });
        return values;
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

        return server.execute(search, newsItems);

    }

    @Override
    public void close() {
        server.close();
    }
}
