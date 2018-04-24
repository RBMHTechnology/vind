/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.logger.entry;

import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.request.UpdateRequest;
import com.rbmhtechnology.vind.monitoring.model.response.Response;
import com.rbmhtechnology.vind.monitoring.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created on 03.10.17.
 */
public class UpdateEntry extends MonitoringEntry {

    final private EntryType type = EntryType.update;
    private final Application application;
    private final Session session;
    private final ZonedDateTime timeStamp;
    private final Response response;
    private final UpdateRequest request;
    private final Boolean success;


    public UpdateEntry(Application application, ZonedDateTime start, ZonedDateTime end, Session session, Update update, Boolean success) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.success = success;
        this.request = new UpdateRequest(update);
        this.response =  new Response(-1, -1, start.until(end, ChronoUnit.MILLIS ));
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

    public UpdateRequest getRequest() {
        return request;
    }

    public Boolean getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
