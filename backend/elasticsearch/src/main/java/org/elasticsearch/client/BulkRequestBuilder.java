package org.elasticsearch.client;

import org.apache.http.client.methods.HttpPost;
import org.elasticsearch.action.ActionRequestValidationException;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;

import java.io.IOException;

public class BulkRequestBuilder {

    private static Request toRequest(BulkRequest bulkRequest, String indexName) throws IOException {
        Request request = new Request(HttpPost.METHOD_NAME, "/"+indexName+"/_bulk");
        Request origRequest = RequestConverters.bulk(bulkRequest);
        request.setEntity(origRequest.getEntity());
        request.setOptions(origRequest.getOptions());
        request.addParameters(origRequest.getParameters());
        return request;
    }

    public static BulkResponse executeBulk(
            BulkRequest bulkRequest,
            RequestOptions options,
            String indexName,
            RestHighLevelClient client
    ) throws IOException {

        ActionRequestValidationException validationException = bulkRequest.validate();
        if (validationException != null && validationException.validationErrors().isEmpty() == false) {
            throw validationException;
        }

        Request req = toRequest(bulkRequest, indexName);
        req.setOptions(options);
        Response response;
        try {
            response = client.getLowLevelClient().performRequest(req);
        } catch (ResponseException e) {
            throw new IOException("Indexing was not successful", e);
        }

        try {
            return client.parseEntity(response.getEntity(), BulkResponse::fromXContent);
        } catch(Exception e) {
            throw new IOException("Unable to parse response body for " + response, e);
        }
    }
}
