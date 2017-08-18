package com.rbmhtechnology.vind.api.result.facet;

import com.rbmhtechnology.vind.api.query.filter.Filter;

/**
 * Class to store the query facet response.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 27.06.16.
 */
public class QueryFacetResult<T> implements FacetResult<T> {

    private Filter filter;
    private int count;

    /**
     * Creates a new instance of{@link  QueryFacetResult}.
     * @param filter {@link Filter} query to apply to the facet search.
     * @param count  number of documents being by the query.
     */
    public QueryFacetResult(Filter filter, int count) {

        this.filter = filter;
        this.count = count;
    }

    /**
     * Get the configured {@link Filter}.
     * @return {@link Filter}.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Grts the number of documents filtered by the query.
     * @return  number of documents filtered.
     */
    public int getCount() {
        return count;
    }
}
