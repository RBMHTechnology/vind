/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.model.user;

/**
 * Created on 02.10.17.
 */
public class User {

    private String name;
    private String id;
    private String contact;

    public User(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public User(String name, String id, String contact) {
        this.name = name;
        this.id = id;
        this.contact = contact;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public String getContact() {
        return contact;
    }
}
