package com.rbmhtechnology.vind.monitoring.model.session;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public class SimpleSession implements Session {

    public String sessionId;

    public SimpleSession(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String getSessionId() {
        return sessionId;
    }
}
