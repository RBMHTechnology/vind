package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.Cursor;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.model.DocumentFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by fonso on 31.03.17.
 */
public class CursorResult extends SearchResult {
    private final Cursor cursor;

    /**
     * Creates a new instance of {@link CursorResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime    the time the query took in the backend.
     * @param results      A list of results parsed to Document.
     * @param searchQuery  The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server       A search server implementation.
     * @param docFactory   document factory holding the schema configuration of documents to parse the results to.
     */
    public CursorResult(long numOfResults, long queryTime, List<Document> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, DocumentFactory docFactory) {
        super(numOfResults, queryTime, results, searchQuery, facetResults, server, docFactory);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.cursor)) {
            this.cursor = (Cursor) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as cursor: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Gets the actual cursor details.
     * @return the cursor details.
     */
    public String getSearchAfter() {
        return this.cursor.getSearchAfter();
    }

    /**
     * Gets the minutes kept alive of the active cursor.
     * @return minutes kept alive of the active cursor.
     */
    public long getMinutesKeptAlive() {
        return this.cursor.getMinutesKeptAlive();
    }

    /**
     * Gets the next cursor results.
     * @return Instance of {@link BeanSearchResult} containing the next cursor results.
     */
    public CursorResult next() {
        try{
            return (CursorResult)server.execute(query.copy().cursor(this.cursor), factory);
        } catch (SearchServerException e) {
            log.error("Unable to retrieve from search server next result", e);
            throw e;
        }
    }

    public void closeCursor() {

    }

    @Override
    public  <P> BeanSearchResult<P> toPojoResult(SearchResult searchResult, Class<P> clazz) {
        return new BeanSliceResult<>(searchResult.numOfResults,
                searchResult.getQueryTime(),
                searchResult.results.stream().map(d -> AnnotationUtil.createPojo(d, clazz)).collect(Collectors.toList()),
                searchResult.query,
                searchResult.facetResults,
                searchResult.server,
                clazz
        ).setElapsedTime(searchResult.getElapsedTime());
    }
}
