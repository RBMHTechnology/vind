package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.configure.SearchConfiguration;
import org.elasticsearch.action.admin.indices.validate.query.ValidateQueryRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.percolator.PercolateQueryBuilder;
import org.elasticsearch.search.Scroll;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


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
                .retryOnConflict(SearchConfiguration.get(SearchConfiguration.ELASTIC_VERSION_CONFLICT_UPDATE_RETRIES,10))
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

    /**
     * Returns the first request of a scrolling search
     * @param index index used for the search
     * @param searchSource search query
     * @param scrollTimeOut minutes the scroll session wil be kept alive.
     * @return the search request used for the first scroll.
     */
    public static SearchRequest getScrollSearchRequest(String index, SearchSourceBuilder searchSource, Long scrollTimeOut) {

        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(scrollTimeOut));
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.scroll(scroll);
        searchRequest.source(searchSource);

        return searchRequest;
    }

    /**
     * Returns the next request of a scrolling search.
     * @param scrollId id given in the previous scroll request.
     * @param scrollTimeOut minutes the scroll session wil be kept alive.
     * @return the search request used for the next scroll.
     */
    public static SearchScrollRequest getScrollSearchRequest(String scrollId, Long scrollTimeOut) {
        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(scrollTimeOut));
        final SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
        scrollRequest.scroll(scroll);

        return scrollRequest;
    }

    public static CreateIndexRequest getCreateIndexRequest(String index) {
        final CreateIndexRequest request = new CreateIndexRequest(index);
        request.settings(ElasticMappingUtils.getDefaultSettings(), XContentType.JSON);
        request.mapping(ElasticMappingUtils.getDefaultMapping(), XContentType.JSON);
        return request;
    }

    public static DeleteByQueryRequest getDeleteByQueryRequest(String index, QueryBuilder query) {

        return new DeleteByQueryRequest(index)
                .setAbortOnVersionConflict(SearchConfiguration.get(SearchConfiguration.ELASTIC_DELETE_ON_VERSION_CONFLICT,false))
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


    public static ValidateQueryRequest getValidateQueryRequest(String defaultIndex, String query) {
        final QueryBuilder builder = QueryBuilders
                .boolQuery()
                .must(QueryBuilders.queryStringQuery(query));
        final ValidateQueryRequest request = new ValidateQueryRequest(defaultIndex);
        request.explain(true);
        request.query(builder);
        return request;
    }

    public static ClearScrollRequest getCloseScrollRequest(String defaultIndex, String scrollId) {
        final ClearScrollRequest request = new ClearScrollRequest();
        request.addScrollId(scrollId);
        return request;
    }
}
