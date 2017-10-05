package com.rbmhtechnology.vind.demo.step5.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.ZonedDateTime;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianNewsItem {

    private String id;

    private String webTitle;

    private String sectionName;

    private String type;

    private ZonedDateTime webPublicationDate;

    private String webUrl;

    public GuardianNewsItem() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWebTitle() {
        return webTitle;
    }

    public void setWebTitle(String webTitle) {
        this.webTitle = webTitle;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ZonedDateTime getWebPublicationDate() {
        return webPublicationDate;
    }

    public void setWebPublicationDate(ZonedDateTime webPublicationDate) {
        this.webPublicationDate = webPublicationDate;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

}
