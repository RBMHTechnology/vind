package com.rbmhtechnology.vind.elasticsearch.backend.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.Objects;

public class ElasticVindClientBasicAuth extends ElasticVindClient{

    private final String user;
    private final String key;

    private ElasticVindClientBasicAuth(String defaultIndex, int port, String scheme, String host, Long connectionTimeout, Long socketTimeout, String user, String key) {
        this.defaultIndex = defaultIndex;
        this.port = port;
        this.host = host;
        this.scheme = scheme;
        this.user = user;
        this.key = key;

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if(Objects.nonNull(this.user) && Objects.nonNull(this.key)) {
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(user, key));
        }


        this.client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(host, port, scheme)
                ).setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
                .setRequestConfigCallback(applyTimeouts(connectionTimeout, socketTimeout))
        );
    }

    public static ElasticVindClientBasicAuth build(String defaultIndex, int port, String scheme, String host, Long connectionTimeout, Long socketTimeout, String user, String key) {
        return new ElasticVindClientBasicAuth(defaultIndex, port, scheme, host, connectionTimeout, socketTimeout, user, key);
    }
}