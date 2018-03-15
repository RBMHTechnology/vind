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
public class BeanGetResult<T> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final long numOfResults;
    private final List<T> results;
    private final RealTimeGet query;
    private final Class <T> clazz;

    public BeanGetResult() {
        this.numOfResults = 0L;
        this.results = Collections.emptyList();
        this.query = new RealTimeGet();
        this.clazz = null;
    }

    /**
     * Creates a new instance of {@link BeanGetResult}.
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results A list of results parsed to Document.
     * @param getQuery The fulltext query executed to retrieve this set of results.
     * @param clazz Bean class to get as result.
     */
    public BeanGetResult(long numOfResults, List<T> results, RealTimeGet getQuery, Class<T> clazz) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = getQuery;
        this.clazz = clazz;

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
    public List<T> getResults() {
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
