package com.rbmhtechnology.vind.elasticsearch.backend.client;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ElasticVindClientApiKeyAuth extends ElasticVindClient {

    private final String key;
    private final String id;

    public ElasticVindClientApiKeyAuth(String defaultIndex, int port, String scheme, String host, Long connectionTimeout, Long socketTimeout, String id, String key) {
        this.defaultIndex = defaultIndex;
        this.port = port;
        this.host = host;
        this.scheme = scheme;

        this.id = id;
        this.key = key;

        final String apiKeyAuth =
                Base64.getEncoder().encodeToString((this.id + ":" + this.key).getBytes(StandardCharsets.UTF_8));

        this.client = new RestHighLevelClient(RestClient.builder(new HttpHost(host, port, scheme))
                .setDefaultHeaders(new Header[]{new BasicHeader("Authorization", "ApiKey " + apiKeyAuth)})
                .setRequestConfigCallback(applyTimeouts(connectionTimeout, socketTimeout)));
    }

    public static ElasticVindClient build(String defaultIndex, int port, String scheme, String host, Long connectionTimeout, Long socketTimeout, String id, String key) {
        return new ElasticVindClientApiKeyAuth(defaultIndex, port, scheme, host, connectionTimeout, socketTimeout, id, key);
    }

    public String getKey() {
        return key;
    }

    public String getId() {
        return id;
    }
}
