package com.rbmhtechnology.vind.monitoring.report.preprocess;

import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public abstract class ReportPreprocessor {

    Logger logger = LoggerFactory.getLogger(ReportPreprocessor.class);

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

        List<String> sessionIds = getSessionIds();

        Stopwatch sw = Stopwatch.createStarted();

        int reportingSteps = 10;//on seconds

        final int[] reportingCount = {1};

        IntStream.range(0, sessionIds.size()).forEach(index -> {

            preprocessSession(sessionIds.get(index));

            if(sw.elapsed(TimeUnit.SECONDS)/reportingSteps > reportingCount[0]) {
                int percentage = (int)Math.floor(((double)index)/sessionIds.size() * 100);
                logger.info("{}% of sessions are preprocessed in {}s", percentage, reportingCount[0]*reportingSteps);
                ++reportingCount[0];
            }
        });

        afterPreprocessing();
    }

    void afterPreprocessing() { }

    public abstract Boolean preprocessSession(String sessionId);
}
