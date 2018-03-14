package com.rbmhtechnology.vind.monitoring.model.application;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 01.08.16.
 */
public class SimpleApplication implements Application {

    private String id;

    public SimpleApplication() {
    }

    public SimpleApplication(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
