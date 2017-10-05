/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.model.request;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.suggestion.SuggestionSearch;

import java.util.Collection;
import java.util.Objects;

/**
 * Created on 02.10.17.
 */
public class SuggestionRequest implements SearchRequest {

    private SuggestionSearch search;
    private String query;
    private Collection<String> suggestionFields;
    private Filter filter;
    private String solrQuery;
    private String source;

    public SuggestionRequest(SuggestionSearch search, String source) {
        this.search = search;
        this.query = this.search.getInput();
        this.suggestionFields = this.search.getSuggestionStringFields();
        if (Objects.nonNull(this.search.getFilter())) {
            this.filter = this.search.getFilter();
        }
        this.source = source;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    public Collection<String> getSuggestionFields() {
        return suggestionFields;
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
