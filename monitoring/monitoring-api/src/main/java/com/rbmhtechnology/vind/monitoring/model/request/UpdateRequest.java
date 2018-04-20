/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.request;

import com.rbmhtechnology.vind.api.query.update.Update;
import com.rbmhtechnology.vind.api.query.update.UpdateOperation;

import java.util.HashMap;
import java.util.SortedSet;

/**
 * Created on 02.10.17.
 */
public class UpdateRequest  {

    private Update update;

    private String documentId;
    private HashMap<String,HashMap<String,SortedSet<UpdateOperation>>> updateActions = new HashMap<>();
    private String context;


    public UpdateRequest(Update update) {
        this.update = update;
        this.documentId = update.getId();
        this.context = update.getUpdateContext();

        update.getOptions().entrySet().stream().
                forEach(entry ->  this.updateActions.put(entry.getKey().getName(),entry.getValue()));
    }

    public String getDocumentId() {
        return documentId;
    }

    public HashMap<String, HashMap<String, SortedSet<UpdateOperation>>> getUpdateActions() {
        return updateActions;
    }

    public String getContext() {
        return context;
    }
}
