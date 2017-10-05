package com.rbmhtechnology.vind.demo.step5.guardian;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 06.07.16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GuardianJsonResponse {

    private GuardianNewsResponse response;

    public GuardianNewsResponse getResponse() {
        return response;
    }

    public void setResponse(GuardianNewsResponse response) {
        this.response = response;
    }

}
