package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.configure.SearchConfiguration;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.percolator.PercolateQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

public class ElasticRequestUtils {

    public static IndexRequest getIndexRequest(Map<String,Object> jsonMap) {
        return new IndexRequest()
                .id(jsonMap.get(FieldUtil.ID).toString())
                .source(jsonMap);
    }

    public static IndexRequest getIndexRequest(String index, Map<String,Object> jsonMap) {
        return new IndexRequest(index)
                .id(jsonMap.get(FieldUtil.ID).toString())
                .source(jsonMap);
    }

    public static UpdateRequest getUpdateRequest(String index, String id, PainlessScript.ScriptBuilder script) {
       return new UpdateRequest(index, id)
               .retryOnConflict(3)
                .script(script.build())
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
    }

    public static GetRequest getRealTimeGetRequest(String index, String docId) {
        return new GetRequest(index, docId);
    }

    public static MultiGetRequest getRealTimeGetRequest(String index, List<String> docIds) {
        final MultiGetRequest request = new MultiGetRequest();
        docIds.forEach(id -> request.add(new MultiGetRequest.Item(index, id)));
        return request;
    }


    public static DeleteRequest getDeleteRequest(String index, String docId) {
        return new DeleteRequest(index, docId)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
    }

    public static SearchRequest getSearchRequest(String index, SearchSourceBuilder searchSource) {

        final SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSource);
        return searchRequest;
    }

    public static CreateIndexRequest getCreateIndexRequest(String index) {
        final CreateIndexRequest request = new CreateIndexRequest(index);
        request.settings(ElasticMappingUtils.getDefaultSettings(), XContentType.JSON);
        request.mapping(ElasticMappingUtils.getDefaultMapping(), XContentType.JSON);
        return request;
    }

    public static DeleteByQueryRequest getDeleteByQueryRequest(String index, QueryBuilder query) {

        return new DeleteByQueryRequest(index)
                .setQuery(query)
                .setRefresh(true);
    }

    public static GetMappingsRequest getMappingsRequest(String index) {
        final GetMappingsRequest request = new GetMappingsRequest();
        request.indices(index);
        return request;
    }

    public static IndexRequest addPercolatorQueryRequest(String index, Map<String,Object> query) {
        return new IndexRequest(index)
                .source(query);
    }
    public static IndexRequest addPercolatorQueryRequest(String index, String id, XContentBuilder query) {
        return new IndexRequest(index)
                .id(id)
                .source(query);
    }

    public static SearchRequest percolateDocumentRequest(String index, List<XContentBuilder> docs, QueryBuilder query) {
        final SearchSourceBuilder searchSource = new SearchSourceBuilder();
        final PercolateQueryBuilder docQuery = new PercolateQueryBuilder("query", docs.stream().map(BytesReference::bytes).collect(Collectors.toList()), XContentType.JSON);
        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(docQuery);
        Optional.ofNullable(query).ifPresent(boolQueryBuilder::must);
        searchSource.query(boolQueryBuilder);
        final SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(searchSource);
        return searchRequest;
    }


}
