/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.api.result;

/**
 * Created on 05.04.18.
 */
public class IndexResult {

    private final Long queryTime;
    private Long elapsedTime;

    public IndexResult(Long queryTime) {
        this.queryTime = queryTime;
    }

    public Long getQueryTime() {
        return queryTime;
    }

    public Long getElapsedTime() {
        return elapsedTime;
    }

    public IndexResult setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }
}
