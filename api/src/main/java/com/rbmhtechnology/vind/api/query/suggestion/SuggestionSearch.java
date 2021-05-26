package com.rbmhtechnology.vind.api.query.suggestion;

import com.rbmhtechnology.vind.api.query.FulltextTerm;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.sort.Sort;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.empty;

/**
 * Class to prepare suggestion queries.
 */
public class SuggestionSearch {

    private String input;
    private int limit = 10;
    private Filter filter = null;
    private Set<FieldDescriptor> suggestionFields = new HashSet<>();
    private Set<String> suggestionStringFields = new HashSet<>();
    private String searchContext = null;
    private Sort sort = null;
    private Optional<FulltextTerm> fulltextTerm = empty();
    private SuggestionOperator operator = SuggestionOperator.AND;

    /**
     * Creates a new instance of {@link SuggestionSearch}.
     */
    public SuggestionSearch() {
        this.input = "*";
    }

    /**
     * Creates a clone of the actual suggestion search query.
     * @return A new {@link SuggestionSearch} instance.
     */
    public SuggestionSearch copy() {
        final SuggestionSearch copy = new SuggestionSearch();

        copy.input = new String(this.input);
        copy.limit = limit;
        if (Objects.nonNull(this.getFilter())) {
            copy.filter = this.getFilter().clone();
        }
        copy.suggestionFields = this.suggestionFields;
        copy.suggestionStringFields = this.suggestionStringFields;
        copy.searchContext = this.searchContext;
        copy.sort = this.sort;
        copy.fulltextTerm = this.fulltextTerm;
        return copy;
    }

    /**
     * Sets the text to get suggestions for.
     * @param input String text to find suggestions for.
     * @return This {@link SuggestionSearch} with the new text.
     */
    public SuggestionSearch text(String input) {
        this.input = input;
        return this;
    }

    /**
     * Sets a basic {@link com.rbmhtechnology.vind.api.query.filter.Filter.TermFilter} for the suggestion search query.
     * @param field String name of the field to filter in.
     * @param value String value of to filter by.
     * @return This {@link SuggestionSearch} with the new filter configuration.
     */
    public SuggestionSearch filter(String field, String value) {
        return filter(Filter.eq(field, value));
    }

    /**
     * Sets a given {@link Filter} for the suggestion search query.
     * @param filter {@link Filter} filter to apply to the suggestions search.
     * @return This {@link SuggestionSearch} with the new filter configuration.
     */
    public SuggestionSearch filter(Filter filter) {
        if (filter == null) {
            return clearFilter();
        } else if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = Filter.and(this.filter, filter);
        }
        return this;
    }

    public int getLimit() {
        return limit;
    }

    public SuggestionSearch setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Remove all filter configurations.
     * @return This {@link SuggestionSearch} without any filter configurations.
     */
    public SuggestionSearch clearFilter() {
        filter = null;
        return this;
    }

    /**
     * Adds a field to search for suggestions in.
     * @param field String name of the document field.
     * @return {@link ExecutableSuggestionSearch} with the added field.
     */
    public ExecutableSuggestionSearch addField(String field) {
        return new StringSuggestionSearch(input,limit,filter,field)
                .setOperator(this.operator)
                .context(this.searchContext)
                .fulltextTerm(fulltextTerm.orElse(null));
    }

    /**
     * Get the names of the fields where the suggestions are searched in.
     * @return A {@code Set<String>} of field names.
     */
    public Set<String> getSuggestionStringFields() {
        return suggestionStringFields;
    }

    /**
     * Sets the fields to search for suggestions in.
     * @param fields String names of the document fields.
     * @return {@link StringSuggestionSearch} with the added fields.
     */
    //FIXME: this supports also: fields()
    public StringSuggestionSearch fields(String... fields) {
        return new StringSuggestionSearch(input,limit,filter,fields)
                .setOperator(this.operator)
                .context(this.searchContext)
                .fulltextTerm(fulltextTerm.orElse(null));
    }

    /**
     * Adds a field to search for suggestions in.
     * @param field {@link FieldDescriptor} of the document field.
     * @return {@link DescriptorSuggestionSearch} with the added field.
     */
    public DescriptorSuggestionSearch addField(FieldDescriptor field) {
        return new DescriptorSuggestionSearch(input,limit,filter,field)
                .setOperator(this.operator)
                .context(this.searchContext)
                .fulltextTerm(fulltextTerm.orElse(null));
    }
    /**
     * Adds the fields to search for suggestions in.
     * @param fields {@link FieldDescriptor} of the document fields.
     * @return {@link DescriptorSuggestionSearch} with the added fields.
     */
    //FIXME: this supports also: fields()
    public DescriptorSuggestionSearch fields(FieldDescriptor... fields) {
        return new DescriptorSuggestionSearch(input,limit,filter,fields)
                .setOperator(this.operator)
                .context(this.searchContext)
                .fulltextTerm(fulltextTerm.orElse(null));
    }

    /**
     * Gets the text of the suggestion query.
     * @return String containing the query target.
     */
    public String getInput() {
        return input;
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

    public SuggestionSearch context(String context) {
        this.searchContext = context;
        return this;
    }

    public String getSearchContext() {
        return this.searchContext;
    }

    public Sort getSort() {
        return sort;
    }

    public SuggestionSearch setSort(final Sort sort) {
        this.sort = sort;
        return this;
    }
    /**
     * Get the fulltext term, to base the search on.
     * Useful when suggesting within an already existing fulltext search
     *
     * @return a fulltext term
     */
    public Optional<FulltextTerm> getFulltextTerm() {
        return fulltextTerm;
    }

    public SuggestionSearch fulltextTerm(final FulltextTerm fulltextTerm) {
        this.fulltextTerm = Optional.ofNullable(fulltextTerm);
        return this;
    }

    public SuggestionOperator getOperator() {
        return operator;
    }

    public SuggestionSearch setOperator(SuggestionOperator operator) {
        this.operator = operator;
        return this;
    }

    public enum SuggestionOperator {
        AND,OR
    }
}
