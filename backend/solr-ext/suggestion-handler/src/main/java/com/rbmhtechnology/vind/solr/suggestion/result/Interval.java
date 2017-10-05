package com.rbmhtechnology.vind.solr.suggestion.result;

import com.rbmhtechnology.vind.solr.suggestion.SuggestionRequestHandler;

import java.time.LocalDateTime;

/**
 * Represents a simple facet POJO
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
class Interval {

    int count;
    LocalDateTime start;
    LocalDateTime end;
   SuggestionResult facets ;

    public Interval(LocalDateTime start, LocalDateTime end, int limit,SuggestionRequestHandler.LimitType limitType ) {
        this.start = start;
        this.end = end;
        this.facets = new SuggesionResultSingle(limit, limitType);

    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public void setEnd(LocalDateTime end) {
        this.end = end;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public SuggestionResult getFacets() {
        return facets;
    }

    public boolean equals(Object o) {
        try {
            return ((Interval)o).start.isEqual(this.start) && ((Interval)o).end.isEqual(this.end);
        } catch (Exception e) {
          return false;
        }
    }
}