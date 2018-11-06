/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.api.query.facet;

/**
 * Created on 05.11.18.
 */
public abstract class TermFacetOption {

    private Integer offset = 0;
    private Integer limit;
    private String sort;
    private Integer overrequest = -1;
    private Boolean refine;
    private Integer overrefine = -1;
    private Integer mincount;
    private Boolean missing = false;
    private Boolean numBuckets = false;
    private Boolean allBuckets = false;
    private String prefix;
    private FacetMethod method = FacetMethod.SMART;

    public Integer getOffset() {
        return offset;
    }

    public TermFacetOption setOffset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public Integer getLimit() {
        return limit;
    }

    public TermFacetOption setLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public String getSort() {
        return sort;
    }

    public TermFacetOption setSort(String sort) {
        this.sort = sort;
        return this;
    }

    public Integer getOverrequest() {
        return overrequest;
    }

    public TermFacetOption setOverrequest(Integer overrequest) {
        this.overrequest = overrequest;
        return this;
    }

    public Boolean isRefine() {
        return refine;
    }

    public TermFacetOption setRefine(Boolean refine) {
        this.refine = refine;
        return this;
    }

    public Integer getOverrefine() {
        return overrefine;
    }

    public TermFacetOption setOverrefine(Integer overrefine) {
        this.overrefine = overrefine;
        return this;
    }

    public Integer getMincount() {
        return mincount;
    }

    public TermFacetOption setMincount(Integer mincount) {
        this.mincount = mincount;
        return this;
    }

    public Boolean isMissing() {
        return missing;
    }

    public TermFacetOption setMissing(Boolean missing) {
        this.missing = missing;
        return this;
    }

    public Boolean isNumBuckets() {
        return numBuckets;
    }

    public TermFacetOption setNumBuckets(Boolean numBuckets) {
        this.numBuckets = numBuckets;
        return this;
    }

    public Boolean isAllBuckets() {
        return allBuckets;
    }

    public TermFacetOption setAllBuckets(Boolean allBuckets) {
        this.allBuckets = allBuckets;
        return this;
    }

    public String getPrefix() {
        return prefix;
    }

    public TermFacetOption setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public FacetMethod getMethod() {
        return method;
    }

    public TermFacetOption setMethod(FacetMethod method) {
        this.method = method;
        return this;
    }

    enum FacetMethod {
        DV, UIF, DVHASH, ENUM, STREAM, SMART
    }
}
