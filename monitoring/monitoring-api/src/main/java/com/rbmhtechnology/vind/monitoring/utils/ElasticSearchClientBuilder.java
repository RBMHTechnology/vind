/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.utils;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created on 01.03.18.
 */
public class ElasticSearchClientBuilder {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchClientBuilder.class);

    private static JestClient jestClient;

    public static JestClient build(String host, String port) {
        if(jestClient == null) {
            final String conn = String.format("http://%s:%s", host, port);
            log.info("Creating ElasticSearch REST Client over {}..,", conn);
            final JestClientFactory factory = new JestClientFactory();
            factory.setHttpClientConfig(new HttpClientConfig.Builder(conn)
                    .multiThreaded(true)
                    .discoveryEnabled(true)
                    .discoveryFrequency(100l, TimeUnit.MILLISECONDS)
                    .maxConnectionIdleTime(1500L, TimeUnit.MILLISECONDS)
                    .maxTotalConnection(75)
                    .defaultMaxTotalConnectionPerRoute(75)
                    .build());
            jestClient = factory.getObject();
        }
        return jestClient;
    }

    public static JestClient build(String host, int port) {
        return build(host, Integer.toString(port));
    }
}
