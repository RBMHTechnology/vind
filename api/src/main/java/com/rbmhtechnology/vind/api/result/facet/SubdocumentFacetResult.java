package com.rbmhtechnology.vind.api.result.facet;

/**
 * Class to store the subdocument facet response.
 *
 * @author Fonso
 * @since 23.06.17.
 */
public class SubdocumentFacetResult<String> implements FacetResult<String> {

    private String parentId;
    private Integer childrenCount;

    public SubdocumentFacetResult(String parentId, Integer childrenCount) {
        this.parentId = parentId;
        this.childrenCount = childrenCount;
    }

    public String getParentId() {
        return parentId;
    }

    public int getChildrenCount() {
        return childrenCount;
    }

}
