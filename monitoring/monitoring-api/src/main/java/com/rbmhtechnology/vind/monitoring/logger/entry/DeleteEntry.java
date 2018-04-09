/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.logger.entry;

import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.response.Response;
import com.rbmhtechnology.vind.monitoring.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Created on 03.10.17.
 */
public class DeleteEntry extends MonitoringEntry {

    final private EntryType type = EntryType.delete;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private Response response;

    public DeleteEntry(Application application, ZonedDateTime start, ZonedDateTime end, long queryTime, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.response =  new Response(-1, queryTime, start.until(end, ChronoUnit.MILLIS ));

    }

    public DeleteEntry(Application application, ZonedDateTime start, ZonedDateTime end, long queryTime, long elapsedTime, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.response =  new Response(-1, queryTime, start.until(end, ChronoUnit.MILLIS )).setElapsedTime(elapsedTime);
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

    @Override
    public String toString() {
        return toJson();
    }
}
