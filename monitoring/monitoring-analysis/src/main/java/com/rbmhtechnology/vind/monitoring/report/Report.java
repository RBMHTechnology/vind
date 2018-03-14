/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report;

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

    private String dateFormat = "dd/MM/yyyy VV";
    private String longDateFormat = "EEEE, MMMM dd, yyyy hh:mm a - VV";
    private ZoneId zoneId = ZoneOffset.UTC;

    private final ReportConfiguration configuration = new ReportConfiguration();

    private ZonedDateTime today = ZonedDateTime.now();
    private String applicationName;
    private ZonedDateTime from;
    private ZonedDateTime to;
    private long requests;
    private  LinkedHashMap<ZonedDateTime, Long> topDays = new LinkedHashMap<>();
    private  LinkedHashMap<String, Long> topUsers = new LinkedHashMap<>();
    private  LinkedHashMap<String, Long> topFacetFields = new LinkedHashMap<>();
    private  LinkedHashMap<String, LinkedHashMap<Object,Long>> facetFieldsValues = new LinkedHashMap<>();
    private  LinkedHashMap<String, Long> topSuggestionFields = new LinkedHashMap<>();
    private  LinkedHashMap<String, LinkedHashMap<Object,Long>> suggestionFieldsValues = new LinkedHashMap<>();
    private  LinkedHashMap<String, Long> topQueries = new LinkedHashMap<>();
    private  LinkedHashMap<String, Long> topFilteredQueries = new LinkedHashMap<>();


    public ZonedDateTime getToday() {
        return today;
    }

    public String getPrettyToday() {
        return DateTimeFormatter.ofPattern(longDateFormat).format(today.withZoneSameInstant(zoneId));

    }

    public String getApplicationName() {
        return applicationName;
    }

    public Report setApplicationName(String applicationName) {
        this.applicationName = applicationName;
        return this;
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
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
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

    public LinkedHashMap<String, Long> getTopFacetFields() {
        return topFacetFields;
    }

    public Report setTopFacetFields(LinkedHashMap<String, Long> topFaceFields) {
        this.topFacetFields = topFaceFields;
        return this;
    }

    public LinkedHashMap<String, LinkedHashMap<Object,Long>> getFacetFieldsValues() {
        return facetFieldsValues;
    }

    public Report setFacetFieldsValues(LinkedHashMap<String, LinkedHashMap<Object,Long>> faceFieldsValues) {
        this.facetFieldsValues = faceFieldsValues;
        return this;
    }

    public LinkedHashMap<String, Long> getTopSuggestionFields() {
        return topSuggestionFields;
    }

    public Report setTopSuggestionFields(LinkedHashMap<String, Long> topSuggestionFields) {
        this.topSuggestionFields = topSuggestionFields;
        return this;
    }

    public LinkedHashMap<String, LinkedHashMap<Object,Long>> getSuggestionFieldsValues() {
        return suggestionFieldsValues;
    }

    public Report setSuggestionFieldsValues(LinkedHashMap<String, LinkedHashMap<Object,Long>> suggestionFieldsValues) {
        this.suggestionFieldsValues = suggestionFieldsValues;
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
