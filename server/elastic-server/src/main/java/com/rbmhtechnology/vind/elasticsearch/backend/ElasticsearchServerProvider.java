package com.rbmhtechnology.vind.elasticsearch.backend;

import com.rbmhtechnology.vind.SearchServerInstantiateException;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.elasticsearch.backend.client.ElasticVindClient;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ElasticsearchServerProvider implements ElasticServerProvider {
    @Override
    public ElasticVindClient getInstance() {

        final Logger log = LoggerFactory.getLogger(ElasticServerProvider.class);

        final String host = SearchConfiguration.get(SearchConfiguration.SERVER_HOST);

        if(host == null) {
            log.error("{} has to be set", SearchConfiguration.SERVER_HOST);
            throw new SearchServerInstantiateException(
                    SearchConfiguration.SERVER_HOST + " has to be set",
                    this.getClass());
        }

        final String collection = SearchConfiguration.get(SearchConfiguration.SERVER_COLLECTION);

        final String connectionTimeout = SearchConfiguration.get(SearchConfiguration.SERVER_CONNECTION_TIMEOUT);
        final String soTimeout = SearchConfiguration.get(SearchConfiguration.SERVER_SO_TIMEOUT);

        log.info("Instantiating Elasticsearch client: {}", host);

        if(collection != null) {
            ElasticVindClient client;
            final AuthTypes authType = AuthTypes.valueOf(SearchConfiguration.get(SearchConfiguration.SEARCH_AUTHENTICATION_METHOD, AuthTypes.NONE.name()));

            final ElasticVindClient.Builder clientBuilder = new ElasticVindClient.Builder(host)
                    .setDefaultIndex(collection);

            if(StringUtils.isNotEmpty(connectionTimeout)) {
                clientBuilder.setConnectionTimeout(Long.parseLong(connectionTimeout));
            }

            if(StringUtils.isNotEmpty(soTimeout)) {
                clientBuilder.setSocketTimeout(Long.parseLong(soTimeout));
            }

            switch (authType) {
                case APIKEY:
                    final String id = SearchConfiguration.get(SearchConfiguration.SEARCH_API_KEY_ID);
                    final String key = SearchConfiguration.get(SearchConfiguration.SEARCH_API_KEY_SECRET);
                    if(Objects.isNull(id) || Objects.isNull(key)) {
                        log.error("Missing API id or secret to authenticate with Elasticsearch backend");
                        throw new SearchServerInstantiateException(
                                "Missing API id or secret to authenticate with Elasticsearch backend",
                                this.getClass());
                    }
                    client = clientBuilder.buildWithApiKeyAuth(id, key);
                    break;
                case BASIC:
                    final String user = SearchConfiguration.get(SearchConfiguration.SEARCH_AUTHENTICATION_USER);
                    final String pssw = SearchConfiguration.get(SearchConfiguration.SEARCH_AUTHENTICATION_KEY);
                    if(Objects.isNull(user) || Objects.isNull(pssw)) {
                        log.error("Missing API user or password to authenticate with Elasticsearch backend");
                        throw new SearchServerInstantiateException(
                                "Missing API user or password to authenticate with Elasticsearch backend",
                                this.getClass());
                    }
                    client = clientBuilder.buildWithBasicAuth(user, pssw);
                    break;
                default:
                    client = clientBuilder.build();
                    break;
            }

            if(StringUtils.isNotEmpty(connectionTimeout)) {
                client.setConnectionTimeout(Long.parseLong(connectionTimeout));
            }

            if(StringUtils.isNotEmpty(soTimeout)) {
                client.setClientTimeout(Long.parseLong(soTimeout));
            }

            return client;
        } else {
            log.error(SearchConfiguration.SERVER_COLLECTION + " has to be set");
            throw new SearchServerInstantiateException(
                    SearchConfiguration.SERVER_COLLECTION + " has to be set",
                    this.getClass());
        }
    }
}
