package com.rbmhtechnology.vind.api.result;

import com.rbmhtechnology.vind.api.result.facet.*;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class to store the facet query results.
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 27.06.16.
 */
public class FacetResults {

    private final TermFacetResult<String> typeFacet;
    private HashMap<FieldDescriptor, TermFacetResult<?>> termFacets;
    private HashMap<String, QueryFacetResult<?>> queryFacets;
    private HashMap<String, RangeFacetResult<?>> rangeFacets;
    private HashMap<String, IntervalFacetResult> intervalFacets;
    private HashMap<String, StatsFacetResult<?>> statsFacets;
    private HashMap<String, List<PivotFacetResult<?>>> pivotFacets;
    private Collection<SubdocumentFacetResult> subdocumentFacets;

    private DocumentFactory factory;

    /**
     * Creates a new instance of {@link FacetResults}.
     * @param factory document factory configured with a document schema.
     * @param termFacets term facet query results.
     * @param typeFacet type facet query results.
     * @param queryFacets query facet query results.
     * @param rangeFacets range facet query results.
     * @param statsFacets statistics facet query results.
     * @param pivotFacets picot facet query results.
     */
    public FacetResults(DocumentFactory factory,
                        HashMap<FieldDescriptor, TermFacetResult<?>> termFacets,
                        TermFacetResult<String> typeFacet,
                        HashMap<String, QueryFacetResult<?>> queryFacets,
                        HashMap<String, RangeFacetResult<?>> rangeFacets,
                        HashMap<String, IntervalFacetResult> intervalFacets,
                        HashMap<String, StatsFacetResult<?>> statsFacets,
                        HashMap<String, List<PivotFacetResult<?>>> pivotFacets,
                        Collection<SubdocumentFacetResult> subDocumentFacets) {
        this.factory = factory;
        this.termFacets = termFacets;
        this.typeFacet = typeFacet;
        this.queryFacets = queryFacets;
        this.rangeFacets = rangeFacets;
        this.intervalFacets = intervalFacets;
        this.statsFacets = statsFacets;
        this.pivotFacets = pivotFacets;
        this.subdocumentFacets = subDocumentFacets;
    }

    /**
     * Gets the term facet results for a given field.
     * @param descriptor Field descriptor.
     * @param <T> Field descriptor type.
     * @return a {@link TermFacetResult} with the facets of the given field.
     */
    public <T> TermFacetResult<T> getTermFacet(FieldDescriptor<T> descriptor) {
        if(termFacets.containsKey(descriptor)) {
            return (TermFacetResult<T>) termFacets.get(descriptor); //TODO
        } else return null;
    }

    /**
     * Gets the term facet results for a given field.
     * @param name String name of the field.
     * @param t Class type stored by the field.
     * @param <T> Field descriptor type.
     * @return A {@link TermFacetResult} with a set of facets of type T and the count of documents matching each one.
     */
    public <T> TermFacetResult<T> getTermFacet(String name, Class<T> t) {
        final FieldDescriptor descriptor = factory.getField(name);
        if(descriptor != null) {
            final Object ret = getTermFacet(descriptor);
            return ret != null ? (TermFacetResult<T>) ret : null;
        }
        else return null;
    }

    /**
     * Gets the type facet results for a given field.
     * @return A {@link TermFacetResult} with a set of facets of type T and the count of documents matching each one.
     */
    public TermFacetResult<String> getTypeFacet() {
        final Object ret = this.typeFacet;
        return Objects.nonNull(ret)? (TermFacetResult<String>) ret : null;
    }

    /**
     * Gets a query facet query result by facet name.
     * @param name String name of the query facet.
     * @param t Class type stored by the field.
     * @param <T> Field descriptor type.
     * @return A {@link QueryFacetResult}.
     */
    public <T> QueryFacetResult<T> getQueryFacet(String name, Class<T> t) {
        return queryFacets.containsKey(name) ? (QueryFacetResult<T>) queryFacets.get(name) : null; //TODO
    }

    /**
     * Gets a range facet query result by facet name.
     * @param name String name of the range facet.
     * @param t Class type stored by the field.
     * @param <T> Field descriptor type.
     * @return A {@link RangeFacetResult}.
     */
    public <T> RangeFacetResult<T> getRangeFacet(String name, Class<T> t) {
        return rangeFacets.containsKey(name) ? (RangeFacetResult<T>) rangeFacets.get(name) : null; //TODO
    }

    /**
     * Gets a interval facet query result by facet name.
     * @param name String name of the range facet.
     * @return A {@link IntervalFacetResult}.
     */
    public IntervalFacetResult getIntervalFacet(String name) { //TODO add type
        return intervalFacets.containsKey(name) ? intervalFacets.get(name) : null; //TODO
    }

    /**
     * Gets a stats facet query result by facet name.
     * @param name String name of the stats facet.
     * @param t Class type stored by the field.
     * @param <T> Field descriptor type.
     * @return A {@link StatsFacetResult}.
     */
    public <T> StatsFacetResult<T> getStatsFacet(String name, Class<T> t) {
        return statsFacets.containsKey(name) ? (StatsFacetResult<T>) statsFacets.get(name) : null; //TODO
    }
    /**
     * Gets a pivot facet query results by facet name.
     * @param name String name of the pivot facet.
     * @param t Class type stored by the field.
     * @param <T> Field descriptor type.
     * @return A list of {@link PivotFacetResult}.
     */
    public <T> List<PivotFacetResult<T>> getPivotsFacet(String name, Class<T> t) {
        return pivotFacets.containsKey(name) ? pivotFacets.get(name).stream()
                                                .map(pivot->(PivotFacetResult<T>)pivot)
                                                .collect(Collectors.toList()) : null; //TODO
    }

    /**
     * Get all the term facet results. This method is not typesave so please use it with care OR use @this#
     * @return a map of field descriptor with its term facets results.
     */
    public HashMap<FieldDescriptor, TermFacetResult<?>> getTermFacets() {
        return termFacets;
    }

    /**
     * Gets all the query facet results.
     * @return A map of name of facet and query facet result.
     */
    public HashMap<String, QueryFacetResult<?>> getQueryFacets() {
        return queryFacets;
    }
    /**
     * Gets all the range facet results.
     * @return A map of name of facet and range facet result.
     */
    public HashMap<String, RangeFacetResult<?>> getRangeFacets() {
        return rangeFacets;
    }
    /**
     * Gets all the stats facet results.
     * @return A map of name of facet an stats facet result.
     */
    public HashMap<String, StatsFacetResult<?>> getStatsFacets() {
        return statsFacets;
    }
    /**
     * Gets all the pivot facet results.
     * @return A map of name of facet an pivot facet result.
     */
    public HashMap<String, List<PivotFacetResult<?>>> getPivotFacets() {
        return pivotFacets;
    }

    public Collection<SubdocumentFacetResult> getSubdocumentFacets() {
        return subdocumentFacets;
    }
}
