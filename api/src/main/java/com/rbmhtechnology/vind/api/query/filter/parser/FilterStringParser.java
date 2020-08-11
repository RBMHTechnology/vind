package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;

import java.io.IOException;

public interface FilterStringParser {

    Filter parse(String luceneQuery, DocumentFactory factory) throws IOException;

}
