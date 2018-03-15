package com.rbmhtechnology.vind.monitoring.logger;

import com.rbmhtechnology.vind.monitoring.logger.entry.MonitoringEntry;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public abstract class MonitoringWriter { //TODO could be an interface

    public static MonitoringWriter getInstance() {
        ServiceLoader<MonitoringWriter> loader = ServiceLoader.load(MonitoringWriter.class);
        final Iterator<MonitoringWriter> it = loader.iterator();
        final MonitoringWriter server;
        if (!it.hasNext()) {
            throw new RuntimeException("No ReportWriter in classpath");
        } else {
            server = it.next();
        }
        if (it.hasNext()) {
            LoggerFactory.getLogger(MonitoringWriter.class).warn("Multiple bindings for ReportWriter found: {}", loader.iterator());
        }
        return server;
    }

    public abstract void log(MonitoringEntry log);
}
