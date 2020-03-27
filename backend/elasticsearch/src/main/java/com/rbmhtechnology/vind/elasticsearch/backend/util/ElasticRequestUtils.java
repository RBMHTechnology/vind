package com.rbmhtechnology.vind.elasticsearch.backend.util;

import org.elasticsearch.action.index.IndexRequest;

import java.util.Map;

public class ElasticRequestUtils {

    public static IndexRequest getIndexRequest(String defaultIndex, Map<String,Object> jsonMap) {
        return new IndexRequest(defaultIndex)
                .id(jsonMap.get(FieldUtil.ID).toString())
                .source(jsonMap);
    }
}
