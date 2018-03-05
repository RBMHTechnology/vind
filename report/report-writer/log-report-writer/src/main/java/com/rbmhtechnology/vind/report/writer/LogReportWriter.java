package com.rbmhtechnology.vind.report.writer;

import com.rbmhtechnology.vind.report.logger.ReportWriter;
import com.rbmhtechnology.vind.report.logger.entry.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 18.07.16.
 */
public class LogReportWriter extends ReportWriter {

    private static final Logger logger = LoggerFactory.getLogger(LogReportWriter.class);

    public LogReportWriter() {}

    @Override
    public void log(LogEntry log) {
        logger.info(log.toJson());
    }

}
