package com.rbmhtechnology.vind.test;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by fonso on 14.02.17.
 */
public class Taxonomy implements Serializable{

    private String term;
    private Integer id;
    private String label;
    private ZonedDateTime date;
    private List<String> synonyms =  new ArrayList<>();

    public Taxonomy(String term, Integer id, String label,ZonedDateTime date) {
        this.term = term;
        this.id = id;
        this.label = label;
        this.date = date;
    }

    public Taxonomy(String term, Integer id, String label,ZonedDateTime date, List<String> synonyms) {
        this.term = term;
        this.id = id;
        this.label = label;
        this.date = date;
        this.synonyms = synonyms;
    }

    public String getTerm() {
        return term;
    }

    public Integer getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public ZonedDateTime getDate() {
        return date;
    }
    public Date getUtilDate() {
        return Date.from(date.toInstant());
    }

    public List<String> getSynonyms() {
        return synonyms;
    }
}
