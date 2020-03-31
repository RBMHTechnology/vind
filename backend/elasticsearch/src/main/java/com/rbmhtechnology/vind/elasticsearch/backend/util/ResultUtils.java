package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.api.result.GetResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import org.apache.commons.collections.CollectionUtils;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ResultUtils {

    public static GetResult buildRealTimeGetResult(MultiGetResponse response, RealTimeGet query, DocumentFactory factory, long elapsedTime) {

        final List<Document> docResults = new ArrayList<>();

        final List<MultiGetItemResponse> results = Arrays.asList(response.getResponses());
        if(CollectionUtils.isNotEmpty(results)){
            docResults.addAll(results.stream()
                    .map(MultiGetItemResponse::getResponse)
                    .map(GetResponse::getSourceAsMap)
                    .map(jsonMap -> DocumentUtil.buildVindDoc(jsonMap ,factory,null))
                    .collect(Collectors.toList()));
        }
        final long nResults = docResults.size();
        return new GetResult(nResults,docResults,query,factory,elapsedTime).setElapsedTime(elapsedTime);
    }
}
