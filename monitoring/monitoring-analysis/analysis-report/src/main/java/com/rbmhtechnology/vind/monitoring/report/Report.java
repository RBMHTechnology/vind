/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

import com.google.gson.JsonObject;
import com.rbmhtechnology.vind.monitoring.report.configuration.ReportConfiguration;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created on 01.03.18.
 */
public class Report {

    private ReportConfiguration configuration;

    private String dateFormat = "dd/MM/yyyy VV";
    private String longDateFormat = "EEEE, MMMM dd, yyyy hh:mm a";
    private ZoneId zoneId = ZoneOffset.UTC;
    private String imageUrl = null;

    private ZonedDateTime today = ZonedDateTime.now();
    private ZonedDateTime from;
    private ZonedDateTime to;
    private long requests;
    private LinkedHashMap<ZonedDateTime, Long> topDays = new LinkedHashMap<>();
    private LinkedHashMap<String, Long> topUsers = new LinkedHashMap<>();
    private LinkedHashMap<String, JsonObject> topFacetFields = new LinkedHashMap<>();
    private LinkedHashMap<String, LinkedHashMap<Object,Long>> facetFieldsValues = new LinkedHashMap<>();
    private LinkedHashMap<String, JsonObject> topSuggestionFields = new LinkedHashMap<>();
    private LinkedHashMap<String, LinkedHashMap<Object,Long>> suggestionFieldsValues = new LinkedHashMap<>();
    private LinkedHashMap<String, JsonObject> topFilterFields = new LinkedHashMap<>();
    private LinkedHashMap<String, LinkedHashMap<Object,Long>> filterFieldsValues = new LinkedHashMap<>();
    private LinkedHashMap<String, Long> topQueries = new LinkedHashMap<>();
    private LinkedHashMap<String, Long> topFilteredQueries = new LinkedHashMap<>();


    public Report(ReportConfiguration configuration) {
        this.configuration = configuration;
    }

    public ZonedDateTime getToday() {
        return today;
    }

    public String getPrettyToday() {
        return DateTimeFormatter.ofPattern(longDateFormat).format(today.withZoneSameInstant(zoneId));
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Report setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        return this;
    }

    public String getApplicationName() {
        return configuration.getApplicationId();
    }

    public ZonedDateTime getFrom() {
        return from;
    }

    public String getPrettyFrom() {
        return DateTimeFormatter.ofPattern(this.dateFormat).format(from.withZoneSameInstant(zoneId));
    }

    public Report setFrom(ZonedDateTime from) {
        this.from = from;
        return this;
    }

    public ZonedDateTime getTo() {
        return to;
    }

    public String getPrettyTo() {
        return DateTimeFormatter.ofPattern(this.dateFormat).format(to.withZoneSameInstant(zoneId));
    }

    public Report setTo(ZonedDateTime to) {
        this.to = to;
        return this;
    }

    public long getRequests() {
        return requests;
    }

    public Report setRequests(long requests) {
        this.requests = requests;
        return this;
    }

    public LinkedHashMap<ZonedDateTime, Long> getTopDays() {
        return topDays;
    }

    public LinkedHashMap<String, Long> getFormattedTopDays() {
        final LinkedHashMap<String, Long> formattedTopDays = new LinkedHashMap<>();
        topDays.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach( entry -> formattedTopDays.put(DateTimeFormatter.ofPattern(this.dateFormat).format(entry.getKey().withZoneSameInstant(zoneId)),entry.getValue()));
        return formattedTopDays;
    }

    public Report setTopDays(LinkedHashMap<ZonedDateTime, Long> topDays) {
        this.topDays = topDays;
        return this;
    }

    public LinkedHashMap<String, Long> getTopUsers() {
        return topUsers;
    }

    public Report setTopUsers(LinkedHashMap<String, Long> topUsers) {
        this.topUsers = topUsers;
        return this;
    }

    public LinkedHashMap<String, JsonObject> getTopFacetFields() {
        return topFacetFields;
    }

    public Report setTopFacetFields(LinkedHashMap<String, JsonObject> topFaceFields) {
        this.topFacetFields = topFaceFields;
        return this;
    }

    public long getCountOfUnusedFacetFields() {
        return topFacetFields.keySet().stream().filter(k -> {return topFacetFields.get(k).get("total").getAsLong() == 0;}).count();
    }

    public LinkedHashMap<String, LinkedHashMap<Object,Long>> getFacetFieldsValues() {
        return facetFieldsValues;
    }

    public Report setFacetFieldsValues(LinkedHashMap<String, LinkedHashMap<Object,Long>> faceFieldsValues) {
        this.facetFieldsValues = faceFieldsValues;
        return this;
    }

    public LinkedHashMap<String, JsonObject> getTopSuggestionFields() {
        return topSuggestionFields;
    }

    public Report setTopSuggestionFields(LinkedHashMap<String, JsonObject> topSuggestionFields) {
        this.topSuggestionFields = topSuggestionFields;
        return this;
    }

    public long getCountOfUnusedSuggestionFields() {
        return topSuggestionFields.keySet().stream().filter(k -> {return topSuggestionFields.get(k).get("total").getAsLong() == 0;}).count();
    }

    public LinkedHashMap<String, LinkedHashMap<Object,Long>> getSuggestionFieldsValues() {
        return suggestionFieldsValues;
    }

    public Report setSuggestionFieldsValues(LinkedHashMap<String, LinkedHashMap<Object,Long>> suggestionFieldsValues) {
        this.suggestionFieldsValues = suggestionFieldsValues;
        return this;
    }

    public LinkedHashMap<String, JsonObject> getTopFilterFields() {
        return topFilterFields;
    }

    public Report setTopFilterFields(LinkedHashMap<String, JsonObject> topFilterFields) {
        this.topFilterFields = topFilterFields;
        return this;
    }

    public long getCountOfUnusedFilterFields() {
        return topFilterFields.keySet().stream().filter(k -> {return topFilterFields.get(k).get("total").getAsLong() == 0;}).count();
    }

    public LinkedHashMap<String, LinkedHashMap<Object, Long>> getFilterFieldsValues() {
        return filterFieldsValues;
    }

    public Report setFilterFieldsValues(LinkedHashMap<String, LinkedHashMap<Object, Long>> filterFieldsValues) {
        this.filterFieldsValues = filterFieldsValues;
        return this;
    }

    public LinkedHashMap<String, Long> getTopQueries() {
        return topQueries;
    }

    public Long getTotalTopQueries() {
        return topQueries.values().stream().reduce(0l, Long::sum);
    }

    public Report setTopQueries(LinkedHashMap<String, Long> topQueries) {
        this.topQueries = topQueries;
        return this;
    }

    public LinkedHashMap<String, Long> getTopFilteredQueries() {
        return topFilteredQueries;
    }

    public Report setTopFilteredQueries(LinkedHashMap<String, Long> topFilteredQueries) {
        this.topFilteredQueries = topFilteredQueries;
        return this;
    }

    public ReportConfiguration getConfiguration() {
        return configuration;
    }
}
