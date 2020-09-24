package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.parser.queryparser.VindQueryParser;

@Deprecated
public class FilterLuceneParser implements FilterStringParser {

    /**
     * Deprecated, use instead the new {@link VindQueryParser#parse(String, DocumentFactory)}
     * @param luceneQuery Lucene string to be parsed as Filter
     * @param factory Document factory used to create the documents to search for.
     * @return A Vind {@link Filter} matching the lucene string
     */
    @Override
    @Deprecated
    public Filter parse(String luceneQuery, DocumentFactory factory) {
        return new VindQueryParser().parse(luceneQuery,factory).getFilter();
    }
}

