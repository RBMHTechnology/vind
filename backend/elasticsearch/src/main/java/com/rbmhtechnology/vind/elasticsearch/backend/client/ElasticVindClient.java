package com.rbmhtechnology.vind.elasticsearch.backend.client;

import com.rbmhtechnology.vind.elasticsearch.backend.util.ElasticRequestUtils;
import com.rbmhtechnology.vind.elasticsearch.backend.util.PainlessScript;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryRequest;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.BulkRequestBuilder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public abstract class ElasticVindClient {

    private static final Logger log = LoggerFactory.getLogger(ElasticVindClient.class);

    protected String defaultIndex;
    protected RestHighLevelClient client;
    protected int port;
    protected String host;
    protected String scheme;
    private long connectionTimeout = 1000;
    private long clientTimeout = 1000;

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

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public ElasticVindClient setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public long getClientTimeout() {
        return clientTimeout;
    }

    public ElasticVindClient setClientTimeout(long clientTimeout) {
        this.clientTimeout = clientTimeout;
        return this;
    }

    public boolean indexExists() throws IOException {
        try {
            final RequestOptions authenticatedDefaultRequest = RequestOptions.DEFAULT;
            final GetIndexRequest existsRequest = new GetIndexRequest(getDefaultIndex());
            return this.client.indices().exists(existsRequest, authenticatedDefaultRequest);
        } catch (Exception e) {
            throw new IOException(String.format("Index does not exist: %s", getDefaultIndex()),e);
        }
    }

    public boolean ping() throws IOException {
        try {
            final RequestOptions authenticatedDefaultRequest = RequestOptions.DEFAULT;
            return this.client.ping(authenticatedDefaultRequest);
        } catch (IOException e) {
            log.error("Unable to ping Elasticsearch server {}://{}:{}", scheme, host, port,e);
            throw new IOException(String.format("Unable to ping Elasticsearch server %s://%s:%s", scheme, host, port),e);
        }
    }

    public BulkResponse add(Map<String, Object> jsonDoc) throws IOException {
        final BulkRequest bulkIndexRequest = new BulkRequest(defaultIndex);
        bulkIndexRequest.add(ElasticRequestUtils.getIndexRequest(jsonDoc));
        bulkIndexRequest.timeout(TimeValue.timeValueMillis(connectionTimeout));
        bulkIndexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        return BulkRequestBuilder.executeBulk(bulkIndexRequest,RequestOptions.DEFAULT,defaultIndex,client);
    }

    public BulkResponse add(List<Map<String, Object>> jsonDocs) throws IOException {
        final BulkRequest bulkIndexRequest = new BulkRequest(defaultIndex);
        jsonDocs.forEach( jsonDoc -> bulkIndexRequest.add(ElasticRequestUtils.getIndexRequest(jsonDoc)) );
        bulkIndexRequest.timeout(TimeValue.timeValueMillis(connectionTimeout));
        bulkIndexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        return BulkRequestBuilder.executeBulk(bulkIndexRequest,RequestOptions.DEFAULT,defaultIndex,client);
    }

    public UpdateResponse update(String id, PainlessScript.ScriptBuilder script) throws IOException {
        final UpdateRequest request = ElasticRequestUtils.getUpdateRequest(defaultIndex, id, script);
        return client.update(request, RequestOptions.DEFAULT);
    }

    public GetResponse realTimeGet(String id) throws IOException {
        return client.get(ElasticRequestUtils.getRealTimeGetRequest(defaultIndex,id),RequestOptions.DEFAULT);
    }

    public MultiGetResponse realTimeGet(List<String> ids) throws IOException {
        final MultiGetRequest request = ElasticRequestUtils.getRealTimeGetRequest(defaultIndex, ids);
        return client.mget(request, RequestOptions.DEFAULT);
    }

    public DeleteResponse deleteById(String id) throws IOException {
        return client.delete(ElasticRequestUtils.getDeleteRequest(defaultIndex,id),RequestOptions.DEFAULT);
    }

    public CreateIndexResponse createIndex(String indexName) throws IOException {
        return client.indices().create(ElasticRequestUtils.getCreateIndexRequest(indexName), RequestOptions.DEFAULT);
    }

    public BulkByScrollResponse deleteByQuery(QueryBuilder query) throws IOException {
        final DeleteByQueryRequest request = ElasticRequestUtils.getDeleteByQueryRequest(defaultIndex, query);
        return client.deleteByQuery(request,RequestOptions.DEFAULT);
    }

    public GetMappingsResponse getMappings() throws IOException {
        final GetMappingsRequest request = ElasticRequestUtils.getMappingsRequest(defaultIndex);
        return client.indices().getMapping(request, RequestOptions.DEFAULT);
    }

    public BulkResponse addPercolateQuery(String queryId, QueryBuilder query) throws IOException {
        return addPercolateQuery(queryId, query, new HashMap<>());
    }
    public BulkResponse addPercolateQuery(String queryId, QueryBuilder query, Map<String, Object> metadata) throws IOException {
        metadata.put("query", query);
        final XContentBuilder queryDoc = mapToXContentBuilder(metadata);

        final BulkRequest bulkIndexRequest = new BulkRequest(defaultIndex);
        bulkIndexRequest.add(ElasticRequestUtils.addPercolatorQueryRequest(defaultIndex, queryId, queryDoc));
        bulkIndexRequest.timeout(TimeValue.timeValueMillis(connectionTimeout));
        bulkIndexRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
        return client.bulk(bulkIndexRequest, RequestOptions.DEFAULT);
    }

    public SearchResponse percolatorDocQuery(List<Map<String, Object>> mapDocs, QueryBuilder query) throws IOException {
        final List<XContentBuilder> xContentDocs = new ArrayList<>();
        for (Map<String, Object> mapDoc : mapDocs) {
            xContentDocs.add(mapToXContentBuilder(mapDoc));
        }
        final SearchRequest request = ElasticRequestUtils.percolateDocumentRequest(defaultIndex, xContentDocs, query);
        return client.search(request, RequestOptions.DEFAULT);
    }

    public SearchResponse percolatorDocQuery(Map<String, Object> mapDoc, QueryBuilder query) throws IOException {
        return percolatorDocQuery(Collections.singletonList(mapDoc), query);
    }
    public SearchResponse percolatorDocQuery(Map<String, Object> matchingDoc) throws IOException {
        return percolatorDocQuery(Collections.singletonList(matchingDoc));
    }
    public SearchResponse percolatorDocQuery(List<Map<String, Object>> mapDoc) throws IOException {
        return percolatorDocQuery(mapDoc, null);
    }

    public void close() throws IOException {
        try {
            this.client.close();
        } catch (IOException e) {
            log.error("Unable to close Elasticsearch client connection to {}://{}:{}", scheme, host, port,e);
            throw new IOException(String.format("Unable to ping Elasticsearch client connection to %s://%s:%s", scheme, host, port),e);
        }
    }

    public SearchResponse scrolledQuery(SearchSourceBuilder query, String scrollId, Long scrollTimeOut) throws IOException {
        if (Objects.nonNull(scrollId)){
            return getNextScroll(scrollId, scrollTimeOut);
        } else {
            return startScrolledQuery(query, scrollTimeOut);
        }
    }

    public SearchResponse getNextScroll(String scrollId, Long scrollTimeOut) throws IOException {
        final SearchScrollRequest request = ElasticRequestUtils.getScrollSearchRequest(scrollId, scrollTimeOut);
        return client.scroll(request,RequestOptions.DEFAULT);
    }

    public SearchResponse startScrolledQuery(SearchSourceBuilder query, Long scrollTimeOut) throws IOException {
        final SearchRequest request = ElasticRequestUtils.getScrollSearchRequest(defaultIndex, query, scrollTimeOut);
        return client.search(request,RequestOptions.DEFAULT);
    }

    public SearchResponse query(SearchSourceBuilder query) throws IOException {
        final SearchRequest request = ElasticRequestUtils.getSearchRequest(defaultIndex, query);
        return client.search(request,RequestOptions.DEFAULT);
    }


    private XContentBuilder mapToXContentBuilder(Map<String, Object> doc) throws IOException {
        final XContentBuilder builder = jsonBuilder().startObject();
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            String k = entry.getKey();
            Object value = entry.getValue();
            builder.field(k, value);
        }
        return builder.endObject();
    }

    public ValidateQueryResponse validateQuery(String query) throws IOException {
        final ValidateQueryRequest request = ElasticRequestUtils.getValidateQueryRequest(defaultIndex, query);
        return client.indices().validateQuery(request, RequestOptions.DEFAULT);
    }

    public void closeScroll(String scrollId) throws IOException {
        final ClearScrollRequest request = ElasticRequestUtils.getCloseScrollRequest(defaultIndex, scrollId);
        client.clearScroll(request, RequestOptions.DEFAULT);
    }

    protected RestClientBuilder.RequestConfigCallback applyTimeouts(Long connectionTimeout, Long socketTimeout) {
        return requestConfig -> {
            if (connectionTimeout != null) {
                requestConfig.setConnectTimeout(connectionTimeout.intValue());
            }
            if (socketTimeout != null) {
                requestConfig.setSocketTimeout(socketTimeout.intValue());
            }
            return requestConfig;
        };
    }

    public static class Builder {
        private String defaultIndex;
        private final int port;
        private final String scheme;
        private final String host;

        private Long connectionTimeout;
        private Long socketTimeout;

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

        public Builder setConnectionTimeout(Long connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder setSocketTimeout(Long socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public ElasticVindClient buildWithBasicAuth(String user, String key) {
            return ElasticVindClientBasicAuth.build(defaultIndex, port, scheme, host, connectionTimeout, socketTimeout, user, key);
        }
        public ElasticVindClient buildWithApiKeyAuth(String id, String key) {
            return ElasticVindClientApiKeyAuth.build(defaultIndex, port, scheme, host, connectionTimeout, socketTimeout, id, key);
        }
        public ElasticVindClient build() {
            return ElasticVindClientNoAuth.build(defaultIndex, port, scheme, host, connectionTimeout, socketTimeout);
        }
    }
}
