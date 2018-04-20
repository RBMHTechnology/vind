/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.api.result;

/**
 * Created on 05.04.18.
 */
public class DeleteResult {

    private final Long queryTime;
    private Long elapsedTime;

    public DeleteResult(Long queryTime) {
        this.queryTime = queryTime;
    }

    public Long getQueryTime() {
        return queryTime;
    }

    public Long getElapsedTime() {
        return elapsedTime;
    }

    public DeleteResult setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }
}
