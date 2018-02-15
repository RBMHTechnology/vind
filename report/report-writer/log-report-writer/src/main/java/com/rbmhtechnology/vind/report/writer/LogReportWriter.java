package com.rbmhtechnology.vind.report.writer;

import com.rbmhtechnology.vind.report.logger.Log;
import com.rbmhtechnology.vind.report.logger.ReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 18.07.16.
 */
public class LogReportWriter extends ReportWriter {

    Logger logger = LoggerFactory.getLogger(LogReportWriter.class);

    public LogReportWriter() {}

    @Override
    public void log(Log log) {
        logger.info(log.toJson());
    }

}
