package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.model.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * This class stores the search result documents as instances of {@link Document}.
 */
public abstract class SearchResult {

    protected Logger log = LoggerFactory.getLogger(getClass());

    protected final long numOfResults;
    protected final List<Document> results;
    protected final FulltextSearch query;
    protected final FacetResults facetResults;
    protected final DocumentFactory factory;
    protected final SearchServer server;
    protected final Long queryTime;
    private Long elapsedTime;

    /**
     * DEPRECATED: use the signature providing the time the query took.{@link SearchResult#SearchResult(long, long, List, FulltextSearch, FacetResults, SearchServer, DocumentFactory)}
     * Creates a new instance of {@link SearchResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results A list of results parsed to Document.
     * @param searchQuery The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param docFactory document factory holding the schema configuration of documents to parse the results to.
     */
    @Deprecated
    public SearchResult(long numOfResults, List<Document> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, DocumentFactory docFactory) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = searchQuery;
        this.facetResults = facetResults;
        this.factory = docFactory;
        this.server = server;
        this.queryTime = null;
    }

    /**
     * Creates a new instance of {@link SearchResult}.
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime the time the query took in the backend.
     * @param results A list of results parsed to Document.
     * @param searchQuery The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param docFactory document factory holding the schema configuration of documents to parse the results to.
     */
    public SearchResult(long numOfResults, long queryTime, List<Document> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, DocumentFactory docFactory) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = searchQuery;
        this.facetResults = facetResults;
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
     * @return A list of results parsed as T
     */
    public List<Document> getResults() {
        return Collections.unmodifiableList(results);
    }


    public SearchResult print() {
        log.info(this.toString());
        return this;
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
     * Static method to get a {@link BeanSearchResult} from a {@link SearchResult}.
     * @param searchResult The search results to be parsed as P class specific results.
     * @param clazz Annotated class type to parse the results to.
     * @param <P> expected pojo Class.
     * @return a BeanSearchResult of type P.
     */
    public abstract  <P> BeanSearchResult<P> toPojoResult(SearchResult searchResult, Class<P> clazz) ;

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
    public SearchResult setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }
}
