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

    void beforePreprocessing(boolean force) {};

    abstract List<String> getSessionIds();

    public void preprocess(boolean force){
        beforePreprocessing(force);
        getSessionIds().forEach(s -> this.preprocessSession(s, force));
        afterPreprocessing(force);
    }

    void afterPreprocessing(boolean force) { }

    public abstract Boolean preprocessSession(String sessionId, boolean force);
}
