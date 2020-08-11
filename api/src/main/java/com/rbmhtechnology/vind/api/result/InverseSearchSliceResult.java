package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearch;
import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.model.DocumentFactory;

import java.util.List;

/**
 * Created by fonso on 31.03.17.
 */
public class InverseSearchSliceResult extends InverseSearchResult {
    private final Slice slice;

    /**
     * Creates a new instance of {@link InverseSearchSliceResult}.
     *
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results      A list of results parsed to Document.
     * @param searchQuery  The {@link InverseSearch} query executed to retrieve this set of results.
     * @param server       A search server implementation.
     * @param docFactory   document factory holding the schema configuration of documents to parse the results to.
     */
    public InverseSearchSliceResult(long numOfResults, long queryTime, List<InverseSearchQuery> results, InverseSearch searchQuery, SearchServer server, DocumentFactory docFactory) {
        super(numOfResults, queryTime, results, searchQuery, server, docFactory);
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
