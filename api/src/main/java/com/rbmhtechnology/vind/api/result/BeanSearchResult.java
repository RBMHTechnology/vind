package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * This class stores the search result documents as instances of the annotated class T.
 */
public class BeanSearchResult<T> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected final long numOfResults;
    protected final List<T> results;
    protected final FulltextSearch query;
    protected final FacetResults facetResults;
    protected final Class<T> annotatedClass;
    protected final SearchServer server;
    protected final Long queryTime;
    private Long elapsedTime;

    /**
     * DEPRECATED: use the signature providing the time the query took.{@link BeanSearchResult#BeanSearchResult(long, long, List, FulltextSearch, FacetResults, SearchServer, Class)}
     * Creates a new instance of {@link BeanSearchResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results A list of results parsed to T.
     * @param searchQuery The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param c Annotated class to parse the results to.
     */
    @Deprecated
    public BeanSearchResult(long numOfResults, List<T> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, Class<T> c) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = searchQuery;
        this.facetResults = facetResults;
        this.server = server;
        this.annotatedClass = c;
        this.queryTime = null;
    }

    /**
     * Creates a new instance of {@link BeanSearchResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime the time the query took in the backend.
     * @param results A list of results parsed to T.
     * @param searchQuery The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param c Annotated class to parse the results to.
     */
    public BeanSearchResult(long numOfResults, long queryTime, List<T> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, Class<T> c) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = searchQuery;
        this.facetResults = facetResults;
        this.server = server;
        this.annotatedClass = c;
        this.queryTime = queryTime;
    }

    /**
     * Gets the number of results stored.
     * @return Number of results.
     */
    public long getNumOfResults() {
        return numOfResults;
    }

    /**
     * Gets the list of results stored.
     * @return A list of results parsed as T
     */
    public List<? extends T> getResults() {
        return Collections.unmodifiableList(results);
    }


    @Override
    public String toString() {
        return "SearchResult{" +
                "numOfResults=" + numOfResults +
                ", results=" + results +
                '}';
    }

    /**
     * Gets the query faceted results.
     * @return {@link FacetResults} onject containing the different facets.
     */
    public FacetResults getFacetResults() {
        return facetResults;
    }


    /**
     * Gets the time the query took in the backend to be performed.
     * @return a number of milliseconds.
     */
    public Long getQueryTime() {
        return queryTime;
    }

    /**
     * Gets the time the query took in the backend to be performed plus the time it takes read from disk and to build the result.
     * @return a number of milliseconds.
     */
    public Long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * Sets the time the query took in the backend to be performed plus the time it takes read from disk and to build the result.
     * @return this instance of {@link SuggestionResult} with the modified elapsed time.
     */
    public BeanSearchResult<T> setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }
}
