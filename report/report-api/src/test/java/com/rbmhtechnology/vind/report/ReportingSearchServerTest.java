package com.rbmhtechnology.vind.report;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import com.rbmhtechnology.vind.report.application.SimpleApplication;
import com.rbmhtechnology.vind.report.logger.Log;
import com.rbmhtechnology.vind.report.logger.ReportWriter;
import com.rbmhtechnology.vind.report.session.SimpleSession;
import com.rbmhtechnology.vind.test.SearchTestcase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;

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
        thrown.expectMessage("No ReportWriter in classpath");
        SearchServer server = new ReportingSearchServer(testSearchServer.getSearchServer());
    }

    @Test
    public void testSuggestionQueryReportingWithSessionAndLogger() throws IOException {
        TestReportWriter logger = new TestReportWriter();

        ReportingSearchServer server = new ReportingSearchServer(testSearchServer.getSearchServer(), new SimpleApplication("app"), new SimpleSession("123"), logger);

        DocumentFactory factory = new DocumentFactoryBuilder("asset").build();

        server.execute(Search.fulltext(),factory);

        server.setSession(new SimpleSession("456"));

        server.execute(Search.fulltext("Hello World"),factory);

        assertEquals(2, logger.logs.size());
        assertEquals("app", ((SimpleApplication) logger.logs.get(0).getValues().get("application")).getId());
        assertEquals("123", ((SimpleSession)logger.logs.get(0).getValues().get("session")).getSessionId());
        assertEquals("*", logger.logs.get(0).getValues().get("query"));
        assertEquals("456", ((SimpleSession)logger.logs.get(1).getValues().get("session")).getSessionId());
        assertEquals("Hello World", logger.logs.get(1).getValues().get("query"));
    }

    public class TestReportWriter extends ReportWriter {

        public ArrayList<Log> logs = new ArrayList<>();

        @Override
        public void log(Log log) {
            logs.add(log);
        }
    }

}
