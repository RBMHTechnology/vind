/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request.sort;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.rbmhtechnology.vind.api.query.sort.Sort;

/**
 * Created on 16.01.18.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"type"})
@JsonIgnoreProperties(ignoreUnknown = true, value = {"descriptor"})

public abstract class SortMixIn extends Sort{

    @JsonProperty("type")
    @Override
    public abstract String getType();

    @JsonProperty("direction")
    @Override
    public abstract Direction getDirection();

}
