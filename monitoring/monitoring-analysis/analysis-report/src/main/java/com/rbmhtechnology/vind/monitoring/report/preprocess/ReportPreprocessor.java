package com.rbmhtechnology.vind.monitoring.report.preprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class ReportPreprocessor {

    List<String> systemFieldFilters = new ArrayList<>();

    public ReportPreprocessor addSystemFilterField(String ... fields) {
        if (Objects.nonNull(fields)) {
            systemFieldFilters.addAll(Arrays.asList(fields));
        }
        return this;
    }

    void beforePreprocessing() {};

    abstract List<String> getSessionIds();

    public void preprocess(){
        beforePreprocessing();
        getSessionIds().forEach(this::preprocessSession);
        afterPreprocessing();
    }

    void afterPreprocessing() { }

    public abstract Boolean preprocessSession(String sessionId);
}
