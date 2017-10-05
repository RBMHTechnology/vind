package com.rbmhtechnology.vind.solr.suggestion.jsonfacetmodel;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fonso on 9/1/16.
 */
public class Pivot {

    public facetType type;
    public String q;
    public String field;
    public int limit;
    public int mincount;
    public Map<String, Pivot> facet;

    public Pivot() {
        type = null;
        q = null;
        field = null;
        limit = -1;
        mincount = 1;
        facet = null;
    }

    public String getType() {
        return type.name();
    }

    public void setType(facetType type) {
        this.type = type;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getMincount() {
        return mincount;
    }

    public void setMincount(int mincount) {
        this.mincount = mincount;
    }

    public Map<String, Pivot> getFacet() {
        return facet;
    }

    public void setFacet(Map<String, Pivot> facet) {
        this.facet = facet;
    }
    public void addFacet(String name, Pivot facet) {
        if (this.facet==null) {
            this.facet = new HashMap<>();
        }
        this.facet.put(name,facet);
    }

    public enum facetType {
        terms,query
    }
}

