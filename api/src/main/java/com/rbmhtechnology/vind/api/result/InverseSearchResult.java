package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearch;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.model.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * This class stores the search result documents as instances of {@link Document}.
 */
public abstract class InverseSearchResult {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected final long numOfResults;
    protected final List<InverseSearchQuery> results;
    protected final InverseSearch query;
    protected final DocumentFactory factory;
    protected final SearchServer server;
    protected final Long queryTime;
    private Long elapsedTime;

    /**
     * Creates a new instance of {@link InverseSearchResult}.
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime the time the query took in the backend.
     * @param results A list of results parsed to {@link InverseSearchQuery}.
     * @param searchQuery The original inverse search executed to retrieve this set of results.
     * @param server A search server implementation.
     * @param docFactory document factory holding the schema configuration of documents used to search.
     */
    public InverseSearchResult(long numOfResults, long queryTime, List<InverseSearchQuery> results, InverseSearch searchQuery, SearchServer server, DocumentFactory docFactory) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = searchQuery;
        this.factory = docFactory;
        this.server = server;
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
     * @return A list of {@link InverseSearchQuery} results
     */
    public List<InverseSearchQuery> getResults() {
        return Collections.unmodifiableList(results);
    }


    public InverseSearchResult print() {
        log.info(this.toString());
        return this;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "numOfResults=" + numOfResults + ", results=" + results + '}';
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
    public InverseSearchResult setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }
}
