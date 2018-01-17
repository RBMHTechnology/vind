/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.model.request;

import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.suggestion.DescriptorSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.ExecutableSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.StringSuggestionSearch;
import com.rbmhtechnology.vind.api.query.suggestion.SuggestionSearch;

import java.util.Collection;
import java.util.Map;
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
    private String solrQuery;
    private String source;

    public SuggestionRequest(ExecutableSuggestionSearch search, String source) {
        if(StringSuggestionSearch.class.isAssignableFrom(search.getClass())) {
            creatSuggestionRequest((StringSuggestionSearch)search,source);
        } else {
            creatSuggestionRequest((DescriptorSuggestionSearch)search,source);
        }
    }

    public void creatSuggestionRequest(StringSuggestionSearch search, String source) {
        this.search = search;
        this.query = this.search.getInput();
        this.suggestionFields = search.getSuggestionFields();
        if (Objects.nonNull(this.search.getFilter())) {
            this.filter = this.search.getFilter();
        }
        this.source = source;
    }

    public void creatSuggestionRequest(DescriptorSuggestionSearch search, String source) {
        this.search = search;
        this.query = this.search.getInput();
        this.suggestionFields = search.getSuggestionFields().stream().map( f -> f.getName()).collect(Collectors.toList());
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

    @Override
    public Map<String, Facet> getFacets() {
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
