/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request;

import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.suggestion.DescriptorSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.StringSuggestionSearch;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created on 02.10.17.
 */
public class SuggestionRequest implements SearchRequest {

    private ExecutableSuggestionSearch search;
    private String query;
    private Collection<String> suggestionFields;
    private Filter filter;
    private String rawQuery;

    public SuggestionRequest() {
    }

    public SuggestionRequest(ExecutableSuggestionSearch search, String rawQuery) {
        if(StringSuggestionSearch.class.isAssignableFrom(search.getClass())) {
            createSuggestionRequest((StringSuggestionSearch)search, rawQuery);
        } else {
            createSuggestionRequest((DescriptorSuggestionSearch)search, rawQuery);
        }
    }

    public void createSuggestionRequest(StringSuggestionSearch search, String rawQuery) {
        this.search = search;
        this.query = this.search.getInput();
        this.suggestionFields = search.getSuggestionFields();
        if (Objects.nonNull(this.search.getFilter())) {
            this.filter = this.search.getFilter();
        }
        this.rawQuery = rawQuery;
    }

    public void createSuggestionRequest(DescriptorSuggestionSearch search, String rawQuery) {
        this.search = search;
        this.query = this.search.getInput();
        this.suggestionFields = search.getSuggestionFields().stream().map( f -> f.getName()).collect(Collectors.toList());
        if (Objects.nonNull(this.search.getFilter())) {
            this.filter = this.search.getFilter();
        }
        this.rawQuery = rawQuery;
    }

    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public Filter getFilter() {
        return null;
    }

    @Override
    public List<Facet> getFacets() {
        return null;
    }

    public Collection<String> getSuggestionFields() {
        return suggestionFields;
    }

    @Override
    public String getRawQuery() {
        return rawQuery;
    }
}
