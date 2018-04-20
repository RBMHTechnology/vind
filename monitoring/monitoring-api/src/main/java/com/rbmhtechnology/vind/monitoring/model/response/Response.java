/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created on 02.10.17.
 */
public class Response {

    @JsonProperty("num_of_results")
    private long results;

    @JsonProperty("query_time")
    private long queryTime;

    @JsonProperty("elapsed_time")
    private long elapsedTime;

    @JsonProperty("vind_time")
    private long vindTime;

    @JsonProperty("num_of_fields")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long suggestedFields = null;

    public Response(long results, long queryTime, long vindTime) {
        this.results = results;
        this.queryTime = queryTime;
        this.vindTime = vindTime;
    }

    public Response(long results, long queryTime, long vindTime, long suggestedFields) {
        this.results = results;
        this.queryTime = queryTime;
        this.vindTime = vindTime;
        this.suggestedFields = suggestedFields;
    }

    public long getResults() {
        return results;
    }

    public long getQueryTime() {
        return queryTime;
    }

    public long getVindTime() {
        return vindTime;
    }

    public Long getSuggestedFields() {
        return suggestedFields;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public Response setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }
}
