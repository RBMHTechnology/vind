package com.rbmhtechnology.vind.solr.suggestion.result;

import com.rbmhtechnology.vind.solr.suggestion.SuggestionRequestHandler;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class SuggesionResultInterval implements SuggestionResult {

    private int count = 0;
    private int limit = Integer.MAX_VALUE;
    private SuggestionRequestHandler.LimitType limitType;
    private HashMap<String,Interval> intervals = new HashMap<>();

    public SuggesionResultInterval(int limit, SuggestionRequestHandler.LimitType limitType) {
        this.limit = limit;
        this.limitType = limitType;
    }

    public Object write() {
        Map<String,Object> suggestions = new HashMap<>();

        HashMap<String,Object> suggestion_intervals = new HashMap<>();

        intervals.keySet().forEach( key -> suggestion_intervals.put(key, intervals.get(key).facets.write()));
        suggestions.put("suggestion_intervals", suggestion_intervals);
        return suggestions;
    }


    public void addFacet(String intervalName,String field, String value, int count, int position) {
        ((SuggesionResultSingle)intervals.get(intervalName).getFacets()).addFacet(field, value, count, position);
    }

    public void addInterval(String intervalName,LocalDateTime start, LocalDateTime end) {
        intervals.put(intervalName,new Interval(start,end,limit,limitType));
    }
    public int getCount() {
        return intervals.size();
    }

}
