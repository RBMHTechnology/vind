package com.rbmhtechnology.vind.monitoring.model;

import com.rbmhtechnology.vind.annotations.Facet;
import com.rbmhtechnology.vind.annotations.FullText;
import com.rbmhtechnology.vind.annotations.Id;
import com.rbmhtechnology.vind.annotations.Score;
import com.rbmhtechnology.vind.annotations.language.Language;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashSet;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class NewsItem {

    //the @id annotation is the only one, which is obligatory; the value should be unique
    @Id
    private String id;

    //the field should be treated as english text
    @FullText(language = Language.English)
    private String title;

    //we want to use this field for faceting and fulltext.
    //additionally we want to boost the value for fulltext a bit (default is 1)
    @FullText(language = Language.English, boost = 1.2f)
    @Facet
    private HashSet<String> category;

    //this field is 'just' a facet field
    @Facet
    private String kind;

    private ZonedDateTime created;

    //we want to have a look at the search score (which is internally used for ranking)
    //this field must be a float value and should not have a setter
    @Score
    private float score;

    //the empty constructor is necessary for internal purposes
    public NewsItem() {}

    public NewsItem(String id, String title, ZonedDateTime created, String kind, String ... categories) {
        this.id = id;
        this.title = title;
        this.created = created;
        this.kind = kind;
        this.category = new HashSet<>(Arrays.asList(categories));
    }

    public String getId() {
        return id;
    }

    public float getScore() {
        return score;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public HashSet<String> getCategory() {
        return category;
    }

    public void setCategory(HashSet<String> category) {
        this.category = category;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return "NewsItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category=" + category +
                ", kind='" + kind + '\'' +
                ", created=" + created +
                ", score=" + score +
                '}';
    }
}
