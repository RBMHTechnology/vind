/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request.filter;

import com.fasterxml.jackson.annotation.*;
import com.rbmhtechnology.vind.api.query.filter.Filter;

/**
 * Created on 28.12.17.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"type"})
@JsonIgnoreProperties(ignoreUnknown = true, value = {
        "descriptor", "filterQuery" })
public abstract class FilterMixIn extends Filter {

    @JsonProperty("scope")
    @Override
    public abstract Scope getFilterScope();

}

