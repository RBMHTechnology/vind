/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.model.request.filter;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.ArrayList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Created on 31.10.17.
 */
@JsonInclude(Include.NON_EMPTY)
@JsonPropertyOrder({"type"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class Filter {


    private final List<Filter> delegates;
    private String type;
    private String filterQuery;


    private Filter(com.rbmhtechnology.vind.api.query.filter.Filter.AndFilter filter) {
        type = filter.getType();
        delegates = new ArrayList<>();

        filter.getChildren().stream()
                .forEach(f -> delegates.addAll(logFilter(f)));

    }

    private Filter(com.rbmhtechnology.vind.api.query.filter.Filter.OrFilter filter) {
        type = filter.getType();
        delegates = new ArrayList<>();

        filter.getChildren().stream()
                .forEach(f -> delegates.addAll(logFilter(f)));

    }

    private Filter(com.rbmhtechnology.vind.api.query.filter.Filter.NotFilter filter) {
        type = filter.getType();
        delegates = logFilter(filter.getDelegate());

    }

    private Filter(com.rbmhtechnology.vind.api.query.filter.Filter.DescriptorFilter filter) {
        type = filter.getType();
        delegates = new ArrayList<>();
        filterQuery = filter.toString();
    }

    private Filter(com.rbmhtechnology.vind.api.query.filter.Filter.PrefixFilter filter) {
        type = filter.getType();
        delegates = new ArrayList<>();
        filterQuery = filter.toString();
    }

    public static List<Filter> logFilter(com.rbmhtechnology.vind.api.query.filter.Filter filter) {
        List<Filter> f = new ArrayList<>();
        if (com.rbmhtechnology.vind.api.query.filter.Filter.AndFilter.class.isAssignableFrom(filter.getClass())){
            f.add(new Filter((com.rbmhtechnology.vind.api.query.filter.Filter.AndFilter)filter));
        }
        if (com.rbmhtechnology.vind.api.query.filter.Filter.OrFilter.class.isAssignableFrom(filter.getClass())){
            f.add(new Filter((com.rbmhtechnology.vind.api.query.filter.Filter.OrFilter)filter));
        }
        if (com.rbmhtechnology.vind.api.query.filter.Filter.NotFilter.class.isAssignableFrom(filter.getClass())){
            f.add(new Filter((com.rbmhtechnology.vind.api.query.filter.Filter.NotFilter)filter));
        }
        if (com.rbmhtechnology.vind.api.query.filter.Filter.DescriptorFilter.class.isAssignableFrom(filter.getClass())){
            f.add(new Filter((com.rbmhtechnology.vind.api.query.filter.Filter.DescriptorFilter)filter));
        }
        if (com.rbmhtechnology.vind.api.query.filter.Filter.PrefixFilter.class.isAssignableFrom(filter.getClass())){
            f.add(new Filter((com.rbmhtechnology.vind.api.query.filter.Filter.PrefixFilter)filter));
        }

        //FIXME : implement the other filter types
        return f;
    }

    public List<Filter> getDelegates() {
        return delegates;
    }

    public String getType() {
        return type;
    }

    public String getFilterQuery() {
        return filterQuery;
    }
}
