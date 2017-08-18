package com.rbmhtechnology.vind.api.result.facet;

import java.util.List;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 28.07.16.
 */
public class IntervalFacetResult implements FacetResult<String> {

    private List<FacetValue<String>> values; //TODO values could include interval info

    public IntervalFacetResult(List<FacetValue<String>> values) {
        this.values = values;
    }

    public List<FacetValue<String>> getValues() {
        return values;
    }
}
