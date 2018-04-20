package com.rbmhtechnology.vind.monitoring;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;
import com.rbmhtechnology.vind.monitoring.logger.entry.FullTextEntry;
import com.rbmhtechnology.vind.monitoring.logger.entry.MonitoringEntry;
import com.rbmhtechnology.vind.monitoring.model.NewsItem;
import com.rbmhtechnology.vind.monitoring.model.application.SimpleApplication;
import com.rbmhtechnology.vind.monitoring.model.session.SimpleSession;
import com.rbmhtechnology.vind.test.SearchTestcase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Alfonso Noriega
 * @since 13.07.16.
 */
public class MonitoringSearchServerTest extends SearchTestcase {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void testSuggestionQueryMonitoring() {
        thrown.expect(RuntimeException.class);
        final SearchServer server = new MonitoringSearchServer(testSearchServer.getSearchServer());
    }

    @Test
    public void testSuggestionQueryMonitoringWithSessionAndLogger() throws IOException {
        TestMonitoringWriter logger = new TestMonitoringWriter();

        MonitoringSearchServer server = new MonitoringSearchServer(testSearchServer.getSearchServer(), new SimpleApplication("app"), new SimpleSession("123"), logger);

        final SingleValueFieldDescriptor.TextFieldDescriptor<String> textField = new FieldDescriptorBuilder<String>()
                .setFacet(true)
                .buildTextField("textField");

        final DocumentFactory factory = new DocumentFactoryBuilder("asset").
                addField(textField)
                .build();

        server.execute(Search.fulltext(),factory);

        server.setSession(new SimpleSession("456"));

        server.execute(Search.fulltext("Hello World").filter(or(eq(textField,"testFilter"), not(prefix("textField","pref")))).facet(textField).sort(Sort.desc(textField)),factory);
        //logger.logs.get(1).toJson();
        assertEquals(2, logger.logs.size());
        assertEquals("app", ((SimpleApplication) logger.logs.get(0).getApplication()).getId());
        assertEquals("123", logger.logs.get(0).getSession().getSessionId());
        assertEquals("*", ((FullTextEntry)logger.logs.get(0)).getRequest().getQuery());
        assertEquals("456", logger.logs.get(1).getSession().getSessionId());
        assertEquals("*", ((FullTextEntry)logger.logs.get(0)).getRequest().getQuery());
    }

    @Test
    public void testQueryMonitoringWithSessionAndLogger() throws IOException {
        TestMonitoringWriter logger = new TestMonitoringWriter();

        MonitoringSearchServer server = new MonitoringSearchServer(testSearchServer.getSearchServer(), new SimpleApplication("app"), new SimpleSession("123"), logger);

        //index 2 news items
        NewsItem i1 = new NewsItem("1", "New Vind instance needed", ZonedDateTime.now().minusMonths(3), "article", "coding");
        NewsItem i2 = new NewsItem("2", "Vind instance available", ZonedDateTime.now(), "blog", "coding", "release");

        server.indexBean(i1);
        server.indexBean(i2);
        server.commit();

        //this search should retrieve news items that should match the search term best
        FulltextSearch search = Search.fulltext("vind release");

        search.facet("category","kind");

        BeanSearchResult<NewsItem> result = server.execute(search, NewsItem.class);

        assertEquals(3, logger.logs.size());

    }

    public class TestMonitoringWriter extends MonitoringWriter {

        public ArrayList<MonitoringEntry> logs = new ArrayList<>();

        @Override
        public void log(MonitoringEntry log) {
            logs.add(log);
        }
    }

}
