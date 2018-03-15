package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.model.DocumentFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by fonso on 31.03.17.
 */
public class SliceResult extends SearchResult {
    private final Slice slice;

    /**
     * DEPRECATED: use the signature providing the time the query took.{@link SliceResult#SliceResult(long, long, List, FulltextSearch, FacetResults, SearchServer, DocumentFactory)}
     * Creates a new instance of {@link SliceResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results      A list of results parsed to Document.
     * @param searchQuery  The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server       A search server implementation.
     * @param docFactory   document factory holding the schema configuration of documents to parse the results to.
     */
    @Deprecated
    public SliceResult(long numOfResults, List<Document> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, DocumentFactory docFactory) {
        super(numOfResults, results, searchQuery, facetResults, server, docFactory);
        if (query.getResultSet().getType().equals(ResultSubset.DivisionType.slice)) {
            this.slice = (Slice) query.getResultSet();
        } else {
            throw new RuntimeException("Search result set is not configured as slice: Result set type is "+query.getResultSet().getType());
        }
    }

    /**
     * Creates a new instance of {@link SliceResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param queryTime    the time the query took in the backend.
     * @param results      A list of results parsed to Document.
     * @param searchQuery  The fulltext query executed to retrieve this set of results.
     * @param facetResults The different faceted results of the query.
     * @param server       A search server implementation.
     * @param docFactory   document factory holding the schema configuration of documents to parse the results to.
     */
    public SliceResult(long numOfResults, long queryTime, List<Document> results, FulltextSearch searchQuery, FacetResults facetResults, SearchServer server, DocumentFactory docFactory) {
        super(numOfResults, queryTime, results, searchQuery, facetResults, server, docFactory);
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
