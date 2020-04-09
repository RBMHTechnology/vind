package com.rbmhtechnology.vind.elasticsearch.backend.util;

import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Map;

public class ElasticRequestUtils {

    public static IndexRequest getIndexRequest(String index, Map<String,Object> jsonMap) {
        return new IndexRequest(index)
                .id(jsonMap.get(FieldUtil.ID).toString())
                .source(jsonMap);
    }

    public static UpdateRequest getUpdateRequest(String index, String id, Map<String,Object> partialDocMap) {
        final UpdateRequest request = new UpdateRequest(index, id);
        //request.script(ScriptQueryBuilder.)doc(partialDocMap);
        return request;
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
        request.settings(Settings.builder()
                .put("index.number_of_shards", 1)
                .put("index.number_of_replicas", 1)
        );

        request.mapping(ElasticMappingUtils.getDefaultMapping(), XContentType.JSON);
        return request;
    }

    public static DeleteByQueryRequest getDeleteByQueryRequest(String index, QueryBuilder query) {

        final DeleteByQueryRequest request =
                new DeleteByQueryRequest(index);
        request.setQuery(query);
        request.setRefresh(true);
        return request;
    }

    public static GetMappingsRequest getMappingsRequest(String index) {
        final GetMappingsRequest request = new GetMappingsRequest();
        request.indices(index);
        return request;
    }
}
