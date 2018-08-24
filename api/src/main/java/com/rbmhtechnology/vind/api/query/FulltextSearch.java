package com.rbmhtechnology.vind.api.query;

import com.rbmhtechnology.vind.api.query.distance.Distance;
import com.rbmhtechnology.vind.api.query.division.Page;
import com.rbmhtechnology.vind.api.query.division.ResultSubset;
import com.rbmhtechnology.vind.api.query.division.Slice;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Facets;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.configure.SearchConfiguration;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.SingleValueFieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.rbmhtechnology.vind.api.query.filter.Filter.*;


/**
 * Class to prepare a full text search query.
 */
public class FulltextSearch {

    private String searchString = null;
    private Filter filter = null;
    private List<Sort> sorting = new ArrayList<>();
    private int facetMinCount =  SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_FACET_INCLUDE_EMPTY, false)? 0 : 1;
    private int facetLimit = SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_FACET_LENGTH, 4);
    private Map<String,Facet> facets = new HashMap<>();
    private ResultSubset resultSet;
    private boolean childrenSearch = false;
    private Operators childrenSearchOperator = Operators.OR;
    private List<FulltextSearch> childrenSearchString = new ArrayList<>();
    private DocumentFactory childrenFactory = null;
    private String timeZone = null;
    private Distance geoDistance = null;
    private String searchContext = null;
    private boolean strict = true;

    /**
     * Creates a new basic full text search query object.
     */
    FulltextSearch() {
        this.searchString = "*";
        this.resultSet = new Page(1, SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_PAGESIZE,10));
    }

    /**
     * Creates a clone of the actual fulltext search query.
     * @return A new {@link FulltextSearch} instance.
     */
    public FulltextSearch copy() {
        final FulltextSearch copy = new FulltextSearch();

        copy.searchString = new String(this.searchString);
        copy.resultSet = resultSet.copy();
        if (Objects.nonNull(this.getFilter())) {
            copy.filter = this.getFilter().clone();
        }
        copy.sorting = this.getSorting().stream().map( s -> s.clone()).collect(Collectors.toList());


        this.getFacets().keySet().stream().forEach(k -> copy.facets.put(k,this.getFacets().get(k).clone()));
        
        return copy;
    }


    /**
     *  Sets the text for fulltext search query to the specified value.
     * @param fullText String text to be searched for.
     * @return This {@link FulltextSearch} instance with the new text query.
     */
    public FulltextSearch text(String fullText) {
        this.searchString = fullText;
        return this;
    }

    /**
     *  Sets the search context for fulltext search.
     * @param context String context to be searched in.
     * @return This {@link FulltextSearch} instance with the new context.
     */
    public FulltextSearch context(String context) {
        this.searchContext = context;
        return this;
    }

    /**
     * Sets the full text search to use the same text search in the nested documents, returning also the parents containing a
     * children which matches the search.
     * @return This {@link FulltextSearch} instance with the the deepSearch enabled.
     */
    public FulltextSearch orChildrenSearch(DocumentFactory childrenFactory){
        this.childrenSearch = true;
        this.childrenSearchOperator = Operators.OR;
        this.childrenSearchString.clear();
        this.childrenSearchString.add(this.copy());
        this.childrenFactory = childrenFactory;
        return this;
    }

    /**
     * Sets the full text search to use the an specific text search in the nested documents, returning also the parents
     * containing a children which matches the children search search. Even if the parent does not mach the original search.
     * @return This {@link FulltextSearch} instance with the the deepSearch enabled.
     */
    @Deprecated
    public FulltextSearch orChildrenSearch(FulltextSearch childrenSearch, DocumentFactory childrenFactory){
        this.childrenSearch = true;
        this.childrenSearchOperator = Operators.OR;
        this.childrenSearchString.clear();
        this.childrenSearchString.add(childrenSearch);
        this.childrenFactory = childrenFactory;
        return this;
    }

    /**
     * Sets the full text search to use the an specific text search in the nested documents, returning also the parents
     * containing a children which matches the children search search. Even if the parent does not mach the original search.
     * @param childrenFactory {@link DocumentFactory} defining the type of the children documents to search by.
     * @param childrenSearch {@link FulltextSearch} searches defining the filters and text to search by.
     * @return This {@link FulltextSearch} instance with the the deepSearch enabled.
     */
    public FulltextSearch orChildrenSearch(DocumentFactory childrenFactory, FulltextSearch... childrenSearch){
        this.childrenSearch = true;
        this.childrenSearchOperator = Operators.OR;
        this.childrenSearchString.clear();
        this.childrenSearchString.addAll(Arrays.asList(childrenSearch));
        this.childrenFactory = childrenFactory;
        return this;
    }

    /**
     * Sets the full text search to use the same text search in the nested documents, returning just the parents matching the search and containing a
     * children which matches de search.
     * @return This {@link FulltextSearch} instance with the the deepSearch enabled.
     */
    public FulltextSearch andChildrenSearch(DocumentFactory childrenFactory){
        this.childrenSearch = true;
        this.childrenSearchOperator = Operators.AND;
        this.childrenSearchString.clear();
        this.childrenSearchString.add(this.copy());
        this.childrenFactory = childrenFactory;
        return this;
    }

    /**
     * Sets the full text search to use the an specific text search in the nested documents, returning just the parents
     * matching the parent search containing children which matches the children search.
     * @return This {@link FulltextSearch} instance with the the deepSearch enabled.
     */
    @Deprecated
    public FulltextSearch andChildrenSearch(FulltextSearch childrenSearch, DocumentFactory childrenFactory){
        this.childrenSearch = true;
        this.childrenSearchOperator = Operators.AND;
        this.childrenSearchString.clear();
        this.childrenSearchString.add(childrenSearch);
        this.childrenFactory = childrenFactory;
        return this;
    }

    /**
     * Sets the full text search to use the an specific text search in the nested documents, returning just the parents
     * matching the parent search containing children which matches the children search.
     * @param childrenFactory {@link DocumentFactory} defining the type of the children documents to search by.
     * @param childrenSearch {@link FulltextSearch} searches defining the filters and text to search by.
     * @return This {@link FulltextSearch} instance with the the deepSearch enabled.
     */
    public FulltextSearch andChildrenSearch(DocumentFactory childrenFactory, FulltextSearch... childrenSearch){
        this.childrenSearch = true;
        this.childrenSearchOperator = Operators.AND;
        this.childrenSearchString.clear();
        this.childrenSearchString.addAll(Arrays.asList(childrenSearch));
        this.childrenFactory = childrenFactory;
        return this;
    }

    /**
     * Adds a basic {@link TermFilter} to the search query.
     * @param field String name of the field to filter by.
     * @param value String value to filter.
     * @return This {@link FulltextSearch} instance with the new filter added.
     */
    public FulltextSearch filter(String field, String value) {
        return filter(eq(field, value));
    }

    /**
     * Adds a {@link Filter} to the search query.
     * @param filter {@link Filter} filter to be added to the query.
     * @return This {@link FulltextSearch} instance with the new filter added.
     */
    public FulltextSearch filter(Filter filter) {
        if (filter == null) {
            return clearFilter();
        } else if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = and(this.filter, filter);
        }
        return this;
    }

    /**
     * Removes all the filters of the fulltext search query.
     * @return This {@link FulltextSearch} instance without the filters.
     */
    public FulltextSearch clearFilter() {
        filter = null;
        return this;
    }

    /**
     * Add a basic {@link com.rbmhtechnology.vind.api.query.sort.Sort.SimpleSort} to the fulltext search query.
     * @param field String Name of the field to calculate the sort on.
     * @param direction {@link com.rbmhtechnology.vind.api.query.sort.Sort.Direction} indicating the sorting order.
     * @return This {@link FulltextSearch} instance with the added sort.
     */
    public FulltextSearch sort(String field, Sort.Direction direction) {
        return sort(Sort.field(field, direction));
    }

    /**
     * Add an specific implementation of {@link Sort} to the search query.
     * @param sort {@link Sort} implementation.
     * @return This {@link FulltextSearch} instance with the added sort.
     */
    public FulltextSearch sort(Sort sort) {
        clearSort();
        sorting.add(sort);
        return this;
    }
    /**
     * Add an group of implementations of {@link Sort} to the search query.
     * @param sort {@link Sort} implementations.
     * @return This {@link FulltextSearch} instance with the added sorts.
     */
    public FulltextSearch sort(Sort... sort) {
        clearSort();
        sorting.addAll(Arrays.asList(sort));
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results.
     * @param page int number of page.
     * @param size int number of results to get in every page.
     * @return This {@link FulltextSearch} instance with page configured.
     */
    public FulltextSearch page(int page, int size) {
        this.resultSet = new Page(page, size);
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results. The number of results is read from the
     * config file if existing, if not, by default is 10.
     * @param page int number of page.
     * @return This {@link FulltextSearch} instance with page configured.
     */
    public FulltextSearch page(int page) {
        this.resultSet = new Page(page, SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_PAGESIZE,10));
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results.
     * @param page {@link Page} object indicating the page to retrieve from query.
     * @return This {@link FulltextSearch} instance with page configured.
     */
    public FulltextSearch page(Page page) {
        this.resultSet = page;
        return this;
    }

    /**
     * Set the slice to be returned from the fulltext search query results.
     * @param offset int index number of the result to start from.
     * @param size int number of results to get from the offset.
     * @return This {@link FulltextSearch} instance with slice configured.
     */
    public FulltextSearch slice(int offset, int size) {
        this.resultSet = new Slice(offset, size);
        return this;
    }

    /**
     * Set the slice to be returned from the fulltext search query results. The number of results is read from the
     * config file if existing, if not, by default is 10.
     * @param offset int index number of the result to start from.
     * @return This {@link FulltextSearch} instance with page configured.
     */
    public FulltextSearch slice(int offset) {
        this.resultSet = new Slice(offset, SearchConfiguration.get(SearchConfiguration.SEARCH_RESULT_PAGESIZE,10));
        return this;
    }

    /**
     * Set the page to be returned from the fulltext search query results.
     * @param slice {@link Slice} object indicating the slice of results to retrieve from query.
     * @return This {@link FulltextSearch} instance with page configured.
     */
    public FulltextSearch slice(Slice slice) {
        this.resultSet = slice;
        return this;
    }

    /**
     * Remove all sort configurations from the search query.
     * @return This {@link FulltextSearch} instance without configured sorting.
     */
    public FulltextSearch clearSort() {
        sorting.clear();
        return this;
    }


    /**
     * Add basic {@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet} to the fulltext search query.
     * @param descriptor {@link FieldDescriptor} indicating the field to facet on.
     * @return This {@link FulltextSearch} instance with the new facet added.
     */
    public FulltextSearch facet(FieldDescriptor ... descriptor) {
        this.facets.putAll(Facets.term(descriptor));
        return this;
    }

    /**
     * Add basic {@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet} to the fulltext search query.
     * @param scope sets the scope where the facet will be done.
     * @param descriptor {@link FieldDescriptor} indicating the field to facet on.
     * @return This {@link FulltextSearch} instance with the new facet added.
     */
    public FulltextSearch facet(Scope scope, FieldDescriptor ... descriptor) {
        this.facets.putAll(Facets.term(scope,descriptor));
        return this;
    }

    /**
     * Add basic {@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet} to the fulltext search query.
     * @param name String name of the field to facet on.
     * @return This {@link FulltextSearch} instance with the new facet added.
     */
    public FulltextSearch facet(String ... name) {
        this.facets.putAll(Facets.term(name));
        return this;
    }

    /**
     * Add basic {@link com.rbmhtechnology.vind.api.query.facet.Facet.TermFacet} to the fulltext search query.
     * @param scope sets the scope where the facet will be done.
     * @param name String name of the field to facet on.
     * @return This {@link FulltextSearch} instance with the new facet added.
     */
    public FulltextSearch facet(Scope scope, String ... name) {
        this.facets.putAll(Facets.term(scope, name));
        return this;
    }

    /**
     * Add a new {@link Facet} to the fulltext search query.
     * @param facet {@link Facet} facet to be added to the search.
     * @return This {@link FulltextSearch} instance with the new facet added.
     */
    public FulltextSearch facet(Facet facet) {
        this.facets.put(facet.getFacetName(), facet);
        return this;
    }

    /**
     * Remove all the facets configured int he search query.
     * @return This {@link FulltextSearch} instance without any facet.
     */
    public FulltextSearch clearFacets() {
        this.facets.clear();
        return this;
    }

    public int getFacetMinCount() {
        return facetMinCount;
    }

    public int getFacetLimit() {
        return facetLimit;
    }

    public FulltextSearch setFacetMinCount(int minCount) {
        this.facetMinCount = minCount;
        return this;
    }

    public FulltextSearch setFacetLimit(int limit) {
        this.facetLimit = limit;
        return this;
    }

    /**
     * Sets a timezone to use for date calculations on this search.
     * @param timeZone {@link String} to configure the search.
     */
    public FulltextSearch timeZone(String timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    /**
     * Sets a geolocation and thus allows to retrieve geodistance
     * @param field the relation field name
     * @param location {@link LatLng} configures the current point of interest
     * @return This {@link FulltextSearch} instance
     */
    public FulltextSearch geoDistance(String field, LatLng location) {
        this.geoDistance = new Distance(field,location);
        return this;
    }

    /**
     * Sets a geolocation and thus allows to retrieve geodistance
     * @param field the relation field
     * @param location {@link LatLng} configures the current point of interest
     * @return This {@link FulltextSearch} instance
     */
    public FulltextSearch geoDistance(SingleValueFieldDescriptor.LocationFieldDescriptor field, LatLng location) {
        this.geoDistance = new Distance(field,location);
        return this;
    }

    /**
     * Sets a geolocation and thus allows to retrieve geodistance
     * @param field the relation field
     * @param location {@link LatLng} configures the current point of interest
     * @return This {@link FulltextSearch} instance
     */
    public FulltextSearch geoDistance(MultiValueFieldDescriptor.LocationFieldDescriptor field, LatLng location) {
        this.geoDistance = new Distance(field,location);
        return this;
    }

    /**
     * Gets the text of the search query.
     * @return String containing the query target.
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * Gets the text of the search query.
     * @return String containing the query target.
     */
    public String getEscapedSearchString() {
        return StringEscapeUtils.escapeJava(searchString);
    }

    /**
     * Gets the context of the search.
     * @return String containing the context target.
     */
    public String getSearchContext() {
        return searchContext;
    }

    /**
     * Gets the status of the children search.
     * @return True if children search is enabled false otherwise.
     */
    public boolean isChildrenSearchEnabled(){ return childrenSearch;}

    /**
     * Gets the set children search operator.
     * @return AND or OR operators.
     */
    public Operators getChildrenSearchOperator() {
        return childrenSearchOperator;
    }

    /**
     * Gets the factory of the child search
     * @return {@link DocumentFactory}
     */
    public DocumentFactory getChildrenFactory() {
        return childrenFactory;
    }

    /**
     * Gets the first children search query.
     * @return {@link FulltextSearch} containing the first children search configuration.
     */
    @Deprecated
    public FulltextSearch getChildrenSearchString() {
        return childrenSearchString.get(0);
    }

    /**
     * Gets the children search defined for this FulltextSearch.
     * @return {@link List<FulltextSearch>} containing the children searches.
     */
    public List<FulltextSearch> getChildrenSearches() {
        return childrenSearchString;
    }

    /**
     * Gets the filter configured for this search query.
     * @return {@link Filter} instance.
     */
    public Filter getFilter() {
        return filter;
    }
    /**
     * Checks if the search has any filter configured.
     * @return Boolean value, true if it has filters false if it has no filters.
     */
    public boolean hasFilter() {
        return filter != null;
    }
    /**
     * Gets the sortins configured for this search query.
     * @return {@link Sort} instances.
     */
    public List<Sort> getSorting() {
        return sorting;
    }
    /**
     * Checks if the search has any sort configured.
     * @return Boolean value, true if it has filters false if it has no filters.
     */
    public boolean hasSorting() {
        return !sorting.isEmpty();
    }
    /**
     * Gets the result set configured for this search query.
     * @return {@link ResultSubset} instance.
     */
    public ResultSubset getResultSet() {
        return resultSet;
    }
    /**
     * Checks if the search has any facet configured.
     * @return Boolean value, true if it has filters false if it has no filters.
     */
    public boolean hasFacet() {
        return !this.facets.isEmpty();
    }

    /**
     * Gets the facets configured for this search query.
     * @return A map of {@link Facet} having as key the facet names.
     */
    public Map<String, Facet> getFacets() {
        return this.facets;
    }

    /**
     * Gets the configured Time Zone for this search.
     * @return {@link String} with the configured Time zone.
     */
    public String getTimeZone() {
        return timeZone;
    }

    /**
     * Returns the geodistance if set
     * @return a geodistance (field and location)
     */
    public Distance getGeoDistance() {
        return geoDistance;
    }

    /**
     * Changes the Fulltext search {@link FulltextSearch#strict} flag. If set to true a {@link RuntimeException} will be
     * thrown when trying to do a nested document search filtering by parent fields in the nested documents or the other
     * way around.
     * @param strict boolean value to activate or deactivate the strict search.
     * @return This {@link FulltextSearch} object with the {@link FulltextSearch#strict} flag changes.
     */
    public FulltextSearch setStrict(boolean strict) {
        this.strict = strict;
        return this;
    }

    /**
     * Get the value of the flag {@link FulltextSearch#strict} for this {@link FulltextSearch} object.
     * @return the boolean value of {@link FulltextSearch#strict}.
     */
    public boolean getStrict() {
        return this.strict;
    }

    @Override
    public String toString(){
        String searchString = "" +
                "{" +
                "\"q\":\"%s\"," +
                "\"filter\":\"%s\"," +
                "\"timeZone\":\"%s\"," +
                "\"sort\":%s," +
                "\"result\":%s," +
                "\"nestedDocSearchFlag\":%s," +
                "\"nestedDocOp\":\"%s\"," +
                "\"nestedDocFactory\":%s," +
                "\"nestedDocSearch\":%s," +
                "\"facetFlag\":%s," +
                "\"facetMinCount\":%s," +
                "\"facetLimit\":%s," +
                "\"facet\":{%s}," +
                "\"geoDistance\":%s," +
                "\"searchContext\":\"%s\"," +
                "\"strictFlag\":%s" +
                "}";

        return String.format(searchString,
                this.searchString,
                this.filter,
                this.timeZone,
                CollectionUtils.isNotEmpty(this.sorting) ? "[" + this.sorting.stream().map(f -> f.toString()).collect(Collectors.joining(", ")) +"]": "[]",
                this.resultSet,
                this.childrenSearch,
                this.childrenSearchOperator,
                this.childrenFactory,
                "[" + this.childrenSearchString.stream().map(FulltextSearch::toString).collect(Collectors.joining(",")) + "]",
                this.hasFacet(),
                this.facetMinCount,
                this.facetLimit,
                this.facets.entrySet().stream().map(e -> e.getValue().toString()).collect(Collectors.joining(",")),
                this.geoDistance,
                this.searchContext,
                this.strict);
    }
    public enum Operators {
        AND, OR
    }
}
