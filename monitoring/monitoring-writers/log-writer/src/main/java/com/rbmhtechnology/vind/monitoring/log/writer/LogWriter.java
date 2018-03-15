package com.rbmhtechnology.vind.monitoring.log.writer;

import com.rbmhtechnology.vind.monitoring.logger.MonitoringWriter;
import com.rbmhtechnology.vind.monitoring.logger.entry.MonitoringEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @author Alfonso Noriega Meneses
 * @since 18.07.16.
 */
public class LogWriter extends MonitoringWriter {

    private static final Logger logger = LoggerFactory.getLogger(LogWriter.class);

    public LogWriter() {}

    @Override
    public void log(MonitoringEntry log) {
        logger.info(log.toJson());
    }

}
