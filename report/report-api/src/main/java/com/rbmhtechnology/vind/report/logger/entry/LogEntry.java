/*
 * Copyright (c) 2017 Redlink GmbH.
 */
package com.rbmhtechnology.vind.report.logger.entry;

import com.rbmhtechnology.vind.report.model.application.Application;
import com.rbmhtechnology.vind.report.model.request.SearchRequest;
import com.rbmhtechnology.vind.report.model.session.Session;

import java.time.ZonedDateTime;

/**
 * Created on 03.10.17.
 */
public interface LogEntry {

    Application getApplication();
    Session getSession();
    EntryType getType();
    ZonedDateTime getTimeStamp();
    SearchRequest getRequest();

    public enum EntryType {
        fulltext, suggestion, interaction
    }
}
