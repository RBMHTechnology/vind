package com.rbmhtechnology.vind.report.model.session;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public class SimpleSession implements Session {

    public String sessionId;

    public SimpleSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
