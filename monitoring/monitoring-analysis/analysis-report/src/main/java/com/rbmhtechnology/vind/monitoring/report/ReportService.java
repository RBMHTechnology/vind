/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

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
    private String applicationId;

    public ReportService(ZonedDateTime from, ZonedDateTime to, String applicationId){
        this.from = from;
        this.to = to;
        this.zoneId = from.getZone();
        this.applicationId = applicationId;
    }

    public ReportService(long from, long to, String zoneId, String applicationId){
        this.zoneId = ZoneId.of(zoneId);
        this.from = ZonedDateTime.ofInstant(Instant.ofEpochMilli(from), this.zoneId);
        this.to = ZonedDateTime.ofInstant(Instant.ofEpochMilli(to), this.zoneId);
        this.applicationId = applicationId;
    }

    public ReportService(Date from, Date to, String zoneId, String applicationId){
        this.zoneId = ZoneId.of(zoneId);
        this.from = ZonedDateTime.ofInstant(from.toInstant(), this.zoneId);
        this.to = ZonedDateTime.ofInstant(to.toInstant(), this.zoneId);
        this.applicationId = applicationId;
    }

    //Getters
    public ZonedDateTime getFrom() {
        return from;
    }

    public ZonedDateTime getTo() {
        return to;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public ZoneId getZoneId() {
        return zoneId;
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

    public ReportService setApplicationId(String applicationId) {
        this.applicationId = applicationId;
        return this;
    }

    //Total number of requests
    public abstract long getTotalRequests();
    //Top Day
    public abstract LinkedHashMap<ZonedDateTime, Integer> getTopDays();

    //Top users
    public abstract LinkedHashMap<String, Long> getTopUsers();

    //Facets
    ///Facet Fields
    public abstract LinkedHashMap<String, Long> getTopFaceFields();

    ///Facet Values per field?
    public abstract LinkedHashMap<String, LinkedHashMap<Object,Long>> getFaceFieldsValues(List<String> fields);

    //Suggestions
    ///Suggestion fields
    public abstract LinkedHashMap<String, Long> getTopSuggestionFields();
    ///Suggestion values per field
    public abstract LinkedHashMap<String, List<LinkedHashMap<String,Long>>> getSuggestionFieldsValues(List<String> fields);

    //Advanced search?
    ///Advanced search fields
    ///Advanced search values per field

    //Full Text
    ///Fulltext top queries
    public abstract LinkedHashMap<String, Long> getTopQueries();

    //Filtered fulltext top queries?
    public abstract LinkedHashMap<String, Long> getTopFilteredQueries(String regexFilter);

}
