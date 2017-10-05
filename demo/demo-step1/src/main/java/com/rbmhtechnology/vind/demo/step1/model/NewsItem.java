package com.rbmhtechnology.vind.demo.step1.model;

import com.rbmhtechnology.vind.annotations.FullText;
import com.rbmhtechnology.vind.annotations.Id;

import java.time.ZonedDateTime;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class NewsItem {

    //the @id annotation is the only one, which is obligatory; the value should be unique
    @Id
    private String id;

    //the fulltext annotation means: 'use this for fulltext search'
    @FullText
    private String title;

    private ZonedDateTime created;

    //the empty constructor is necessary for internal purposes
    public NewsItem() {}

    public NewsItem(String id, String title, ZonedDateTime created) {
        this.id = id;
        this.title = title;
        this.created = created;
    }

    public String getId() {
        return id;
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
}
