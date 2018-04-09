package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.annotations.AnnotationUtil;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.get.RealTimeGet;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.DocumentFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class stores the search result documents as instances of {@link Document}.
 */
public class GetResult {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final long numOfResults;
    private final List<Document> results;
    private final RealTimeGet query;
    private final DocumentFactory factory;
    private final long queryTime;
    private long elapsedTime;

    public GetResult() {
        this.numOfResults = 0L;
        this.results = Collections.emptyList();
        this.query = new RealTimeGet();
        this.factory = new DocumentFactoryBuilder("empty").build();
        this.queryTime = -1;
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
        this.queryTime = -1;
    }

    /**
     * Creates a new instance of {@link GetResult}.
     * @param numOfResults Number of documents returned by the search server instance.
     * @param results A list of results parsed to Document.
     * @param getQuery The fulltext query executed to retrieve this set of results.
     * @param docFactory document factory holding the schema configuration of documents to parse the results to.
     * @param qTime time it takes the backend to perform the query.
     */
    public GetResult(long numOfResults, List<Document> results, RealTimeGet getQuery, DocumentFactory docFactory, long qTime) {
        this.numOfResults = numOfResults;
        this.results = results;
        this.query = getQuery;
        this.factory = docFactory;
        this.queryTime = qTime;
    }

    /**
     * Returns the time taken by the backend to get the results and the time it takes
     * to the backend client create a response.
     * @return long number representing the time in milliseconds.
     */
    public long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * Sets time taken by the backend to get the results and the time it takes
     * to the backend client create a response.
     * @param elapsedTime long number representing the time in milliseconds.
     * @return an instance of {@link GetResult} with the updated elapsed time.
     */
    public GetResult setElapsedTime(long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }

    /**
     * Returns the time taken by the backend to get the results.
     * @return long number representing the time in milliseconds.
     */
    public long getQueryTime() {
        return queryTime;
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

    public  <P> BeanGetResult<P> toPojoResult(GetResult getResult, Class<P> clazz) {
        return new BeanGetResult<>(getResult.numOfResults,
                getResult.results.stream().map(d -> AnnotationUtil.createPojo(d, clazz)).collect(Collectors.toList()),
                getResult.query,
                clazz,
                queryTime
        ).setElapsedTime(elapsedTime);
    }



}
