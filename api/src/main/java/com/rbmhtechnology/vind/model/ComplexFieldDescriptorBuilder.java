package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.annotations.language.Language;
import com.rbmhtechnology.vind.model.value.LatLng;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Created by fonso on 22.02.17.
 */
public class ComplexFieldDescriptorBuilder<T,F,S> {


    private boolean stored = false;
    private boolean indexed = true;
    private boolean fullText = false;
    private Language language = Language.None;
    private float boost = 1;
    private boolean facet = false;
    private boolean suggest = false;
    private boolean filter;
    private Map<String,String> metadata = new HashMap<>();
    private Function<T, List<String>> fullTextFunction;
    private Function<T, S> storeFunction;
    private Function<T, List<F>> facetFunction;
    private Function<T, List<String>> suggestFunction;
    private Function<T, List<S>> filterFunction;


    /**
     * Builds a ComplexFieldDescriptor field which facet type should extend CharSequence, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Text facet field descriptor.
     */
    public <F extends CharSequence> SingleValuedComplexField.TextComplexField<T,F,S> buildTextComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        SingleValuedComplexField.TextComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.TextComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(storeFunction);
        }
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable ComplexFieldDescriptor field which facet type should extend CharSequence, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex Text facet field descriptor.
     */
    public <F extends CharSequence> SingleValuedComplexField.TextComplexField<T,F,S> buildSortableTextComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<T,F> sortFunction){
        SingleValuedComplexField.TextComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.TextComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);

        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a Multivalued ComplexFieldDescriptor field which facet type should extend CharSequence, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Text facet field descriptor.
     */
    public <F extends CharSequence> MultiValuedComplexField.TextComplexField<T,F,S> buildMultivaluedTextComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        MultiValuedComplexField.TextComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.TextComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(c -> {
                final Iterator iterator = ((Collection)c).iterator();
                if (iterator.hasNext()) {
                    return storeFunction.apply((T)iterator.next());
                } else {
                    return null;
                }
            });
        }
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter,filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable Multivalued ComplexFieldDescriptor field which facet type should extend CharSequence, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex Text facet field descriptor.
     */
    public <F extends CharSequence> MultiValuedComplexField.TextComplexField<T,F,S> buildSortableMultivaluedTextComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<Collection<T>,F> sortFunction){
        MultiValuedComplexField.TextComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.TextComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a ComplexFieldDescriptor field which facet type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Number facet field descriptor.
     */
    public <F extends Number> SingleValuedComplexField.NumericComplexField<T,F,S> buildNumericComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        SingleValuedComplexField.NumericComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.NumericComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(storeFunction);
        }
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable ComplexFieldDescriptor field which facet type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex Number facet field descriptor.
     */
    public <F extends Number> SingleValuedComplexField.NumericComplexField<T,F,S> buildSortableNumericComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<T,F> sortFunction){
        SingleValuedComplexField.NumericComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.NumericComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a Multivalued ComplexFieldDescriptor field which facet type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Text facet field descriptor.
     */
    public <F extends Number> MultiValuedComplexField.NumericComplexField<T,F,S> buildMultivaluedNumericComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        MultiValuedComplexField.NumericComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.NumericComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(c ->{
                final Iterator iterator = ((Collection)c).iterator();
                if (iterator.hasNext()) {
                    return storeFunction.apply((T)iterator.next());
                } else {
                    return null;
                }
            });
        }
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable Multivalued ComplexFieldDescriptor field which facet type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex Text facet field descriptor.
     */
    public <F extends Number> MultiValuedComplexField.NumericComplexField<T,F,S> buildSortableMultivaluedNumericComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<Collection<T>,F> sortFunction){
        MultiValuedComplexField.NumericComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.NumericComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a ComplexFieldDescriptor field which facet type should extend Date, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Date facet field descriptor.
     */
    public <F extends Date> SingleValuedComplexField.UtilDateComplexField<T,F,S> buildUtilDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        SingleValuedComplexField.UtilDateComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.UtilDateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(storeFunction);
        }
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable ComplexFieldDescriptor field which facet type should extend Date, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex Date facet field descriptor.
     */
    public <F extends Date> SingleValuedComplexField.UtilDateComplexField<T,F,S> buildSortableUtilDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<T,F> sortFunction){
        SingleValuedComplexField.UtilDateComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.UtilDateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a Multivalued ComplexFieldDescriptor field which facet type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Text facet field descriptor.
     */
    public <F extends Date> MultiValuedComplexField.UtilDateComplexField<T,F,S> buildMultivaluedUtilDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        MultiValuedComplexField.UtilDateComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.UtilDateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(c ->{
                final Iterator iterator = ((Collection)c).iterator();
                if (iterator.hasNext()) {
                    return storeFunction.apply((T)iterator.next());
                } else {
                    return null;
                }
            });
        }
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable Multivalued ComplexFieldDescriptor field which facet type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex Text facet field descriptor.
     */
    public <F extends Date> MultiValuedComplexField.UtilDateComplexField<T,F,S> buildSortableMultivaluedUtilDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<Collection<T>,F> sortFunction){
        MultiValuedComplexField.UtilDateComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.UtilDateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a ComplexFieldDescriptor field which facet type should extend ZonedDateTime, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex ZonedDateTime facet field descriptor.
     */
    public <F extends ZonedDateTime> SingleValuedComplexField.DateComplexField<T,F,S> buildDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        SingleValuedComplexField.DateComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.DateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(storeFunction);
        }
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable ComplexFieldDescriptor field which facet type should extend ZonedDateTime, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex ZonedDateTime facet field descriptor.
     */
    public <F extends ZonedDateTime> SingleValuedComplexField.DateComplexField<T,F,S> buildSortableDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<T,F> sortFunction){
        SingleValuedComplexField.DateComplexField<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.DateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a Multivalued ComplexFieldDescriptor field which facet type should extend ZoneDateTime, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Text facet field descriptor.
     */
    public <F extends ZonedDateTime> MultiValuedComplexField.DateComplexField<T,F,S> buildMultivaluedDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        MultiValuedComplexField.DateComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.DateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        if(stored) {
            complexFieldDescriptor.sort = true;
            complexFieldDescriptor.setSort(c ->{
                final Iterator iterator = ((Collection)c).iterator();
                if (iterator.hasNext()) {
                    return storeFunction.apply((T)iterator.next());
                } else {
                    return null;
                }
            });
        }
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a sortable Multivalued ComplexFieldDescriptor field which facet type should extend ZoneDateTime, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @param sortFunction {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return A complex Text facet field descriptor.
     */
    public <F extends ZonedDateTime> MultiValuedComplexField.DateComplexField<T,F,S> buildSortableMultivaluedDateComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType, Function<Collection<T>,F> sortFunction){
        MultiValuedComplexField.DateComplexField<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.DateComplexField<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        complexFieldDescriptor.setSort(sortFunction);
        return complexFieldDescriptor;
    }

    /**
     * Builds a ComplexFieldDescriptor field which facet type should extend LatLng, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex LatLng facet field descriptor.
     */
    public <F extends LatLng> SingleValuedComplexField.LocationComplexFieldDescriptor<T,F,S> buildLocationComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        SingleValuedComplexField.LocationComplexFieldDescriptor<T,F,S> complexFieldDescriptor = new SingleValuedComplexField.LocationComplexFieldDescriptor<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setMultiValue(false);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter, filterFunction);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Builds a Multivalued ComplexFieldDescriptor field which facet type should extend LatLng, configured with the current flags.
     * @param field Name of the new field.
     * @param complexConcept the class of the complex object to be stored.
     * @param facetType the class of the facet values to be indexed.
     * @param storeType the class of the value to be stored.
     * @return A complex Text facet field descriptor.
     */
    public <F extends LatLng> MultiValuedComplexField.LocationComplexFieldDescriptor<T,F,S> buildMultivaluedLocationComplexField(String field, Class<T> complexConcept, Class<F> facetType, Class<S> storeType){
        MultiValuedComplexField.LocationComplexFieldDescriptor<T,F,S> complexFieldDescriptor = new MultiValuedComplexField.LocationComplexFieldDescriptor<>(field, complexConcept, facetType, storeType);
        complexFieldDescriptor.setIndexed(indexed);
        complexFieldDescriptor.setStored(stored,storeFunction);
        complexFieldDescriptor.setFacet(facet, facetFunction);
        complexFieldDescriptor.setFullText(fullText, fullTextFunction);
        complexFieldDescriptor.setSuggest(suggest,suggestFunction);
        complexFieldDescriptor.setAdvanceFilter(filter,filterFunction);
        complexFieldDescriptor.setMultiValue(true);
        complexFieldDescriptor.setLanguage(language);
        complexFieldDescriptor.setBoost(boost);
        complexFieldDescriptor.setMetadata(metadata);
        return complexFieldDescriptor;
    }

    /**
     * Sets the field to be stored or not.
     * @param stored True to configure the field to be stored.
     * @param lambda {@link Function} to use to obtain the value to use as stored search for this complex field.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setStored(boolean stored, Function<T,S> lambda) {
        this.stored = stored;
        this.storeFunction = lambda;
        return this;
    }

    /**
     * Sets the field to be stored or not.
     * @param advanceFilter True to configure the field to have special filtering.
     * @param lambda {@link Function} to use to obtain the value to use for advance filtering on this complex field.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setAdvanceFilter(boolean advanceFilter, Function<T,List<S>> lambda) {
        this.filter = advanceFilter;
        this.filterFunction = lambda;
        return this;
    }

    /**
     * Sets the field to be indexed or not.
     * @param indexed True to configure the field to be indexed.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setIndexed(boolean indexed) {
        this.indexed = indexed;
        return this;
    }

    /**
     * Sets the field to be fullText or not and describes how to obtain for the fulltext value.
     * @param fullText True to configure the field to be fullText.
     * @param lambda {@link Function} to use to obtain the value to use as fulltext search for this complex field.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setFullText(boolean fullText, Function<T,List<String>> lambda) {
        this.fullText = fullText;
        this.fullTextFunction = lambda;
        return this;
    }
    /**
     * Sets the field language: German("de"), English("en"), Spanish("es") or None(null).
     * @param language Language value.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setLanguage(Language language) {
        this.language = language;
        return this;
    }
    /**
     * Sets the boost value for the field.
     * @param boost A float value to modify the calculated score for thr field. 1 is the,'no boost value'.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setBoost(float boost) {
        this.boost = boost;
        return this;
    }


    /**
     * Sets the field to be used for faceting or not and describes how to obtain for the facet value.
     * @param facet True to configure the field to be used on faceting.
     * @param lambda {@link Function} to use to obtain the values for facet operations for this complex field.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setFacet(boolean facet, Function<T,List<F>> lambda) {
        this.facet = facet;
        this.facetFunction = lambda;
        return this;
    }

    /**
     * Sets the field to be used for suggestion or not
     * @param suggest True to configure the field to be used on suggestion.
     * @param lambda {@link Function} to use to obtain the value to use as suggestion search for this complex field.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new configuration.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> setSuggest(boolean suggest, Function<T,List<String>> lambda) {
        this.suggest = suggest;
        this.suggestFunction = lambda;
        return this;
    }

    /**
     * Add metadata to the field.
     * @param name metadata property name.
     * @param value metadata value.
     * @return the {@link ComplexFieldDescriptorBuilder} with the new metadata added.
     */
    public ComplexFieldDescriptorBuilder<T,F,S> putMetadata(String name, String value) {
        this.metadata.put(name, value);
        return this;
    }
}
