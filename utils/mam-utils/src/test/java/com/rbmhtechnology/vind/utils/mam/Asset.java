package com.rbmhtechnology.vind.utils.mam;

import com.rbmhtechnology.vind.annotations.Entry;
import com.rbmhtechnology.vind.annotations.FullText;
import com.rbmhtechnology.vind.annotations.Id;
import com.rbmhtechnology.vind.annotations.Metadata;

import java.time.ZonedDateTime;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
public class Asset {

    @Id
    public String id;

    @FullText
    @Metadata(@Entry(name = RESTMetadataProvider.ID, value = "1319102420792-686346531"))
    public String title;

    @Metadata(@Entry(name = RESTMetadataProvider.ID, value = "1404728958802-98806344"))
    public ZonedDateTime created;

    public Asset() {
    }

    public Asset(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
