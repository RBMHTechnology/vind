package com.rbmhtechnology.vind.report.session;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 18.07.16.
 */
public class UserSession extends SimpleSession {

    private String userId, userName;

    public UserSession(String sessionId, String userId, String userName) {
        super(sessionId);
        this.userId = userId;
        this.userName = userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }
}
