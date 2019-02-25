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

    private ServerType serverType = ServerType.RemoteStandalone;

    private SearchServer searchServer;

    @Override
    protected void before() throws Throwable {
        super.before();

        serverType.prepareConfig();

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

    private enum ServerType {

        Embedded("com.rbmhtechnology.vind.solr.backend.EmbeddedSolrServerProvider", null, null, false),
        RemoteStandalone("com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider", "http://localhost:8983/solr", "vind", false),
        RemoteCloud("com.rbmhtechnology.vind.solr.backend.RemoteSolrServerProvider", "localhost:9983", "vind", true);

        private String provider;
        private String host;
        private String collection;
        private boolean cloud;

        ServerType(String provider, String host, String collection, boolean cloud) {
            this.provider = provider;
            this.host = host;
            this.collection = collection;
            this.cloud = cloud;
        }

        void prepareConfig() {
            SearchConfiguration.set(SearchConfiguration.SERVER_PROVIDER, provider);
            if(host != null) SearchConfiguration.set(SearchConfiguration.SERVER_HOST, host);
            if(collection != null) SearchConfiguration.set(SearchConfiguration.SERVER_COLLECTION, collection);
            SearchConfiguration.set(SearchConfiguration.SERVER_SOLR_CLOUD, cloud);
            System.setProperty("runtimeLib", String.valueOf(cloud));
        }
    }

}
