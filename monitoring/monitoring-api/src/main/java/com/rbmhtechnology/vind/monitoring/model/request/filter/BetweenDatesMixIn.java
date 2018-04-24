/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;

/**
 * Created on 29.12.17.
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class BetweenDatesMixIn extends Filter {

    @JsonIgnore
    public abstract DateMathExpression getStart();

    @JsonIgnore
    public abstract DateMathExpression getEnd();

    @JsonProperty("start")
    public abstract long getTimeStampStart();

    @JsonProperty("end")
    public abstract long getTimeStampEnd();

}
