/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request.facet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Interval;

/**
 * Created on 29.12.17.
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class DateRangeMixin extends Facet.DateRangeFacet {

    @JsonIgnore
    public abstract Object getStart();

    @JsonIgnore
    public abstract Object getEnd();

    @JsonProperty("start")
    public abstract long getTimeStampStart();

    @JsonProperty("end")
    public abstract long getTimeStampEnd();

}
