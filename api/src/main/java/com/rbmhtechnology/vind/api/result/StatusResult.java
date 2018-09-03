package com.rbmhtechnology.vind.api.result;

import java.util.HashMap;
import java.util.Map;

public class StatusResult {

    public enum Status {
        DOWN, UP
    }

    private Status status;

    private HashMap<String,Object> details;

    private StatusResult(Status status) {
        this.status = status;
        this.details = new HashMap<>();
    }

    public static StatusResult up() {
        return new StatusResult(Status.UP);
    }

    public static StatusResult down() {
        return new StatusResult(Status.DOWN);
    }

    public Status getStatus() {
        return status;
    }

    public StatusResult setDetail(String name, Object value) {
        details.put(name, value);
        return this;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

}
