package com.rbmhtechnology.vind.report.logger;

import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 13.07.16.
 */
public abstract class ReportWriter { //TODO could be an interface

    public static ReportWriter getInstance() {
        ServiceLoader<ReportWriter> loader = ServiceLoader.load(ReportWriter.class);
        final Iterator<ReportWriter> it = loader.iterator();
        final ReportWriter server;
        if (!it.hasNext()) {
            throw new RuntimeException("No ReportWriter in classpath");
        } else {
            server = it.next();
        }
        if (it.hasNext()) {
            LoggerFactory.getLogger(ReportWriter.class).warn("Multiple bindings for ReportWriter found: {}", loader.iterator());
        }
        return server;
    }

    public abstract void log(Log log);

}
