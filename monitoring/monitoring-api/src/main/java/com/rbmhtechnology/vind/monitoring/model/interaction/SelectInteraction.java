/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.interaction;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created on 03.10.17.
 */
public class SelectInteraction implements Interaction{

    private String id;
    private final String TYPE = "select";

    public SelectInteraction(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @JsonProperty("action")
    @Override
    public String getType() {
        return TYPE;
    }
}
