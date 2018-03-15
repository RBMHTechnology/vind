/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;

import java.util.*;

/**
 * Created on 02.10.17.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FullTextRequest implements SearchRequest{

    private FulltextSearch search;
    private String query;
    private com.rbmhtechnology.vind.api.query.filter.Filter filter;
    private List<Facet> facets;
    private String rawQuery;

    public FullTextRequest() {
    }

    public FullTextRequest(FulltextSearch search, String solrQuery) {
        this.search = search;
        this.query = Objects.nonNull(this.search.getSearchString()) ? this.search.getSearchString() : "*";
        if (Objects.nonNull(this.search.getFilter())) {
            this.filter = this.search.getFilter();//com.rbmhtechnology.vind.report.model.request.filter.Filter.logFilter(this.search.getFilter()) ;
        }
        this.facets = new ArrayList<>(this.search.getFacets().values());
        this.rawQuery = solrQuery;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    @Override
    public List<Facet> getFacets() {
        return facets;
    }

    @Override
    public String getRawQuery() {
        return rawQuery;
    }

}
