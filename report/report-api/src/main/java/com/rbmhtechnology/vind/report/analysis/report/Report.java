/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.analysis.report;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.LinkedHashMap;

/**
 * Created on 01.03.18.
 */
public class Report {
  
    private ZonedDateTime from;
    private ZonedDateTime to;
    private long requests;
    private LinkedHashMap<Integer, ZonedDateTime> topDays;
    private  LinkedHashMap<String, Long> topUsers;
    private  LinkedHashMap<String, Long> topFaceFields;
    private  LinkedHashMap<String, List<Object>> faceFieldsValues;
    private  LinkedHashMap<String, Long> topSuggestionFields;
    private  LinkedHashMap<String, List<Object>> suggestionFieldsValues;
    private  LinkedHashMap<String, Long> topQueries;
    private  LinkedHashMap<String, Long> topFilteredQueries;

    public ZonedDateTime getFrom() {
        return from;
    }

    public Report setFrom(ZonedDateTime from) {
        this.from = from;
        return this;
    }

    public ZonedDateTime getTo() {
        return to;
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

    public LinkedHashMap<Integer, ZonedDateTime> getTopDays() {
        return topDays;
    }

    public Report setTopDays(LinkedHashMap<Integer, ZonedDateTime> topDays) {
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

    public LinkedHashMap<String, Long> getTopFaceFields() {
        return topFaceFields;
    }

    public Report setTopFaceFields(LinkedHashMap<String, Long> topFaceFields) {
        this.topFaceFields = topFaceFields;
        return this;
    }

    public LinkedHashMap<String, List<Object>> getFaceFieldsValues() {
        return faceFieldsValues;
    }

    public Report setFaceFieldsValues(LinkedHashMap<String, List<Object>> faceFieldsValues) {
        this.faceFieldsValues = faceFieldsValues;
        return this;
    }

    public LinkedHashMap<String, Long> getTopSuggestionFields() {
        return topSuggestionFields;
    }

    public Report setTopSuggestionFields(LinkedHashMap<String, Long> topSuggestionFields) {
        this.topSuggestionFields = topSuggestionFields;
        return this;
    }

    public LinkedHashMap<String, List<Object>> getSuggestionFieldsValues() {
        return suggestionFieldsValues;
    }

    public Report setSuggestionFieldsValues(LinkedHashMap<String, List<Object>> suggestionFieldsValues) {
        this.suggestionFieldsValues = suggestionFieldsValues;
        return this;
    }

    public LinkedHashMap<String, Long> getTopQueries() {
        return topQueries;
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
}
