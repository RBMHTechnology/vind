package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.value.LatLng;

import java.util.List;
import java.util.function.Function;

/**
 * Created by fonso on 13.02.17.
 */
public abstract class ComplexFieldDescriptor<T,F,S> extends FieldDescriptor<T> {
    private Function<T, List<F>> facetFunction;
    private Function<T, S> storeFunction;
    private Function<T, List<String>> fullTextFunction;
    private Function<T, List<String>> suggestFunction;
    private Function<T, List<F>> advanceFilter;
    private Class<F> facetType;
    private Class<S> storeType;
    private boolean filter;

    protected ComplexFieldDescriptor(String fieldName, Class<T> type, Class<F> facet, Class<S> storedType) {
        super(fieldName, type);
        this.facetType = facet;
        this.storeType = storedType;
    }

    protected ComplexFieldDescriptor setFullText(boolean fullText,Function<T,List<String>> lambda) {
        super.setFullText(fullText);
        this.fullTextFunction = lambda;
        return this;
    }

    protected ComplexFieldDescriptor setStored(boolean stored,Function<T,S> lambda) {
        super.setStored(stored);
        this.storeFunction = lambda;
        return this;
    }

    protected ComplexFieldDescriptor setFacet(boolean facet,Function<T,List<F>> lambda) {
        super.setFacet(facet);
        this.facetFunction = lambda;
        return this;
    }

    protected ComplexFieldDescriptor setSuggest(boolean suggest,Function<T,List<String>> lambda) {
        super.setSuggest(suggest);
        this.suggestFunction = lambda;
        return this;
    }

    protected ComplexFieldDescriptor setAdvanceFilter(boolean filter,Function<T,List<F>> lambda) {
        this.filter = filter;
        this.advanceFilter = lambda;
        return this;
    }

    public Function<T, List<F>> getFacetFunction() {
        return facetFunction;
    }

    public Function<T, List<String>> getFullTextFunction() {
        return fullTextFunction;
    }

    public Function<T, List<String>> getSuggestFunction() {
        return suggestFunction;
    }

    public Function<T, S> getStoreFunction() {
        return storeFunction;
    }

    public Function<T, List<F>> getAdvanceFilter() {
        return advanceFilter;
    }

    public Class<F> getFacetType() {
        return facetType;
    }

    public Class<S> getStoreType() {
        return storeType;
    }

    public boolean isAdvanceFilter() {
        return filter;
    }

    /**
     * Instantiates a new {@link Filter} to checking if a field value is not empty.
     * @return A configured filter for the field.
     */
    public Filter isNotEmpty() {
        return isNotEmpty(Filter.DEFAULT_SCOPE);
    }

    /**
     * Instantiates a new {@link Filter} to checking if a field value is not empty.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return A configured filter for the field.
     */
    public Filter isNotEmpty(Filter.Scope scope) {

        switch (scope) {
            case Suggest:
            return new Filter.NotEmptyFilter(this.getName(), scope);
            default:
                if(CharSequence.class.isAssignableFrom(this.facetType)) {
                    return new Filter.NotEmptyTextFilter(this.getName(), scope);
                }
                if(LatLng.class.isAssignableFrom(this.facetType)) {
                    return new Filter.NotEmptyLocationFilter(this.getName(), scope);
                }

                return new Filter.NotEmptyFilter(this.getName(), scope);
        }
    }

    /**
     * Instantiates a new {@link Filter} to checking if a field value is empty.
     * @return A configured filter for the field.
     */
    public Filter isEmpty() {
        return new Filter.NotFilter(this.isNotEmpty());
    }

    /**
     * Instantiates a new {@link Filter} to checking if a field value is empty.
     * @param scope Enum {@link Scope} describing the scope to perform the filter on.
     * @return A configured filter for the field.
     */
    public Filter isEmpty(Filter.Scope scope) {
        return new Filter.NotFilter( this.isNotEmpty(scope));
    }
}

