package com.rbmhtechnology.vind.report.logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.result.SearchResult;
import com.rbmhtechnology.vind.report.application.Application;
import com.rbmhtechnology.vind.report.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public class Log {

    private ObjectMapper mapper = new ObjectMapper();

    private Map<String,Object> values;

    public Log(Application application, FulltextSearch search, String type, SearchResult result, ZonedDateTime start, Session session) {
        //TODO must map a specific format
        values = new HashMap<>();
        values.put("application", application);
        values.put("type","fulltext");
        values.put("returnType", type);
        values.put("query",search.getSearchString());
        if(search.hasFilter()) values.put("filter",search.getFilter().toString());
        if(search.hasFacet()) values.put("facet", search.getFacets().toString());
        values.put("sorting", search.getSorting().toString());
        values.put("session", session);
        if (search.getResultSet().getType().equals(ResultSubset.DivisionType.page)) {
            values.put("page", ((Page)search.getResultSet()).getPage());
        }
        if (search.getResultSet().getType().equals(ResultSubset.DivisionType.slice)) {
            values.put("offset", ((Slice)search.getResultSet()).getOffset());
        }
        values.put("numOfResults",result.getNumOfResults());
        values.put("time", start.toString());
        values.put("duration", start.until(ZonedDateTime.now(), ChronoUnit.MILLIS));
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
