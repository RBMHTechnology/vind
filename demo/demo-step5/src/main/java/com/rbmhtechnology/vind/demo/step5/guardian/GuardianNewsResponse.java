package com.rbmhtechnology.vind.demo.step5.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianNewsResponse {

    private int pages;
    private List<GuardianNewsItem> results;

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public List<GuardianNewsItem> getResults() {
        return results;
    }

    public void setResults(List<GuardianNewsItem> results) {
        this.results = results;
    }

}
