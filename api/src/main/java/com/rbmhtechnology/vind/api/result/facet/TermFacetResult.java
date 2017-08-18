package com.rbmhtechnology.vind.api.result.facet;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to store the term facet response.
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 27.06.16.
 */
public class TermFacetResult<T> implements FacetResult<T> {

    private List<FacetValue<T>> values;

    /**
     * Creates a new instance of {@link TermFacetResult}.
     */
    public TermFacetResult() {
        this.values = new ArrayList<>();
    }

    /**
     * Creates a new instance of {@link TermFacetResult}.
     * @param values List of {@link FacetValue} containing the term facet results.
     */
    public TermFacetResult(List<FacetValue<T>> values) {
        this.values = values;
    }

    /**
     * Gets the list of {@link FacetValue}.
     * @return List of {@link FacetValue}.
     */
    public List<FacetValue<T>> getValues() {
        return values;
    }

    public TermFacetResult<T> addFacetValue(FacetValue<T> value) {
        values.add(value);
        return this;
    }

    @Override
    public String toString() {
        return "TermFacetResult" + values.toString();
    }
}
