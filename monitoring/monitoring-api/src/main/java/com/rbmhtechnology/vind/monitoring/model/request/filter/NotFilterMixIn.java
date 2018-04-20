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
public abstract class NotFilterMixIn extends Filter.NotFilter {


    /**
     * Creates an instance of {@link com.rbmhtechnology.vind.api.query.filter.Filter.NotFilter NotFilter} of a given filter.
     *
     * @param a {@link Filter} to be one part of the NOT query.
     */
    public NotFilterMixIn(Filter a) {
        super(a);
    }

    @JsonProperty("delegates")
    @Override
    public abstract Filter getDelegate();

}
