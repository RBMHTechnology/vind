/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.logger.entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.result.BeanSearchResult;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.report.model.application.Application;
import com.rbmhtechnology.vind.report.model.interaction.Interaction;
import com.rbmhtechnology.vind.report.model.request.SearchRequest;
import com.rbmhtechnology.vind.report.model.request.SuggestionRequest;
import com.rbmhtechnology.vind.report.model.request.facet.FacetMixin;
import com.rbmhtechnology.vind.report.model.request.filter.AndFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.FilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.NotFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.OrFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.sort.SortMixIn;
import com.rbmhtechnology.vind.report.model.response.Response;
import com.rbmhtechnology.vind.report.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created on 03.10.17.
 */
public class InteractionEntry extends LogEntry{

    private ObjectMapper mapper = LogEntry.getMapper();

    final private EntryType type = EntryType.interaction;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private Interaction request;

    public InteractionEntry(Application application, String source, Interaction interaction, ZonedDateTime start, ZonedDateTime end, Session session) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
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
