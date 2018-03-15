package com.rbmhtechnology.vind.monitoring.log.writer;

import com.rbmhtechnology.vind.monitoring.MonitoringSearchServer;
import com.rbmhtechnology.vind.test.SearchTestcase;
import org.junit.Test;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 18.07.16.
 */
public class LogWriterTest extends SearchTestcase {

    @Test
    public void testSuggestionQueryReporting() {
        MonitoringSearchServer server = new MonitoringSearchServer(testSearchServer.getSearchServer());
    }

}