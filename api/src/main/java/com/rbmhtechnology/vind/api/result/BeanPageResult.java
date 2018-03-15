package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;

import java.util.List;

/**
 * Created by fonso on 31.03.17.
 */
public class BeanPageResult<T> extends BeanSearchResult<T> {

    private Page page;

    /**
     * DEPRECATED: use the signature providing the time the query took.{@link BeanPageResult#BeanPageResult(long, long, List, FulltextSearch, FacetResults, SearchServer, Class)}
     * Creates a new instance of {@link BeanPageResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results A list of results parsed to T.
     * @param query The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param c Annotated class to parse the results to.
     */

    @Deprecated
    public BeanPageResult(long numOfResults, List<T> results, FulltextSearch query, FacetResults facetResults, SearchServer server, Class<T> c) {
        super(numOfResults, results, query, facetResults, server, c);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.page)) {
            this.page = (Page) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as page: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Creates a new instance of {@link BeanPageResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime the time the query took in the backend.
     * @param results A list of results parsed to T.
     * @param query The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server A search server implementation.
     * @param c Annotated class to parse the results to.
     */
    public BeanPageResult(long numOfResults, long queryTime ,List<T> results, FulltextSearch query, FacetResults facetResults, SearchServer server, Class<T> c) {
        super(numOfResults, queryTime, results, query, facetResults, server, c);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.page)) {
            this.page = (Page) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as page: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Gets the next page of results.
     * @return Instance of BeanSearchResult containing the next page results.
     * @throws {@link SearchServerException} When something goes wrong with the search execution.
     */
    public BeanSearchResult<T> nextPage() {
        try{
            return server.execute(query.copy().page(this.page.next()), annotatedClass);
        } catch (SearchServerException  e) {
            log.error("Unable to retrieve from search server next result", e);
            throw e;
        }
    }

    /**
     * Gets the previous page of results.
     * @return Instance of {@link BeanSearchResult} containing the previous page results.
     * @throws {@link SearchServerException} When something goes wrong with the search execution.
     */
    public BeanSearchResult<?> previousPage()  {
        try{
            return server.execute(query.copy().page(this.page.previous()), annotatedClass);
        } catch (SearchServerException  e) {
            log.error("Unable to retrieve from search server previous result", e);
            throw e;
        }
    }

    /**
     * Checks whether this result has a next page or not.
     * @return true if there is a next page.
     */
    public boolean hasNextPage() {
        return this.page.getPage() < getNumberOfPages();
    }

    /**
     * Checks whether this result has a previous page or not.
     * @return true if there is a previous page.
     */
    public boolean hasPreviousPage() {
        return this.page.getPage() > 1;
    }

    /**
     * Gets the actual page details.
     * @return the page details.
     */
    public int getPage() {
        return this.page.getPage();
    }

    /**
     * Gets the number of results in the actual page configuration.
     * @return a number of results.
     */
    public int getPagesize() {
        return this.page.getPagesize();
    }

    /**
     * Gets the number of pages for the query and with the actual page configuration.
     * @return a number of pages.
     */
    public int getNumberOfPages() {
        return (int) Math.ceil(numOfResults/((double)this.page.getPagesize()));
    }
}
