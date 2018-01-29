package com.rbmhtechnology.vind.solr.backend;

import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.solr.backend.SolrServerProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 21.06.16.
 */
public class RemoteSolrServerProvider implements SolrServerProvider {

    @Override
    public SolrClient getInstance() {

        Logger log = LoggerFactory.getLogger(SolrServerProvider.class);

        String host = SearchConfiguration.get(SearchConfiguration.SERVER_HOST);
        //Backwards compatibility
        String solrHost = SearchConfiguration.get(SearchConfiguration.SERVER_SOLR_HOST);
        if(host == null & solrHost != null) {
            host = solrHost;
        }

        if(host == null) {
            log.error("{} has to be set", SearchConfiguration.SERVER_HOST);
            throw new RuntimeException(SearchConfiguration.SERVER_HOST + " has to be set");
        }

        String collection = SearchConfiguration.get(SearchConfiguration.SERVER_COLLECTION);
        //Backwards compatibility
        String solrCollection = SearchConfiguration.get(SearchConfiguration.SERVER_SOLR_COLLECTION);
        if(collection == null & solrCollection != null) {
            collection = solrCollection;
        }

        final String connectionTimeout = SearchConfiguration.get(SearchConfiguration.SERVER_CONNECTION_TIMEOUT);
        final String soTimeout = SearchConfiguration.get(SearchConfiguration.SERVER_SO_TIMEOUT);

        if(SearchConfiguration.get(SearchConfiguration.SERVER_SOLR_CLOUD, false)) {
            log.info("Instantiating solr cloud client: {}", host);

            if(collection != null) {
                CloudSolrClient client = new CloudSolrClient(host);
                client.setDefaultCollection(collection);

                if(StringUtils.isNotEmpty(connectionTimeout)) {
                    client.setZkConnectTimeout(Integer.valueOf(connectionTimeout));
                }

                if(StringUtils.isNotEmpty(soTimeout)) {
                    client.setZkClientTimeout(Integer.valueOf(soTimeout));
                }

                return client;
            } else {
                log.error(SearchConfiguration.SERVER_COLLECTION + " has to be set");
                throw new RuntimeException(SearchConfiguration.SERVER_COLLECTION + " has to be set");
            }

        } else {

            if(collection != null) {
                host = String.join("/",host,collection);
            }
            log.info("Instantiating solr http client: {}", host);
            return new HttpSolrClient(host);

        }

    }
}
