package com.rbmhtechnology.vind.monitoring.model.session;

import com.rbmhtechnology.vind.monitoring.model.user.User;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 18.07.16.
 */
public class UserSession extends SimpleSession {

    private User user;

    public UserSession(String sessionId, User user) {
        super(sessionId);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getSessionId(){
        return user.getName();
    }

}
