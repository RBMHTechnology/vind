/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.configuration;

import java.util.*;

/**
 * Created on 08.03.18.
 */
public class ReportWriterConfiguration {

    private Map<String, String> generalFilters = new HashMap<>();
    private Map<String, HashMap<String,String>> facetFieldsExtension = new HashMap<>();
    private Map<String, HashMap<String,String>> suggestionFieldsExtension = new HashMap<>();
    private Map<String, HashMap<String,String>> fulltextQueryExtension = new HashMap<>();
    private List<String> queryFilters = new ArrayList<>();

    public Map<String, String> getGeneralFilters() {
        return generalFilters;
    }

    public ReportWriterConfiguration addGeneralFilter(String fieldName, String filterValue) {
        if (Objects.nonNull(filterValue) & Objects.nonNull(fieldName)) {
            this.generalFilters.put(fieldName,filterValue);
        }
        return this;
    }

    public Map<String, HashMap<String, String>> getFacetFieldsExtension() {
        return Collections.unmodifiableMap(facetFieldsExtension);
    }


    public ReportWriterConfiguration addFacetFieldExtension(String extensionName, HashMap<String, String> facetFieldsExtension) {
        if (Objects.nonNull(extensionName) ) {
            if (Objects.isNull(facetFieldsExtension)) {
                facetFieldsExtension = new HashMap<>();
            }
            this.facetFieldsExtension.put(extensionName, facetFieldsExtension);
        }
        return this;
    }

    public Map<String, HashMap<String, String>> getSuggestionFieldsExtension() {
        return suggestionFieldsExtension;
    }

    public ReportWriterConfiguration addSuggestionFieldExtension(String extensionName, HashMap<String, String> suggestionFieldsExtension) {
        if (Objects.nonNull(extensionName) ) {
            if (Objects.isNull(suggestionFieldsExtension)) {
                suggestionFieldsExtension = new HashMap<>();
            }
            this.suggestionFieldsExtension.put(extensionName, suggestionFieldsExtension);
        }
        return this;
    }

    public Map<String, HashMap<String, String>> getFulltextQueryExtension() {
        return fulltextQueryExtension;
    }

    public ReportWriterConfiguration addFulltextQueryExtension(String extensionName, HashMap<String, String> fulltextQueryExtension) {
        if (Objects.nonNull(extensionName) ) {
            if (Objects.isNull(fulltextQueryExtension)) {
                fulltextQueryExtension = new HashMap<>();
            }
            this.fulltextQueryExtension.put(extensionName, fulltextQueryExtension);
        }
        return this;
    }

    public List<String> getQueryFilters() {
        return queryFilters;
    }

    public ReportWriterConfiguration setQueryFilters(List<String> queryFilters) {
        this.queryFilters = queryFilters;
        return this;
    }

    public ReportWriterConfiguration setQueryFilters(String... queryFilters) {
        if (Objects.nonNull(queryFilters)) {
            this.queryFilters = Arrays.asList(queryFilters);
        }
        return this;
    }

    public ReportWriterConfiguration addQueryFilters(String... queryFilters) {
        if (Objects.nonNull(queryFilters)) {
            this.queryFilters.addAll(Arrays.asList(queryFilters));
        }
        return this;
    }
}
