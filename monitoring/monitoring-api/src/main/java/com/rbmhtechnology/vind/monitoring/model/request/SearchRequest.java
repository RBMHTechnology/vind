/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request;

import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.filter.Filter;

import java.util.List;
import java.util.Map;

/**
 * Created on 02.10.17.
 */
public interface SearchRequest {

    String getQuery();
    Filter getFilter();

    List<Facet> getFacets();

    String getRawQuery();

}
