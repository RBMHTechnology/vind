package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.model.DocumentFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Alfonso Noriega Meneses on 31.03.17.
 */
public class PageResult extends SearchResult {

    private Page page;

    /**
     * DEPRECATED: use the signature providing the time the query took.{@link PageResult#PageResult(long, long, List, FulltextSearch, FacetResults, SearchServer, DocumentFactory)}
     * Creates a new instance of {@link PageResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results      A list of results parsed to Document.
     * @param searchQuery  The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server       A search server implementation.
     * @param docFactory   document factory holding the schema configuration of documents to parse the results to.
     */
    @Deprecated
    public PageResult(long numOfResults, List<Document> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, DocumentFactory docFactory) {
        super(numOfResults, results, searchQuery, facetResults, server, docFactory);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.page)) {
            this.page = (Page) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as page: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Creates a new instance of {@link PageResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime    the time the query took in the backend.
     * @param results      A list of results parsed to Document.
     * @param searchQuery  The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server       A search server implementation.
     * @param docFactory   document factory holding the schema configuration of documents to parse the results to.
     */
    public PageResult(long numOfResults, long queryTime, List<Document> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, DocumentFactory docFactory) {
        super(numOfResults, queryTime, results, searchQuery, facetResults, server, docFactory);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.page)) {
            this.page = (Page) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as page: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Gets the next page of results.
     * @return Instance of {@link BeanSearchResult} containing the next page results.
     * @throws {@link SearchServerException} thrown when the server is not able to execute the query.
     */
    public SearchResult nextPage() {
        try{
            return server.execute(query.copy().page(this.page.next()), factory);
        } catch (SearchServerException e) {
            log.error("Unable to retrieve from search server next result", e);
            throw e;
        }
    }

    /**
     * Gets the previous page of results.
     * @return Instance of BeanSearchResult containing the previous page results.
     * @throws {@link SearchServerException} thrown when the server is not able to execute the query.
     */
    public SearchResult previousPage() {
        try{
            return server.execute(query.copy().page(this.page.previous()), factory);
        } catch (SearchServerException e) {
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


    @Override
    public  <P> BeanSearchResult<P> toPojoResult(SearchResult searchResult, Class<P> clazz) {
        return new BeanPageResult<>(searchResult.numOfResults,
                searchResult.getQueryTime(),
                searchResult.results.stream().map(d -> AnnotationUtil.createPojo(d, clazz)).collect(Collectors.toList()),
                searchResult.query,
                searchResult.facetResults,
                searchResult.server,
                clazz
        ).setElapsedTime(searchResult.getElapsedTime());
    }
}
