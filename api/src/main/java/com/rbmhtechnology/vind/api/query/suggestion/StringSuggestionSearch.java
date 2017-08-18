package com.rbmhtechnology.vind.api.query.suggestion;

import com.rbmhtechnology.vind.api.query.filter.Filter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Class to configure suggestions based on field String names.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 07.07.16.
 */
public class StringSuggestionSearch implements ExecutableSuggestionSearch {

    private String input;
    private int limit = 10;
    private Filter filter = null;
    private Set<String> suggestionFields = new HashSet<>();
    private String searchContext = null;

    /**
     * Creates a new instance of {@link StringSuggestionSearch}.
     * @param input String text to find suggestion for.
     * @param filter {@link Filter} to apply to the suggestion search.
     * @param field String names of the document fields where to find the suggestions.
     */
    protected StringSuggestionSearch(String input, int limit, Filter filter, String ... field) {
        Objects.requireNonNull(field);
        this.input = input;
        this.limit = limit;
        this.filter = filter;
        suggestionFields.addAll(Arrays.asList(field));
    }

    /**
     * Set the text to find suggestions for.
     * @param input String text to find suggestion for.
     * @return {@link StringSuggestionSearch} with the new text.
     */
    public StringSuggestionSearch text(String input) {
        this.input = input;
        return this;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public StringSuggestionSearch setLimit(int limit) {
        this.limit = limit;
        return this;
    }
    @Override
    public StringSuggestionSearch context(String context) {
        this.searchContext = context;
        return this;
    }

    @Override
    public String getSearchContext() {
        return this.searchContext;
    }
    /**
     * Sets a basic {@link com.rbmhtechnology.vind.api.query.filter.Filter.TermFilter} for the suggestion search query.
     * @param field String name of the field to filter in.
     * @param value String value of to filter by.
     * @return This {@link StringSuggestionSearch} with the new filter configuration.
     */
    public StringSuggestionSearch filter(String field, String value) {
        return filter(Filter.eq(field, value));
    }
    /**
     * Sets a given {@link Filter} for the suggestion search query.
     * @param filter {@link Filter} filter to apply to the suggestions search.
     * @return This {@link StringSuggestionSearch} with the new filter configuration.
     */
    public StringSuggestionSearch filter(Filter filter) {
        if (filter == null) {
            return clearFilter();
        } else if (this.filter == null) {
            this.filter = filter;
        } else {
            this.filter = Filter.and(this.filter, filter);
        }
        return this;
    }
    /**
     * Remove all filter configurations.
     * @return This {@link StringSuggestionSearch} without any filter configurations.
     */
    public StringSuggestionSearch clearFilter() {
        filter = null;
        return this;
    }
    /**
     * Remove all field configurations.
     * @return This {@link StringSuggestionSearch} without any field configured.
     */
    public StringSuggestionSearch resetFields() {
        suggestionFields.clear();
        return this;
    }

    /**
     * Adds a field to search for suggestions in.
     * @param field String name of the document field.
     * @return {@link StringSuggestionSearch} with the added field.
     */
    public StringSuggestionSearch addField(String field) {
        suggestionFields.add(field);
        return this;
    }

    /**
     * Get the names of the fields where the suggestions are searched in.
     * @return A {@code Set<String>} of field names.
     */
    public Set<String> getSuggestionFields() {
        return suggestionFields;
    }

    /**
     * Sets the fields to search for suggestions in.
     * @param fields String names of the document fields.
     * @return {@link StringSuggestionSearch} with the added fields.
     */
    public StringSuggestionSearch fields(String... fields) {
        resetFields();
        suggestionFields.addAll(Arrays.asList(fields));
        return this;
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

    @Override
    public boolean isStringSuggestion() {
        return true;
    }

}
