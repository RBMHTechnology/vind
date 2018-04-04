/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;

/**
 * Created on 04.04.18.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)

public class DateMathExpressionMixIn extends DateMathExpression {
}
