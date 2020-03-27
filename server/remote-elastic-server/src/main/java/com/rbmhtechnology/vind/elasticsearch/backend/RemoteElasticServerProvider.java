package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.configure.SearchConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteElasticServerProvider implements ElasticServerProvider {
    @Override
    public ElasticVindClient getInstance() {

        final Logger log = LoggerFactory.getLogger(ElasticServerProvider.class);

        final String host = SearchConfiguration.get(SearchConfiguration.SERVER_HOST);

        if(host == null) {
            log.error("{} has to be set", SearchConfiguration.SERVER_HOST);
            throw new RuntimeException(SearchConfiguration.SERVER_HOST + " has to be set");
        }

        final String collection = SearchConfiguration.get(SearchConfiguration.SERVER_COLLECTION);

        final String connectionTimeout = SearchConfiguration.get(SearchConfiguration.SERVER_CONNECTION_TIMEOUT);
        final String soTimeout = SearchConfiguration.get(SearchConfiguration.SERVER_SO_TIMEOUT);

        log.info("Instantiating Elasticsearch client: {}", host);

        if(collection != null) {
            ElasticVindClient client =
                    new ElasticVindClient.Builder(host)
                            .setDefaultIndex(collection)
                            .build(SearchConfiguration.SEARCH_AUTHENTICATION_USER, SearchConfiguration.SEARCH_AUTHENTICATION_KEY);

            if(StringUtils.isNotEmpty(connectionTimeout)) {
                client.setConnectionTimeOut(Long.parseLong(connectionTimeout));
            }

            if(StringUtils.isNotEmpty(soTimeout)) {
                client.setClientTimOut(Long.parseLong(soTimeout));
            }

            return client;
        } else {
            log.error(SearchConfiguration.SERVER_COLLECTION + " has to be set");
            throw new RuntimeException(SearchConfiguration.SERVER_COLLECTION + " has to be set");
        }
    }
}
