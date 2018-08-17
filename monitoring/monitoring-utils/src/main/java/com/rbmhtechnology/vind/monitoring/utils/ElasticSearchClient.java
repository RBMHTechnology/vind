/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.utils;

import com.google.common.io.ByteStreams;
import com.google.gson.JsonObject;
import io.redlink.utils.ResourceLoaderUtils;
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created on 28.02.18.
 */
public class ElasticSearchClient {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchClient.class);
    public static final String SCROLL_TIME_SESSION = "30m";
    public static final int ES_MAX_TRIES = 3;
    public static final int ES_WAIT_TIME = 3000;

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

    public boolean init(String elasticHost, String elasticPort, String elasticIndex, String logType, Path processResultMappingFile) {
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

            if (StringUtils.isNotBlank(logType) && (processResultMappingFile != null)) {
                log.info("Updating type mapping.");
                final String mappingJson = new String(ByteStreams.toByteArray(Files.newInputStream(processResultMappingFile)));
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

    private synchronized JestClient getElasticSearchClient(boolean forceRebuild) {
        if(forceRebuild) {
            try {
                this.elasticClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.elasticClient = null;
        }

        if (elasticClient == null) {
            elasticClient =  ElasticSearchClientBuilder.build(elasticHost, elasticPort);
        }
        return elasticClient;
    }

    private synchronized JestClient getElasticSearchClient() {
        return this.getElasticSearchClient(false);
    }

    public synchronized SearchResult  getQuery(String query) {
        final JestClient client = getElasticSearchClient();
        if (client != null) {

            final Search.Builder searchBuilder = new Search.Builder(query)
                    .addIndex(elasticIndex);

            if (StringUtils.isNotEmpty(this.logType)) {
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


    public synchronized JestResult scrollResults(String scrollId) {
        final JestClient client = getElasticSearchClient();
        try {
            return scrollResults(scrollId, 0 , client);
        } catch (InterruptedException e) {
            log.error("Error in scroll request: {}", e.getMessage(), e );
            throw new RuntimeException("Error in scroll request query: " + e.getMessage(), e);
        }
    }

    private synchronized JestResult scrollResults(String scrollId, int retry, JestClient client) throws InterruptedException {
        if (client != null) {
            final SearchScroll scroll = new SearchScroll.Builder(scrollId, SCROLL_TIME_SESSION).build();
            try {
                final JestResult result = client.execute(scroll);
                log.debug("Completed scroll query. Succeeded: {}", result.isSucceeded());
                return result;
            } catch (IOException e) {
                log.warn("Error in scroll request query: {}", e.getMessage(), e);
                if(retry > ES_MAX_TRIES) {
                    log.error("Error in scroll request: reached maximum number of scroll tries [{}].", retry);
                    throw new RuntimeException("Error in scroll request query: " + e.getMessage(), e);

                } else {
                    Thread.sleep((retry + 1) * ES_WAIT_TIME);
                    return scrollResults(scrollId, retry + 1, client);
                }
            }
        }
        log.error("Error in scroll request query: ES client has not been initialized, client is null.");
        throw new RuntimeException("Error in scroll request query: ES client has not been initialized, client is null.");
    }

    public synchronized SearchResult  getScrollQuery(String query) {
        final JestClient client = getElasticSearchClient();
        try {
            return getScrollQuery(query, 0 , client);
        } catch (InterruptedException e) {
            log.error("Error in query scroll request: {}", e.getMessage(), e );
            throw new RuntimeException("Error in query scroll request query: " + e.getMessage(), e);
        }
    }

    private synchronized SearchResult  getScrollQuery(String query, int retry, JestClient client) throws InterruptedException {

        if (client != null) {
            final Search.Builder searchBuilder = new Search.Builder(query)
                    .addIndex(elasticIndex)
                    .setParameter(Parameters.SCROLL, SCROLL_TIME_SESSION);

            final Search search = searchBuilder.build();

            try {
                final SearchResult result = client.execute(search);
                log.debug("Completed query scroll query in {} tries. Succeeded: {}", retry + 1, result.isSucceeded());
                return result;
            } catch (IOException e) {
                log.warn("Try {} - Error in query scroll request query: {}", retry, e.getMessage(), e);
                if(retry > ES_MAX_TRIES) {
                    log.error("Error in query scroll request: reached maximum number of scroll tries [{}].", retry);
                    throw new RuntimeException("Error in query scroll request query: " + e.getMessage(), e);
                } else {
                    Thread.sleep((retry + 1) * ES_WAIT_TIME);
                    return getScrollQuery(query, retry + 1, client);
                }
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
        log.error("Error in scroll request query: ES client has not been initialized, client is null.");
        throw new RuntimeException("Error in scroll request query: ES client has not been initialized, client is null.");
    }

    public synchronized void put(String content) {
        cacheResult(content);
        //TODO: more?
    }

    public String loadQueryFromFile(String fileName, Object ... args) {
        final Path path = ResourceLoaderUtils.getResourceAsPath("queries/" + fileName);
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
            bulkUpdate(bulkProcessor, 0, client);
        } catch (InterruptedException e) {
            log.error("Error executing bulk update: {}", e.getMessage(),e);
            throw new RuntimeException("Error executing bulk update: " + e.getMessage(), e);
        }
    }

    private void bulkUpdate(Bulk.Builder bulkProcessor, int retries, JestClient client) throws InterruptedException {
        if (Objects.nonNull(client)) {
            try {
                BulkResult result = client.execute(bulkProcessor.build());
                if (result.getFailedItems().size() > 0) {
                    final String errorIds = result.getFailedItems().stream()
                                .map( fi -> fi.id)
                                .collect(Collectors.joining(", "));
                    log.error("Error executing bulk update: {} items where no updated [{}].", result.getFailedItems().size(), errorIds);

                }
            } catch (IOException e) {
                log.warn("Error executing bulk update: {}", e.getMessage(), e);
                if (retries > ES_MAX_TRIES) {
                    log.error("Error executing bulk update: reached maximum number of retries [{}].", retries);
                    throw new RuntimeException("Error executing bulk update: " + e.getMessage(), e);
                } else {
                    Thread.sleep((retries + 1) * ES_WAIT_TIME);
                    bulkUpdate(bulkProcessor, retries + 1, client);
                }
            }
        } else {
            log.error("Error in bulk update request: ES client has not been initialized, client is null.");
            throw new RuntimeException("Error in bulk update request: ES client has not been initialized, client is null.");
        }
    }

    public void closeScroll(String scrollId) {
        final ClearScroll clearScroll = new ClearScroll.Builder().addScrollId(scrollId).build();
        try {
            final JestResult result = getElasticSearchClient().execute(clearScroll);
            log.debug("Closed scroll query {}. Succeeded: {}", scrollId, result.isSucceeded());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
