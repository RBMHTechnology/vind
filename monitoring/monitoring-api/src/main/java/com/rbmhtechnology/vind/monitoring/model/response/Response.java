/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created on 02.10.17.
 */
public class Response {

    @JsonProperty("num_of_results")
    private long results;

    @JsonProperty("query_time")
    private long queryTime;

    @JsonProperty("num_of_fields")
    private long suggestedFields;

    public Response(long results, long queryTime) {
        this.results = results;
        this.queryTime = queryTime;
    }

    public Response(long results, long suggestedFields, long queryTime) {
        this.results = results;
        this.queryTime = queryTime;
        this.suggestedFields = suggestedFields;
    }

    public long getResults() {
        return results;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public long getSuggestedFields() {
        return suggestedFields;
    }
}
