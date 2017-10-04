/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.logger.entry;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.report.model.application.Application;
import com.rbmhtechnology.vind.report.model.request.FullTextRequest;
import com.rbmhtechnology.vind.report.model.request.Paging;
import com.rbmhtechnology.vind.report.model.request.SearchRequest;
import com.rbmhtechnology.vind.report.model.response.Response;
import com.rbmhtechnology.vind.report.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created on 03.10.17.
 */
public class FullTextEntry implements LogEntry{

    final private EntryType type = EntryType.fulltext;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private FullTextRequest request;
    private Response response;
    private String sorting;
    private Paging paging;


    public FullTextEntry(Application application, String source, FulltextSearch search, SearchResult result, ZonedDateTime start, ZonedDateTime end, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.request = new FullTextRequest(search,source);
        this.response = new Response(result.getNumOfResults(), start.until(end, ChronoUnit.MILLIS));
        this.sorting = search.getSorting().toString();
        this.paging = new Paging(search.getResultSet());
    }

    public FullTextEntry(Application application, String source, FulltextSearch search, BeanSearchResult result, ZonedDateTime start, ZonedDateTime end, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.request = new FullTextRequest(search,source);
        this.response = new Response(result.getNumOfResults(), start.until(end, ChronoUnit.MILLIS));
        this.sorting = search.getSorting().toString();
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public EntryType getType() {
        return type;
    }

    @Override
    public ZonedDateTime getTimeStamp() {
        return timeStamp;
    }

    @Override
    public SearchRequest getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public String getSorting() {
        return sorting;
    }

    public Paging getPaging() {
        return paging;
    }
}
