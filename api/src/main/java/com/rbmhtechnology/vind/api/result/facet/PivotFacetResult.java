package com.rbmhtechnology.vind.api.result.facet;

import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.util.List;
import java.util.Map;

/**
 * Class to store the pivot facet response.
 *
 * @author Fonso
 * @since 08.07.16.
 */
public class PivotFacetResult<T> implements FacetResult<T> {

    private List<PivotFacetResult<?>> pivot;
    private Map<String,RangeFacetResult<?>> rangeSubfacets;
    private Map<String,QueryFacetResult<?>> querySubfacets;
    private Map<String,StatsFacetResult<?>> statsSubfacets;
    private T value;
    private FieldDescriptor field;
    private Integer count;

    /**
     * Create a new instance of {@link PivotFacetResult}.
     * @param pivot List of {@link PivotFacetResult} combinations with this instance.
     * @param value Value of this pivot result.
     * @param field {@link FieldDescriptor} field on which the pivot is done.
     * @param count {@link Integer} with the number of combinations this result has.
     * @param queries List of {@link QueryFacetResult} combined with this pivot result.
     * @param stats List of {@link StatsFacetResult} combined with this pivot result.
     * @param rangeSubfacets List of {@link RangeFacetResult} combined with this pivot result.
     */
    public PivotFacetResult(List<PivotFacetResult<?>> pivot, T value, FieldDescriptor<T> field, Integer count, Map<String,QueryFacetResult<?>> queries, Map<String,StatsFacetResult<?>> stats,Map<String,RangeFacetResult<?>> rangeSubfacets) {
        this.pivot = pivot;
        this.value = value;
        this.field = field;
        this.count = count;
        this.rangeSubfacets = rangeSubfacets;
        this.querySubfacets = queries;
        this.statsSubfacets = stats;
    }

    /**
     * Get the {@link PivotFacetResult} combinations with this result.
     * @return  List of {@link PivotFacetResult}.
     */
    public List<PivotFacetResult<?>> getPivot() {
        return pivot;
    }
    /**
     * Get the {@link RangeFacetResult} combinations with this result.
     * @return  List of {@link RangeFacetResult}.
     */
    public Map<String, RangeFacetResult<?>> getRangeSubfacets() {
        return rangeSubfacets;
    }
    /**
     * Get the {@link StatsFacetResult} combinations with this result.
     * @return  List of {@link StatsFacetResult}.
     */
    public Map<String, StatsFacetResult<?>> getStatsSubfacets() {
        return statsSubfacets;
    }
    /**
     * Get the {@link QueryFacetResult} combinations with this result.
     * @return  List of {@link QueryFacetResult}.
     */
    public Map<String, QueryFacetResult<?>> getQuerySubfacets() {
        return querySubfacets;
    }

    /**
     * Gets the value of this {@link PivotFacetResult}.
     * @return T typed value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Gets the {@link FieldDescriptor} of this {@link PivotFacetResult}.
     * @return {@link FieldDescriptor}
     */
    public FieldDescriptor getField() {
        return field;
    }

    /**
     * Gets the number of {@link PivotFacetResult} combined with this instance.
     * @return {@link Integer} count of combinations this result has.
     */
    public Integer getCount() {
        return count;
    }
}
