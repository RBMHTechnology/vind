/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.analysis;

import com.rbmhtechnology.vind.monitoring.logger.entry.FullTextEntry;
import com.rbmhtechnology.vind.monitoring.logger.entry.MonitoringEntry;
import com.rbmhtechnology.vind.monitoring.logger.entry.SuggestionEntry;
import com.rbmhtechnology.vind.monitoring.model.request.SuggestionRequest;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created on 08.01.18.
 */
public class BasicReportAnalyzer extends LogAnalyzer{


    @Override
    public String analyze(MonitoringEntry log) {
        return null;
    }

    @Override
    public String analyze(Collection <MonitoringEntry> log) {

        //General Info
        ///Name
        ///App
        ///Workspace
        ///Module
        ///from Date
        ///to Date

        //Total number of requests
        final long requests = log.size();

        //Top Day
        final Map<Integer, Long> topDays = log.stream().map(logEntry -> logEntry.getTimeStamp().getDayOfYear())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        //Top users
        final Map<String, Long> topSessions = log.stream().map(logEntry -> logEntry.getSession().getSessionId())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        //Facets
        ///Facet Fields

        ///Facet Values per field?

        //Suggestions
        ///Suggestion fields
        final Map<String, Long> suggestionFields = log.stream()
                .filter(logEntry -> logEntry.getType().name().equals(MonitoringEntry.EntryType.suggestion))
                .map(logEntry -> ((SuggestionRequest) ((SuggestionEntry)logEntry).getRequest()).getSuggestionFields())
                .flatMap(fields -> fields.stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        ///Suggestion values per field

        //Advanced search?
        ///Advanced search fields
        ///Advanced search values per field

        //Full Text
        ///Fulltext top queries
        final Map<String, Long> fulltextQueries = log.stream()
                .filter(logEntry -> logEntry.getType().name().equals(MonitoringEntry.EntryType.fulltext))
                .map(logEntry -> ((FullTextEntry)logEntry).getRequest().getQuery())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        //Filtered fulltext top queries?

        return null;
    }
}
