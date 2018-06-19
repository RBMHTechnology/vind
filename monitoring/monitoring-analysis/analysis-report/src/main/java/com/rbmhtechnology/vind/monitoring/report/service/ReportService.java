/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.service;

import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.configuration.ReportConfiguration;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created on 28.02.18.
 */
public abstract class ReportService implements AutoCloseable {

    private ZonedDateTime from;
    private ZonedDateTime to;
    private ZoneId zoneId;
    private ReportConfiguration configuration;

    public ReportService(ReportConfiguration configuration, ZonedDateTime from, ZonedDateTime to){
        this.from = from;
        this.to = to;
        this.zoneId = from.getZone();
        this.configuration = configuration;
    }

    public ReportService(long from, long to, String zoneId, ReportConfiguration configuration){
        this.zoneId = ZoneId.of(zoneId);
        this.from = ZonedDateTime.ofInstant(Instant.ofEpochMilli(from), this.zoneId);
        this.to = ZonedDateTime.ofInstant(Instant.ofEpochMilli(to), this.zoneId);
    }

    public ReportService(Date from, Date to, String zoneId, ReportConfiguration configuration){
        this.zoneId = ZoneId.of(zoneId);
        this.from = ZonedDateTime.ofInstant(from.toInstant(), this.zoneId);
        this.to = ZonedDateTime.ofInstant(to.toInstant(), this.zoneId);
    }

    //Getters
    public ZonedDateTime getFrom() {
        return from;
    }

    public ZonedDateTime getTo() {
        return to;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }

    public ReportConfiguration getConfiguration() {
        return configuration;
    }

    //Setters
    public ReportService setFrom(ZonedDateTime from) {
        this.from = from;
        return this;
    }

    public ReportService setFrom(Date from, String timeZoneId) {
        return this.setFrom(ZonedDateTime.ofInstant(from.toInstant(), ZoneId.of(timeZoneId)));
    }

    public ReportService setFrom(long from, String timeZoneId) {
        return this.setFrom(ZonedDateTime.ofInstant(Instant.ofEpochMilli(from), ZoneId.of(timeZoneId)));
    }

    public ReportService setTo(ZonedDateTime to) {
        this.to = to;
        return this;
    }

    public ReportService setTo(Date to, String timeZoneId) {
        return this.setFrom(ZonedDateTime.ofInstant(to.toInstant(), ZoneId.of(timeZoneId)));

    }

    public ReportService setTo(long to, String timeZoneId) {
        return this.setFrom(ZonedDateTime.ofInstant(Instant.ofEpochMilli(to), ZoneId.of(timeZoneId)));

    }

    public void setConfiguration(ReportConfiguration configuration) {
        this.configuration = configuration;
    }

    //Total number of requests
    public abstract long getTotalRequests();
    //Top Day
    public abstract LinkedHashMap<ZonedDateTime, Long> getTopDays();

    //Top users
    public abstract LinkedHashMap<String, Long> getTopUsers();

    //Facets
    ///Facet Fields
    public abstract LinkedHashMap<String, JsonObject> getTopFacetFields();

    ///Facet Values per field?
    public abstract LinkedHashMap<String, LinkedHashMap<Object,Long>> getFacetFieldsValues(List<String> fields);

    //Suggestions
    ///Suggestion fields
    public abstract LinkedHashMap<String, JsonObject> getTopSuggestionFields();

    public abstract LinkedHashMap<String, JsonObject> getTopFilterFields();

    ///Suggestion values per field
    public abstract LinkedHashMap<String, LinkedHashMap<Object, Long>> getSuggestionFieldsValues(List<String> fields);

    //Advanced search?
    ///Advanced search fields
    ///Advanced search values per field

    //Full Text
    ///Fulltext top queries
    public abstract LinkedHashMap<String, Long> getTopQueries();

    //Filtered fulltext top queries?
    public abstract LinkedHashMap<String, Long> getTopFilteredQueries(String regexFilter);

    public abstract LinkedHashMap<String, LinkedHashMap<Object, Long>> getFilterFieldsValues(List<String> fields);
}
