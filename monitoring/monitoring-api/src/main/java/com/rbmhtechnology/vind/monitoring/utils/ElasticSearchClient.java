/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.utils;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestResult;
import io.searchbox.core.*;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.params.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * Created on 28.02.18.
 */
public class ElasticSearchClient {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchClient.class);
    public static final String SCROLL_TIME_SESSION = "20m";

    private String elasticPort;
    private String elasticHost;
    private String elasticIndex;
    transient JestClient elasticClient;
    private String logType;

    public boolean init(String elasticHost, String elasticPort, String elasticIndex) {
        return init(elasticHost, elasticPort, elasticIndex,null, null);
    }

    public boolean init(String elasticHost, String elasticPort, String elasticIndex, String logType) {
        return init(elasticHost, elasticPort, elasticIndex, logType, null);
    }

    public boolean init(String elasticHost, String elasticPort, String elasticIndex, String logType, String processResultMappingFile) {
        this.elasticHost = elasticHost;
        this.elasticPort = elasticPort;
        this.elasticIndex = elasticIndex;
        this.logType = logType;

        try {
            final JestClient client = getElasticSearchClient();
            boolean indexExists = client.execute(new IndicesExists.Builder(elasticIndex).build()).isSucceeded();
            if (!indexExists){
                log.info("Creating elasticsearch index.");
                client.execute(new CreateIndex.Builder(elasticIndex).build());
            }

            if (StringUtils.isNotBlank(logType) && StringUtils.isNotBlank(processResultMappingFile)) {
                log.info("Updating type mapping.");
                final String mappingJson = new String(ByteStreams.toByteArray(new FileInputStream(processResultMappingFile)));
                client.execute(new PutMapping.Builder(elasticIndex, logType, mappingJson).build());
            }

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

            final Search.Builder searchBuilder = new Search.Builder(query)
                    .addIndex(elasticIndex);

            if (StringUtils.isNotEmpty(this.logType)) {
                searchBuilder.addType(this.logType);
                searchBuilder.addType(this.logType);
            }

            final Search search = searchBuilder.build();

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

    public synchronized JestResult scrollResults(String scrollId) {
        final JestClient client = getElasticSearchClient();
        if (client != null) {

            final SearchScroll scroll = new SearchScroll.Builder(scrollId, SCROLL_TIME_SESSION).build();

            try {
                final JestResult result = client.execute(scroll);
                log.debug("Completed scroll query. Succeeded: {}", result.isSucceeded());
                return result;
            } catch (IOException e) {
                log.error("Error in scroll request query: {}", e.getMessage(), e);
                throw new RuntimeException("Error in scroll request query: " + e.getMessage(), e);
            }
        }
        return null;
    }

    public synchronized SearchResult  getScrollQuery(String query) {
        final JestClient client = getElasticSearchClient();
        if (client != null) {

            final Search.Builder searchBuilder = new Search.Builder(query)
                    .addIndex(elasticIndex)
                    .setParameter(Parameters.SCROLL, SCROLL_TIME_SESSION);

            final Search search = searchBuilder.build();

            try {
                final SearchResult result = client.execute(search);
                log.debug("Completed scroll query. Succeeded: {}", result.isSucceeded());
                return result;
            } catch (IOException e) {
                log.error("Error in scroll request query: {}", e.getMessage(), e);
                throw new RuntimeException("Error in scroll request query: " + e.getMessage(), e);
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

    public String loadQueryFromFile(String fileName, Object ... args) {
        final Path path = Paths.get(Objects.requireNonNull(ElasticSearchClient.class.getClassLoader().getResource("queries/" + fileName)).getPath());
        try {
            final byte[] encoded = Files.readAllBytes(path);
            final String query = new String(encoded, "UTF-8");
            return String.format(query, args);

        } catch (IOException e) {
            log.error("Error preparing query from file '{}': {}", path, e.getMessage(), e);
            throw new RuntimeException("Error preparing query from file '" + path + "': " + e.getMessage(), e);
        }

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

    public void bulkUpdate(List<JsonObject> updates, String docType){

        final Bulk.Builder bulkProcessor = new Bulk.Builder();


        //prepare update actions
        updates.forEach( u -> {
            final String id = u.remove("_id").getAsString();
            final String index = u.remove("_index").getAsString();
            final JsonObject updateDoc = new JsonObject();
            updateDoc.add("doc",u.getAsJsonObject());
            final Update update = new Update
                    .Builder(updateDoc.toString())
                    .index(index)
                    .id(id)
                    .type(docType).build();

            bulkProcessor.addAction(update);
        });


        final JestClient client = getElasticSearchClient();
        try {
            BulkResult result = client.execute(bulkProcessor.build());
            if (result.getFailedItems().size() > 0) {
                log.error("Error executing bulk update: {} items where no updated.", result.getFailedItems().size());
            }
        } catch (IOException e) {
            log.error("Error executing bulk update: {}", e.getMessage(),e);
            throw new RuntimeException("Error executing bulk update: " + e.getMessage(), e);
        }
    }
}