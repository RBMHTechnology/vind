/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.logger.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.session.Session;
import com.rbmhtechnology.vind.monitoring.model.interaction.Interaction;

import java.time.ZonedDateTime;

/**
 * Created on 03.10.17.
 */
public class InteractionEntry extends MonitoringEntry {

    private ObjectMapper mapper = MonitoringEntry.getMapper();

    final private EntryType type = EntryType.interaction;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private Interaction request;

    public InteractionEntry(Application application, Interaction interaction, ZonedDateTime timestamp, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = timestamp;
        this.request = interaction;
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

    public Interaction getRequest() {
        return request;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
