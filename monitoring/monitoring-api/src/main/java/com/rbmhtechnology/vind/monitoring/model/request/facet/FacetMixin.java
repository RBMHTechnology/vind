/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request.facet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.rbmhtechnology.vind.api.query.facet.Facet;

/**
 * Created on 29.12.17.
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"type"})
@JsonIgnoreProperties(ignoreUnknown = true, value = {"fieldDescriptor","descriptor", "log","tagedPivots","fieldName" })

public abstract class FacetMixin extends Facet {

    @JsonProperty("field")
    @Override
    public abstract String getName();

}
