package com.rbmhtechnology.vind.report.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.suggestion.SuggestionSearch;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.report.logger.entry.FullTextEntry;
import com.rbmhtechnology.vind.report.model.application.Application;
import com.rbmhtechnology.vind.report.model.interaction.Interaction;
import com.rbmhtechnology.vind.report.model.request.FullTextRequest;
import com.rbmhtechnology.vind.report.model.request.SuggestionRequest;
import com.rbmhtechnology.vind.report.model.response.Response;
import com.rbmhtechnology.vind.report.model.session.Session;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public class Log {

    public static final String SOLR_DATE_TIME_FORMAT = "yyyy-MM-dd'T'hh:mm:ss'Z'";
    private ObjectMapper mapper = new ObjectMapper();

    private Map<String,Object> values;

    public Log(FullTextEntry logEntry) {
        values = new HashMap<>();
        values.put("application", logEntry.getApplication());
        values.put("session", logEntry.getSession());
        //values.put("module", module);
        values.put("timestamp", DateTimeFormatter.ofPattern(SOLR_DATE_TIME_FORMAT).format(logEntry.getTimeStamp().withZoneSameInstant(ZoneOffset.UTC)));
        values.put("type","fulltext");
        values.put("request",logEntry.getRequest());
        values.put("sorting", logEntry.getSorting());
        values.put("paging", logEntry.getPaging());
        values.put("response",logEntry.getResponse());
    }

    public Log(Application application, SuggestionSearch search, SuggestionResult result, ZonedDateTime start, ZonedDateTime end, Session session) {
        values = new HashMap<>();
        values.put("application", application);
        values.put("session", session);
        //values.put("module", module);
        values.put("timestamp", DateTimeFormatter.ofPattern(SOLR_DATE_TIME_FORMAT).format(start.withZoneSameInstant(ZoneOffset.UTC)));
        values.put("type","suggestion");
        values.put("request",new SuggestionRequest(search, "suggestion"));
        values.put("response",new Response(result.size(), result.getSuggestedFields().size() ,start.until(end, ChronoUnit.MILLIS)));
    }

    public Log(Application application, Interaction interaction, ZonedDateTime start, Session session) {
        values = new HashMap<>();
        values.put("application", application);
        values.put("session", session);
        //values.put("module", module);
        values.put("timestamp", DateTimeFormatter.ofPattern(SOLR_DATE_TIME_FORMAT).format(start.withZoneSameInstant(ZoneOffset.UTC)));
        values.put("type","interaction");
        values.put("request", interaction);
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(values);
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    public Map<String,Object> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
