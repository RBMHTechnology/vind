/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.logger.entry;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.response.Response;
import com.rbmhtechnology.vind.monitoring.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created on 03.10.17.
 */
public class GetEntry extends MonitoringEntry {

    final private EntryType type = EntryType.get;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private Response response;
    private List<String> documentIds;

    public GetEntry(Application application, ZonedDateTime start, ZonedDateTime end, long queryTime, Session session, List<String> docs, long numResult) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.documentIds = docs;
        this.response =  new Response(numResult, queryTime, start.until(end, ChronoUnit.MILLIS ));
    }

    public GetEntry(Application application, ZonedDateTime start, ZonedDateTime end, long queryTime, long elapsedTime, Session session, List<String> docs, long numResult) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.documentIds = docs;
        this.response =  new Response(numResult, queryTime, start.until(end, ChronoUnit.MILLIS ));
        this.response.setElapsedTime(elapsedTime);
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

    public Response getResponse() {
        return response;
    }

    public List<String> getDocumentIds() {
        return documentIds;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
