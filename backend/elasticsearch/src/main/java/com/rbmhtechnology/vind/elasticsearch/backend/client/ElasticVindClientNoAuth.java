package com.rbmhtechnology.vind.elasticsearch.backend.client;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

public class ElasticVindClientNoAuth extends ElasticVindClient {

    public ElasticVindClientNoAuth(String defaultIndex, int port, String scheme, String host, Long connectionTimeout, Long socketTimeout) {
        this.defaultIndex = defaultIndex;
        this.port = port;
        this.host = host;
        this.scheme = scheme;

        this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme))
                .setRequestConfigCallback(applyTimeouts(connectionTimeout, socketTimeout)));
    }

    public static ElasticVindClient build(String defaultIndex, int port, String scheme, String host, Long connectionTimeout, Long socketTimeout) {
        return new ElasticVindClientNoAuth(defaultIndex, port, scheme, host, connectionTimeout, socketTimeout);
    }
}
