package com.rbmhtechnology.vind.elasticsearch.backend.client;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public  class ElasticVindClientNoAuth extends ElasticVindClient{

    public ElasticVindClientNoAuth(String defaultIndex, int port, String scheme, String host) {
        this.defaultIndex = defaultIndex;
        this.port = port;
        this.host = host;
        this.scheme = scheme;

        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, scheme)
                )
        );

    }

    public static ElasticVindClient build(String defaultIndex, int port, String scheme, String host) {
        return new ElasticVindClientNoAuth(defaultIndex, port, scheme, host);
    }

}
