package com.rbmhtechnology.vind.api.query.inverseSearch;

import com.google.common.collect.ImmutableList;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.configure.SearchConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class InverseSearch {

    private final List<Document> docs = new ArrayList<>();
    private Filter queryFilter;
    private ResultSubset resultSet;

    public InverseSearch(Document...docs) {
        Optional.ofNullable(docs)
                .map(Arrays::asList)
                .ifPresent(this.docs::addAll);

        this.resultSet = new Page(1, SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_PAGESIZE,10));
    }

    public List<Document> getDocs() {
        return ImmutableList.copyOf(docs);
    }

    public Filter getQueryFilter() {
        return queryFilter;
    }

    public InverseSearch setQueryFilter(Filter queryFilter) {
        this.queryFilter = queryFilter;
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results.
     * @param page int number of page.
     * @param size int number of results to get in every page.
     * @return This {@link InverseSearch} instance with page configured.
     */
    public InverseSearch page(int page, int size) {
        this.resultSet = new Page(page, size);
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results. The number of results is read from the
     * config file if existing, if not, by default is 10.
     * @param page int number of page.
     * @return This {@link InverseSearch} instance with page configured.
     */
    public InverseSearch page(int page) {
        this.resultSet = new Page(page, SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_PAGESIZE,10));
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results.
     * @param page {@link Page} object indicating the page to retrieve from query.
     * @return This {@link InverseSearch} instance with page configured.
     */
    public InverseSearch page(Page page) {
        this.resultSet = page;
        return this;
    }

    /**
     * Set the slice to be returned from the fulltext search query results.
     * @param offset int index number of the result to start from.
     * @param size int number of results to get from the offset.
     * @return This {@link InverseSearch} instance with slice configured.
     */
    public InverseSearch slice(int offset, int size) {
        this.resultSet = new Slice(offset, size);
        return this;
    }

    /**
     * Set the slice to be returned from the fulltext search query results. The number of results is read from the
     * config file if existing, if not, by default is 10.
     * @param offset int index number of the result to start from.
     * @return This {@link InverseSearch} instance with page configured.
     */
    public InverseSearch slice(int offset) {
        this.resultSet = new Slice(offset, SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_PAGESIZE,10));
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results.
     * @param slice {@link Slice} object indicating the slice of results to retrieve from query.
     * @return This {@link InverseSearch} instance with page configured.
     */
    public InverseSearch slice(Slice slice) {
        this.resultSet = slice;
        return this;
    }

    /**
     * Gets the result set configured for this search query.
     * @return {@link ResultSubset} instance.
     */
    public ResultSubset getResultSet() {
        return resultSet;
    }

    @Override
    public String toString(){
        String searchString = "" +
                "{" +
                "\"docs\":\"[%s]\"," +
                "\"filter\":\"%s\"," +
                "\"result\":%s," +
                "}";

        return String.format(searchString,
                this.docs.stream().map(Document::toString).collect(Collectors.joining(",")),
                this.queryFilter,
                this.resultSet);
    }

    public InverseSearch copy() {
        final Document[] documents = (Document[])new ArrayList<>(this.getDocs()).toArray();
        final InverseSearch copy = new InverseSearch(documents);
        copy.resultSet = resultSet.copy();
        if (Objects.nonNull(queryFilter)) {
            copy.queryFilter = this.queryFilter.clone();
        }
        return copy;
    }
}
