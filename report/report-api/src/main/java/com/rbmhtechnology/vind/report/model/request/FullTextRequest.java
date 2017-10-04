/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.model.request;

import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Created on 02.10.17.
 */
public class FullTextRequest implements SearchRequest{

    private FulltextSearch search;
    private String query;
    private Filter filter;
    private Map<String, Facet> facets;
    private String solrQuery;
    private String source;

    public FullTextRequest(FulltextSearch search, String source) {
        this.search = search;
        this.query = Objects.nonNull(this.search.getSearchString()) ? this.search.getSearchString() : "*";
        if (Objects.nonNull(this.search.getFilter())) {
            this.filter = this.search.getFilter();
        }
        this.facets = this.search.getFacets();
        this.source = source;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    public Map<String, Facet> getFacets() {
        return facets;
    }

    @Override
    public String getSolrQuery() {
        return solrQuery;
    }

    @Override
    public String getSource() {
        return this.source;
    }

}
