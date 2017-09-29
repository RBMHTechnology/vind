package com.rbmhtechnology.vind.api.result.facet;

/**
 * Class to store the subdocument facet response.
 *
 * @author Fonso
 * @since 23.06.17.
 */
public class SubdocumentFacetResult<String> implements FacetResult<String> {

    private Integer parentCount;
    private Integer childrenCount;

    public SubdocumentFacetResult(Integer parentCount, Integer childrenCount) {
        this.parentCount = parentCount;
        this.childrenCount = childrenCount;
    }

    public Integer getParentCount() {
        return parentCount;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

}
