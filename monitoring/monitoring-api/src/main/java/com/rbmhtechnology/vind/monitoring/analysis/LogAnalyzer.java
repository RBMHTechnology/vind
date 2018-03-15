/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.analysis;

import com.rbmhtechnology.vind.monitoring.logger.entry.MonitoringEntry;

import java.util.Collection;

/**
 * Created on 08.01.18.
 *
 * Abstract class to be implemented by any log analysis.
 */
public abstract class LogAnalyzer {

    public abstract String analyze(MonitoringEntry log);


    public abstract String analyze(Collection<MonitoringEntry> log);
}
