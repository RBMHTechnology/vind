package com.rbmhtechnology.vind.api.result.facet;

import java.util.List;

/**
 * Class to store the range facet response.
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 29.06.16.
 */
public class RangeFacetResult<T> implements FacetResult<T> {

    private List<FacetValue<T>> values;
    private T start;
    private T end;
    private Long gap;

    /**
     * Creates a new instance of{@link RangeFacetResult}.
     * @param values List of {@link FacetValue} with one element for each step on the range with matching values.
     * @param start Lower limit of the specified range.
     * @param end Higher limit of the specified range.
     * @param gap Size between the range steps.
     */
    public RangeFacetResult(List<FacetValue<T>> values, T start, T end, Long gap) {
        this.values = values;
        this.start = start;
        this.end = end;
        this.gap = gap;
    }

    /**
     * Gets the {@link FacetValue} result of the range facet query.
     * @return List of {@link FacetValue}.
     */
    public List<FacetValue<T>> getValues() {
        return values;
    }

    /**
     * Gets the starting point of the range.
     * @return T typed lower limit of the range.
     */
    public T getStart() {
        return start;
    }
    /**
     * Gets the ending point of the range.
     * @return T typed higher limit of the range.
     */
    public T getEnd() {
        return end;
    }
}
