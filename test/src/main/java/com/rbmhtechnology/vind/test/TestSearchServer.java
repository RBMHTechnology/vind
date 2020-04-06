package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.SearchServer;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TestSearchServer extends ExternalResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    private SearchServer searchServer;

    protected TestSearchServer() {
    }

    public static TestSearchServer create() {
        switch (ServerType.current()) {
            case Elastic:
                return new ElasticTestSearchServer();
            default:
                return new TestSearchServer();
        }
    }

    @Override
    protected void before() throws Throwable {
        super.before();
    }

    @Override
    protected void after() {
        try {
            if(searchServer != null) {
                searchServer.close();
            }
        } catch (SearchServerException e) {
            log.error("Error closing SearchServer: {}", e.getMessage(), e);
        } finally {
            System.getProperties().remove("runtimeLib");
            super.after();
        }

    }

    public final SearchServer getSearchServer() {
        if(searchServer == null) {
            searchServer = ServerType.current().getSearchServer();
        }
        return searchServer;
    }

}
