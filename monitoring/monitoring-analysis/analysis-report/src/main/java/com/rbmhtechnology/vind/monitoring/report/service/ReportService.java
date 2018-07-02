/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.service;

import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.Report;
import com.rbmhtechnology.vind.monitoring.report.configuration.ReportConfiguration;
import com.rbmhtechnology.vind.monitoring.report.preprocess.ReportPreprocessor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

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

    abstract ReportPreprocessor getPreprocessor();

    public void preprocessData() {
        if (Objects.nonNull(configuration.getSystemFilterFields())) {
            getPreprocessor().addSystemFilterField(configuration.getSystemFilterFields());
        }
        getPreprocessor().preprocess();
    }

    public Report generateReport() {

        this.preprocessData();

        final List<String> topFaceFieldNames = this.getTopFacetFields();

        final LinkedHashMap<String, JsonObject> topFaceFields = this.prepareScopeFilterResults(topFaceFieldNames, "facet");
        final ArrayList<String> facetFields = new ArrayList<>(topFaceFields.keySet());

        final LinkedHashMap<String, JsonObject> topSuggestionFields = this.getTopSuggestionFields();
        final ArrayList<String> suggestFields = new ArrayList<>(topSuggestionFields.keySet());

        final LinkedHashMap<String, JsonObject> topFilterFields = this.getTopFilterFields();
        final ArrayList<String> filterFields = new ArrayList<>(topFilterFields.keySet());

        return new Report(configuration)
                .setFrom(this.getFrom())
                .setTo(this.getTo())
                .setTopDays(this.getTopDays())
                .setRequests(this.getTotalRequests())
                .setTopSuggestionFields(topSuggestionFields)
                .setSuggestionFieldsValues(this.getSuggestionFieldsValues(suggestFields))
                .setFacetFieldsValues(this.getFacetFieldsValues(facetFields))
                .setTopFacetFields(topFaceFields)
                .setTopQueries(this.getTopQueries())
                .setTopUsers(this.getTopUsers())
                .setTopFilterFields(topFilterFields)
                .setFilterFieldsValues(this.getFilterFieldsValues(filterFields));
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

    public abstract LinkedHashMap<String, JsonObject> prepareScopeFilterResults(List<String> fields, String scope);

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
    public abstract List<String> getTopFacetFields();

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
    public abstract LinkedHashMap<String, Long> getTopFilteredQueries();

    public abstract LinkedHashMap<String, LinkedHashMap<Object, Long>> getFilterFieldsValues(List<String> fields);
}
