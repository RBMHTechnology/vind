package com.rbmhtechnology.vind.report;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.model.FieldDescriptorBuilder;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.report.logger.Log;
import com.rbmhtechnology.vind.report.logger.ReportWriter;
import com.rbmhtechnology.vind.report.model.NewsItem;
import com.rbmhtechnology.vind.report.model.application.SimpleApplication;
import com.rbmhtechnology.vind.report.model.request.SearchRequest;
import com.rbmhtechnology.vind.report.model.session.SimpleSession;
import com.rbmhtechnology.vind.test.SearchTestcase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;
import static org.junit.Assert.assertEquals;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public class ReportingSearchServerTest extends SearchTestcase {

    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void testSuggestionQueryReporting() {
        thrown.expect(RuntimeException.class);
        final SearchServer server = new ReportingSearchServer(testSearchServer.getSearchServer());
    }

    @Test
    public void testSuggestionQueryReportingWithSessionAndLogger() throws IOException {
        TestReportWriter logger = new TestReportWriter();

        ReportingSearchServer server = new ReportingSearchServer(testSearchServer.getSearchServer(), new SimpleApplication("app"), new SimpleSession("123"), logger);

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
        assertEquals("app", ((SimpleApplication) logger.logs.get(0).getValues().get("application")).getId());
        assertEquals("123", ((SimpleSession)logger.logs.get(0).getValues().get("session")).getSessionId());
        assertEquals("*", ((SearchRequest)logger.logs.get(0).getValues().get("request")).getQuery());
        assertEquals("456", ((SimpleSession)logger.logs.get(1).getValues().get("session")).getSessionId());
        assertEquals("Hello World", ((SearchRequest)logger.logs.get(1).getValues().get("request")).getQuery());
    }

    @Test
    public void testQueryReportingWithSessionAndLogger() throws IOException {
        TestReportWriter logger = new TestReportWriter();

        ReportingSearchServer server = new ReportingSearchServer(testSearchServer.getSearchServer(), new SimpleApplication("app"), new SimpleSession("123"), logger);

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

        assertEquals(1, logger.logs.size());

    }

    public class TestReportWriter extends ReportWriter {

        public ArrayList<Log> logs = new ArrayList<>();

        @Override
        public void log(Log log) {
            logs.add(log);
        }
    }

}
