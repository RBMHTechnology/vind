/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.model.request;

import com.rbmhtechnology.vind.api.query.filter.Filter;

/**
 * Created on 02.10.17.
 */
public interface SearchRequest {

    String getQuery();
    Filter getFilter();
    String getSolrQuery();
    String getSource();

}
