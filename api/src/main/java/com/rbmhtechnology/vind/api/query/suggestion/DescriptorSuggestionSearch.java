package com.rbmhtechnology.vind.api.query.suggestion;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Class to configure suggestions based on field descriptors.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 07.07.16.
 */
public class DescriptorSuggestionSearch implements ExecutableSuggestionSearch {

    private String input;
    private int limit = 10;
    private Filter filter = null;
    private Set<FieldDescriptor> suggestionFields = new HashSet<>();
    private String searchContext = null;
    /**
     * Creates a new instance of {@link DescriptorSuggestionSearch}.
     * @param input String text to find suggestion for.
     * @param filter {@link Filter} to apply to the suggestion search.
     * @param field {@link FieldDescriptor} fields where to find the suggestions.
     */
    protected DescriptorSuggestionSearch(String input, int limit, Filter filter, FieldDescriptor ... field) {
        Objects.requireNonNull(field);
        this.input = input;
        this.limit = limit;
        this.filter = filter;
        suggestionFields.addAll(Arrays.asList(field));
    }

    /**
     * Set the text to find suggestions for.
     * @param input String text to find suggestion for.
     * @return {@link DescriptorSuggestionSearch} with the new text.
     */
    public DescriptorSuggestionSearch text(String input) {
        this.input = input;
        return this;
    }
    /**
     * Sets a basic {@link com.rbmhtechnology.vind.api.query.filter.Filter.TermFilter} for the suggestion search query.
     * @param field {@link FieldDescriptor} of the field to filter in.
     * @param value String value of to filter by.
     * @return This {@link DescriptorSuggestionSearch} with the new filter configuration.
     */
    public DescriptorSuggestionSearch filter(FieldDescriptor field, String value) {
        return filter(Filter.eq(field, value));
    }

    @Override
    public DescriptorSuggestionSearch setLimit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public DescriptorSuggestionSearch context(String context) {
        this.searchContext = context;
        return this;
    }

    @Override
    public String getSearchContext() {
        return this.searchContext;
    }

    /**
     * Sets a given {@link Filter} for the suggestion search query.
     * @param filter {@link Filter} filter to apply to the suggestions search.
     * @return This {@link DescriptorSuggestionSearch} with the new filter configuration.
     */
    public DescriptorSuggestionSearch filter(Filter filter) {
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
     * @return This {@link DescriptorSuggestionSearch} without any filter configurations.
     */
    public DescriptorSuggestionSearch clearFilter() {
        filter = null;
        return this;
    }

    /**
     * Remove all field configurations.
     * @return This {@link DescriptorSuggestionSearch} without any field configured.
     */
    public DescriptorSuggestionSearch resetFields() {
        suggestionFields.clear();
        return this;
    }
    /**
     * Adds a field to search for suggestions in.
     * @param field {@link FieldDescriptor} of the document field.
     * @return {@link DescriptorSuggestionSearch} with the added field.
     */
    public DescriptorSuggestionSearch addField(FieldDescriptor field) {
        suggestionFields.add(field);
        return this;
    }
    /**
     * Get the descriptors of the fields where the suggestions are searched in.
     * @return A {@code Set<FieldDescriptor>} of field descriptors.
     */
    public Set<FieldDescriptor> getSuggestionFields() {
        return suggestionFields;
    }
    /**
     * Sets the fields to search for suggestions in.
     * @param fields {@link FieldDescriptor} of the document fields.
     * @return {@link DescriptorSuggestionSearch} with the added fields.
     */
    public DescriptorSuggestionSearch fields(FieldDescriptor... fields) {
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
        return false;
    }
}
