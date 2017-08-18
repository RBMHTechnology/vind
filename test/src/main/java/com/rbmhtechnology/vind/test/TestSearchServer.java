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
        SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, "com.rbmhtechnology.vind.solr.EmbeddedSolrServerProvider");

        //SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_PROVIDER, "com.rbmhtechnology.vind.solr.RemoteSolrServerProvider");
        //SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_HOST, "http://localhost:8983/solr/core");

        searchServer = SearchServer.getInstance();
    }

    @Override
    protected void after() {
        try {
            searchServer.close();
        } catch (SearchServerException e) {
            log.error("Error closing SearchServer: {}", e.getMessage(), e);
        }

        super.after();
    }

    public SearchServer getSearchServer() {
        return searchServer;
    }

}
