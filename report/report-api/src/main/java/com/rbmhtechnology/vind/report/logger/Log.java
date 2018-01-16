package com.rbmhtechnology.vind.report.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.api.query.suggestion.SuggestionSearch;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.report.logger.entry.FullTextEntry;
import com.rbmhtechnology.vind.report.model.application.Application;
import com.rbmhtechnology.vind.report.model.interaction.Interaction;
import com.rbmhtechnology.vind.report.model.request.*;
import com.rbmhtechnology.vind.report.model.request.facet.FacetMixin;
import com.rbmhtechnology.vind.report.model.request.filter.AndFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.FilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.NotFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.filter.OrFilterMixIn;
import com.rbmhtechnology.vind.report.model.request.sort.SortMixIn;
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
    private ObjectMapper mapper = new ObjectMapper()
            .addMixIn(Filter.class, FilterMixIn.class)
            .addMixIn(Filter.AndFilter.class, AndFilterMixIn.class)
            .addMixIn(Filter.OrFilter.class, OrFilterMixIn.class)
            .addMixIn(Filter.NotFilter.class, NotFilterMixIn.class)
            .addMixIn(Facet.class, FacetMixin.class)
            .addMixIn(Sort.class, SortMixIn.class)
            ;

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
            throw new RuntimeException(e);
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
