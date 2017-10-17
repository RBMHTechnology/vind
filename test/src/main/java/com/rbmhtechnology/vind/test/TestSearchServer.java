package com.rbmhtechnology.vind.test;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class TestSearchServer extends ExternalResource {

    private Logger log = LoggerFactory.getLogger(getClass());

    private SearchServer searchServer;

    @Override
    protected void before() throws Throwable {
        super.before();
        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.EmbeddedSolrServerProvider");

        //SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider");
        //SearchConfiguration.set(SearchConfiguration.SERVER_HOST, "http://localhost:8983/solr/core");

        System.setProperty("runtimeLib", "false");
        searchServer = SearchServer.getInstance();
    }

    @Override
    protected void after() {
        try {
            searchServer.close();
        } catch (SearchServerException e) {
            log.error("Error closing SearchServer: {}", e.getMessage(), e);
        } finally {
            System.getProperties().remove("runtimeLib");
            super.after();
        }

    }

    public SearchServer getSearchServer() {
        return searchServer;
    }

}
