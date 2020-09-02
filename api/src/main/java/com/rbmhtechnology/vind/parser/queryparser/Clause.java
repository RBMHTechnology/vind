package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;

public interface Clause {
    Filter toVindFilter(DocumentFactory factory);
}
