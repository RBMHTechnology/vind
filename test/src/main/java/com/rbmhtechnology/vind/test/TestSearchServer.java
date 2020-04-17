package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.api.SearchServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 */
public class TestSearchServer {

    private static final Logger LOGGER = LoggerFactory.getLogger( TestSearchServer.class );

    private SearchServer searchServer;

    protected TestSearchServer() {
    }

    protected void start() throws RuntimeException {
        LOGGER.info("Start testserver");
    }

    protected void close() throws RuntimeException {
        LOGGER.info("Close testserver");
    }

    public final SearchServer getSearchServer() {
        if(searchServer == null) {
            searchServer = ServerType.current().getSearchServer();
        }
        return searchServer;
    }

}
