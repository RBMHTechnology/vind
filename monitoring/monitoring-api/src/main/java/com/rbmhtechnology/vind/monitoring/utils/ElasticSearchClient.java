/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.utils;

import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created on 28.02.18.
 */
public class ElasticSearchClient {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchClient.class);

    private String elasticPort;
    private String elasticHost;
    private String elasticIndex;
    transient JestClient elasticClient;

    public boolean init(String elasticHost, String elasticPort, String elasticIndex) {
        this.elasticHost = elasticHost;
        this.elasticPort = elasticPort;
        this.elasticIndex = elasticIndex;

        try {
            final JestClient client = getElasticSearchClient();
            client.execute(new CreateIndex.Builder(elasticIndex).build());
            log.info("Established elasticsearch connection to host '{}:{}', index '{}'.", elasticHost, elasticPort, elasticIndex);
            elasticClient = client;
        } catch (IOException e) {
            log.error("Error creating base index on ElasticSearch: {}", e.getMessage(), e);
            elasticClient = null;
        }
        return true;
    }

    public void destroy() throws IOException {
        if (elasticClient != null) {
            try {
                elasticClient.close();
            } catch (IOException e) {
                log.error("Error closing ElasticSearch client: {}", e.getMessage(), e);
                throw e;
            }
            elasticClient = null;
            log.info("Destroyed ElasticSearch client");
        }
    }

    private synchronized JestClient getElasticSearchClient() {
        if (elasticClient == null) {
            elasticClient =  ElasticSearchClientBuilder.build(elasticHost, elasticPort);
        }
        return elasticClient;
    }

    public synchronized SearchResult  getQuery(String query) {
        final JestClient client = getElasticSearchClient();
        if (client != null) {

            final Search search = new Search.Builder(query)
                    .addIndex(elasticIndex)
                    .addType("logEntry")//TODO extract this
                    .build();
            try {
                final SearchResult result = client.execute(search);
                log.debug("Completed total requests query. Succeeded: {}", result.isSucceeded());
                return result;
            } catch (IOException e) {
                log.error("Error in total requests query: {}", e.getMessage(), e);
                return null;
            }
            //TODO: move to async at some point
            /*client.executeAsync(search,new JestResultHandler<JestResult>() {
                @Override
                public void completed(JestResult result) {
                    log.debug("Completed total requests query. Succeeded: {}", result.isSucceeded());
                }

                @Override
                public void failed(Exception e) {
                    log.error("Error indexing content : {}", e.getMessage(), e);
                }
            });*/
        }
        return null;
    }

    public synchronized void put(String content) {
        cacheResult(content);
        //TODO: more?
    }

    /**
     * Cache a result
     *
     * @param content
     */
    private void cacheResult(final String content) {
        final JestClient client = getElasticSearchClient();
        if (client != null) {
            final Index contentIndex = new Index.Builder(content).index(elasticIndex).type("logEntry").build();
            try {
                final DocumentResult result = client.execute(contentIndex);
                log.debug("Completed indexation of content {} with succeeded={}", content, result.isSucceeded());
            } catch (IOException e) {
                log.error("Error indexing content {}: {}", content, e.getMessage(), e);
            }
            //TODO: move to async at some point
            /*client.executeAsync(contentIndex, new JestResultHandler<JestResult>() {
                @Override
                public void completed(JestResult result) {
                    log.debug("Completed indexation of content {} with succeeded={}", content, result.isSucceeded());
                }

                @Override
                public void failed(Exception e) {
                    log.error("Error indexing content {}: {}", content, e.getMessage(), e);
                }
            });*/

        } else {
            log.warn("Content {} won't be cached, there is not target bucket", content);
        }
    }
}