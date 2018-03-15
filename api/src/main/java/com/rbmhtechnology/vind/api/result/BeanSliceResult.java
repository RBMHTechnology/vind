package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.api.query.division.Slice;

import java.util.List;

/**
 * Created by fonso on 31.03.17.
 */
public class BeanSliceResult<T> extends BeanSearchResult<T> {
    private final Slice slice;

    /**
     * DEPRECATED: use the signature providing the time the query took.{@link BeanSliceResult#BeanSliceResult(long, long, List, FulltextSearch, FacetResults, SearchServer, Class)}
     * Creates a new instance of {@link BeanPageResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results A list of results parsed to T.
     * @param searchQuery The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param c Annotated class to parse the results to.
     */
    @Deprecated
    public BeanSliceResult(long numOfResults, List<T> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, Class<T> c) {
        super(numOfResults, results, searchQuery, facetResults, server, c);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.slice)) {
            this.slice = (Slice) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as slice: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Creates a new instance of {@link BeanPageResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime the time the query took in the backend.
     * @param results A list of results parsed to T.
     * @param searchQuery The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param c Annotated class to parse the results to.
     */
    public BeanSliceResult(long numOfResults, long queryTime, List<T> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, Class<T> c) {
        super(numOfResults, queryTime, results, searchQuery, facetResults, server, c);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.slice)) {
            this.slice = (Slice) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as slice: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Gets the actual offeset details.
     * @return the offset details.
     */
    public int getOffset() {
        return this.slice.getOffset();
    }

    /**
     * Gets the number of results in the actual slice configuration.
     * @return a number of results.
     */
    public int getSliceSize() {
        return this.slice.getSliceSize();
    }
}
