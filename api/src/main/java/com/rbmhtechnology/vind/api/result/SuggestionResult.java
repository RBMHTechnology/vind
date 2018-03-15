package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.result.facet.TermFacetResult;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.HashMap;
import java.util.Set;

/**
 * Class used to store the suggestion query results.
 *
 */
public class SuggestionResult {

    private HashMap<FieldDescriptor, TermFacetResult<?>> suggestions = new HashMap<>();
    private String spellcheck;
    private DocumentFactory factory;
    private Long queryTime;
    private Long elapsedTime;

    public SuggestionResult(){}

    /**
     * DEPRECATED: use the signature providing the time the query took.{@link SuggestionResult#SuggestionResult(HashMap, String, long, DocumentFactory)}
     * Create a new instance of {@link SuggestionResult}.
     *
     * @param suggestions a map of document fields as keys and suggestions plus count of documents by suggestion as values.
     * @param factory document factory configured with a document schema.
     * @param spellcheck {@link String} value of the spell check suggestion if existing.
     */
    @Deprecated
    public SuggestionResult(HashMap<FieldDescriptor, TermFacetResult<?>> suggestions, String spellcheck, DocumentFactory factory) {
        this.suggestions = suggestions;
        this.spellcheck = spellcheck;
        this.factory = factory;
        this.queryTime = null;
    }

    /**
     * Create a new instance of {@link SuggestionResult}.
     * @param suggestions a map of document fields as keys and suggestions plus count of documents by suggestion as values.
     * @param factory document factory configured with a document schema.
     * @param spellcheck {@link String} value of the spell check suggestion if existing.
     * @param queryTime    the time the query took in the backend.
     * @param factory      the {@link DocumentFactory} used for mapping the results.
     */
    public SuggestionResult(HashMap<FieldDescriptor, TermFacetResult<?>> suggestions, String spellcheck, long queryTime, DocumentFactory factory) {
        this.suggestions = suggestions;
        this.spellcheck = spellcheck;
        this.factory = factory;
        this.queryTime = queryTime;
    }

    /**
     * Gets the suggestions from a field.
     * @param name String name of the field.
     * @return {@link TermFacetResult} with the suggestions as facet.
     */
    public TermFacetResult<?> get(String name) {
        return factory != null ? suggestions.get(factory.getField(name)) : null;
    }

    /**
     * Gets the suggestions from a field.
     * @param descriptor {@link FieldDescriptor} of the field.
     * @return {@link TermFacetResult} with the suggestions as facet.
     */
    public TermFacetResult<?> get(FieldDescriptor descriptor) {
        return suggestions.get(descriptor);
    }

    /**
     * Gets the fields descriptors which are queried for suggestions.
     * @return {@link FieldDescriptor} with the suggested fields.
     */
    public Set<FieldDescriptor> getSuggestedFields() {
        return suggestions.keySet();
    }

    /**
     * Return spellcheck results (or empty list, if there are none)
     * @return spellcheck results
     */
    public String getSpellcheck() {
        return spellcheck;
    }

    /**
     * Gets the number of suggestions from every field.
     * @return a number of suggestions.
     */
    public int size() {
        int size = suggestions.values().stream()
                .mapToInt(termFacetResult ->
                                termFacetResult.getValues().size()
                ).sum();
        return size;
    }

    /**
     * Gets the time the query took in the backend to be performed.
     * @return a number of milliseconds.
     */
    public Long getQueryTime() {
        return queryTime;
    }

    /**
     * Gets the time the query took in the backend to be performed plus the time it takes read from disk and to build the result.
     * @return a number of milliseconds.
     */
    public Long getElapsedTime() {
        return elapsedTime;
    }

    /**
     * Sets the time the query took in the backend to be performed plus the time it takes read from disk and to build the result.
     * @return this instance of {@link SuggestionResult} with the modified elapsed time.
     */
    public SuggestionResult setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
        return this;
    }
}
