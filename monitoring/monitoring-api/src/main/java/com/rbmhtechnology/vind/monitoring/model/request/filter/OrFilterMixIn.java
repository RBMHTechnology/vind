/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rbmhtechnology.vind.api.query.filter.Filter;

import java.util.Set;

/**
 * Created on 28.12.17.
 */
public abstract class OrFilterMixIn extends Filter.OrFilter {

    /**
     * Creates an instance of {@link com.rbmhtechnology.vind.api.query.filter.Filter.OrFilter OrFilter} of two given filters.
     *
     * @param a {@link Filter} to be one part of the OR query.
     * @param b {@link Filter} to be one part of the OR query.
     */
    public OrFilterMixIn(Filter a, Filter b) {
        super(a, b);
    }

    @JsonProperty("delegates")
    @Override
    public abstract Set<Filter> getChildren();

}
