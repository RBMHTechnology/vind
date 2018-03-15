/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.logger.entry;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.request.FullTextRequest;
import com.rbmhtechnology.vind.monitoring.model.request.Paging.Paging;
import com.rbmhtechnology.vind.monitoring.model.request.SearchRequest;
import com.rbmhtechnology.vind.monitoring.model.response.Response;
import com.rbmhtechnology.vind.monitoring.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created on 03.10.17.
 */
public class FullTextEntry extends MonitoringEntry {

    final private EntryType type = EntryType.fulltext;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private FullTextRequest request;
    private Response response;
    private List<Sort> sorting;
    private Paging paging;

    public FullTextEntry() {
    }

    public FullTextEntry(SearchServer server, DocumentFactory factory, Application application, FulltextSearch search, SearchResult result, ZonedDateTime start, ZonedDateTime end, long queryTime, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.request = new FullTextRequest(search.copy(), server.getRawQuery(search,factory));
        this.response = new Response(result.getNumOfResults(), queryTime, start.until(end, ChronoUnit.MILLIS));
        this.sorting = search.getSorting();
        this.paging = new Paging(search.getResultSet());
    }

    public FullTextEntry(SearchServer server, DocumentFactory factory, Application application, FulltextSearch search, BeanSearchResult result, ZonedDateTime start, ZonedDateTime end, long queryTime, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;

        this.request = new FullTextRequest(search.copy(), server.getRawQuery(search,factory));
        this.response = new Response(result.getNumOfResults(), queryTime, start.until(end, ChronoUnit.MILLIS));
        this.sorting = search.getSorting();
        this.paging = new Paging(search.getResultSet());
    }

    public FullTextEntry(SearchServer server, DocumentFactory factory, Application application, FulltextSearch search, SearchResult result, ZonedDateTime start, ZonedDateTime end, long queryTime, long elapsedTime, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.request = new FullTextRequest(search.copy(), server.getRawQuery(search,factory));
        this.response = new Response(result.getNumOfResults(), queryTime, start.until(end, ChronoUnit.MILLIS));
        this.response.setElapsedTime(elapsedTime);
        this.sorting = search.getSorting();
        this.paging = new Paging(search.getResultSet());
    }

    public FullTextEntry(SearchServer server, DocumentFactory factory, Application application, FulltextSearch search, BeanSearchResult result, ZonedDateTime start, ZonedDateTime end, long queryTime, long elapsedTime, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;

        this.request = new FullTextRequest(search.copy(), server.getRawQuery(search,factory));
        this.response = new Response(result.getNumOfResults(), queryTime, start.until(end, ChronoUnit.MILLIS));
        this.response.setElapsedTime(elapsedTime);
        this.sorting = search.getSorting();
        this.paging = new Paging(search.getResultSet());
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

    public SearchRequest getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
    }

    public List<Sort> getSorting() {
        return sorting;
    }

    public Paging getPaging() {
        return paging;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
