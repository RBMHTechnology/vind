package com.rbmhtechnology.vind.elasticsearch.backend;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public  class ElasticVindClient {

    private static final Logger log = LoggerFactory.getLogger(ElasticVindClient.class);

    private String defaultIndex;
    private final RestHighLevelClient client;
    private final int port;
    private final String host;
    private final String scheme;
    private long connectionTimeOut;
    private long clientTimOut;
    private final String user;
    private final String key;

    private ElasticVindClient(String defaultIndex, int port, String scheme, String host, String user, String key) {
        this.defaultIndex = defaultIndex;
        this.port = port;
        this.host = host;
        this.scheme = scheme;
        this.user = user;
        this.key = key;

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(user, key));

        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, scheme)
                ).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
                        return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                })
        );
    }

    private ElasticVindClient(int port, String scheme, String host, String user, String key) {
        this.port = port;
        this.host = host;
        this.scheme = scheme;
        this.user = user;
        this.key = key;

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, key));
        final RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(host, port, scheme))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));

        this.client = new RestHighLevelClient(restClientBuilder);
    }

    public RestHighLevelClient getClient() {
        return client;
    }

    public String getDefaultIndex() {
        return defaultIndex;
    }

    public ElasticVindClient setDefaultIndex(String index) {
        this.defaultIndex = index;
        return this;
    }

    public long getConnectionTimeOut() {
        return connectionTimeOut;
    }

    public ElasticVindClient setConnectionTimeOut(long connectionTimeOut) {
        this.connectionTimeOut = connectionTimeOut;
        return this;
    }

    public long getClientTimOut() {
        return clientTimOut;
    }

    public ElasticVindClient setClientTimOut(long clientTimOut) {
        this.clientTimOut = clientTimOut;
        return this;
    }

    public boolean ping() {
        try {
            final RequestOptions authenticatedDefaultRequest = RequestOptions.DEFAULT;
            return this.client.ping(authenticatedDefaultRequest);
        } catch (IOException e) {
            log.error("Unable to ping Elasticsearch server {}://{}:{}", scheme, host, port,e);
            throw new RuntimeException(String.format("Unable to ping Elasticsearch server %s://%s:%s", scheme, host, port),e);
        }
    }

    public void close() {
        try {
            this.client.close();
        } catch (IOException e) {
            log.error("Unable to close Elasticsearch client connection to {}://{}:{}", scheme, host, port,e);
            throw new RuntimeException(String.format("Unable to ping Elasticsearch client connection to %s://%s:%s", scheme, host, port),e);
        }
    }

    public static class Builder {
        private String defaultIndex;
        private final int port;
        private final String scheme;
        private final String host;

        public Builder(String host) {
            final URI elasticUri = URI.create(host);
            this.port = elasticUri.getPort();
            this.host = elasticUri.getHost();
            this.scheme = elasticUri.getScheme();

        }

        public Builder setDefaultIndex(String index) {
            this.defaultIndex = index;
            return this;
        }

        public ElasticVindClient build(String user, String key) {
            return new ElasticVindClient(defaultIndex, port, scheme, host, user, key);
        }
    }
}
