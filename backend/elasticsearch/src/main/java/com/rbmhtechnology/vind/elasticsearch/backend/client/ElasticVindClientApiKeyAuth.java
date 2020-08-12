package com.rbmhtechnology.vind.elasticsearch.backend.client;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public  class ElasticVindClientApiKeyAuth extends ElasticVindClient{

    private final String apiKeyAuth;

    public ElasticVindClientApiKeyAuth(String defaultIndex, int port, String scheme, String host, String apiKeyAuth) {
        this.defaultIndex = defaultIndex;
        this.port = port;
        this.host = host;
        this.scheme = scheme;

        this.apiKeyAuth = apiKeyAuth;

        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, scheme)
                ).setDefaultHeaders(  new Header[]{new BasicHeader("Authorization",
                        "ApiKey " + this.apiKeyAuth)})
        );

    }

    public static ElasticVindClient build(String defaultIndex, int port, String scheme, String host, String apiKeyAuth) {
        return new ElasticVindClientApiKeyAuth(defaultIndex, port, scheme, host, apiKeyAuth);
    }

    public String getApiKeyAuth() {
        return apiKeyAuth;
    }
}
