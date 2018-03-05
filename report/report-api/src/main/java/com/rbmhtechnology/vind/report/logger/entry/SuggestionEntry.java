/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.logger.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.report.model.application.Application;
import com.rbmhtechnology.vind.report.model.request.SearchRequest;
import com.rbmhtechnology.vind.report.model.request.SuggestionRequest;
import com.rbmhtechnology.vind.report.model.response.Response;
import com.rbmhtechnology.vind.report.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created on 03.10.17.
 */
public class SuggestionEntry extends LogEntry{

    private ObjectMapper mapper = LogEntry.getMapper();

    final private EntryType type = EntryType.suggestion;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private SuggestionRequest request;
    private Response response;

    public SuggestionEntry() {
    }

    public SuggestionEntry(SearchServer server, DocumentFactory factory, Application application, String source, ExecutableSuggestionSearch search, SuggestionResult result, ZonedDateTime start, ZonedDateTime end, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;

        this.request = new SuggestionRequest(search/*.copy()*/, source);//TODO add copy to suggestion search
        this.response = new Response(result.size(), result.getSuggestedFields().size() ,start.until(end, ChronoUnit.MILLIS));
    }

    public SuggestionEntry(Application application, ExecutableSuggestionSearch search, SuggestionResult result, ZonedDateTime start, ZonedDateTime end, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;

        this.request = new SuggestionRequest(search/*.copy()*/, this.type.name());//TODO add copy to suggestion search
        this.response = new Response(result.size(), result.getSuggestedFields().size() ,start.until(end, ChronoUnit.MILLIS));
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
    
    @Override
    public String toString() {
        return toJson();
    }
}
