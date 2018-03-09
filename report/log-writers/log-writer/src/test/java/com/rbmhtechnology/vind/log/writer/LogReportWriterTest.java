package com.rbmhtechnology.vind.log.writer;

import com.rbmhtechnology.vind.report.ReportingSearchServer;
import com.rbmhtechnology.vind.test.SearchTestcase;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 18.07.16.
 */
public class LogReportWriterTest extends SearchTestcase {

    @Test
    public void testSuggestionQueryReporting() throws IOException {
        ReportingSearchServer server = new ReportingSearchServer(testSearchServer.getSearchServer());
    }

}