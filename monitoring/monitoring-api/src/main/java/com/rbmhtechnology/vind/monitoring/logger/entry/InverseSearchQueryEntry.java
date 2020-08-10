
package com.rbmhtechnology.vind.monitoring.logger.entry;

import com.rbmhtechnology.vind.model.InverseSearchQuery;
import com.rbmhtechnology.vind.monitoring.model.application.Application;
import com.rbmhtechnology.vind.monitoring.model.response.Response;
import com.rbmhtechnology.vind.monitoring.model.session.Session;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Created on 10.08.20.
 */
public class InverseSearchQueryEntry extends MonitoringEntry {

    final private EntryType type = EntryType.indexInverseSearchQuery;
    private Application application;
    private Session session;
    private ZonedDateTime timeStamp;
    private Response response;
    private String inverseSearchQueryId;
    public InverseSearchQueryEntry() {
    }

    public InverseSearchQueryEntry(Application application, ZonedDateTime start, ZonedDateTime end, long queryTime, Session session, InverseSearchQuery queryDoc) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.inverseSearchQueryId = queryDoc.getId();
        this.response =  new Response(-1, queryTime, start.until(end, ChronoUnit.MILLIS ));
    }

    public InverseSearchQueryEntry(Application application, ZonedDateTime start, ZonedDateTime end, long queryTime, long elapsedTime, Session session, InverseSearchQuery queryDoc) {
        this.application = application;
        this.session = session;
        this.timeStamp = start;
        this.inverseSearchQueryId = queryDoc.getId();
        this.response =  new Response(-1, queryTime, start.until(end, ChronoUnit.MILLIS ));
        this.response.setElapsedTime(elapsedTime);
    }

    @Override
    public Application getApplication() {
        return application;
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public EntryType getType() {
        return type;
    }

    @Override
    public ZonedDateTime getTimeStamp() {
        return timeStamp;
    }

    public Response getResponse() {
        return response;
    }

    public String getInverseSearchQueryId() {
        return inverseSearchQueryId;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
