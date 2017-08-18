package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * This class stores the search result documents as instances of {@link Document}.
 */
public class GetResult {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final long numOfResults;
    private final List<Document> results;
    private final RealTimeGet query;
    private final DocumentFactory factory;

    public GetResult() {
        this.numOfResults = 0L;
        this.results = Collections.emptyList();
        this.query = new RealTimeGet();
        this.factory = new DocumentFactoryBuilder("empty").build();
    }

    /**
     * Creates a new instance of {@link GetResult}.
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results A list of results parsed to Document.
     * @param getQuery The fulltext query executed to retrieve this set of results.
     * @param docFactory document factory holding the schema configuration of documents to parse the results to.
     */
    public GetResult(long numOfResults, List<Document> results, RealTimeGet getQuery, DocumentFactory docFactory) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = getQuery;
        this.factory = docFactory;

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

    @Override
    public String toString() {
        return "GetResult{" +
                "numOfResults=" + numOfResults +
                ", results=" + results +
                '}';
    }



}
