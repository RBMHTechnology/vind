/*
 * Copyright (c) 2018 Redlink GmbH.
 */
package com.rbmhtechnology.vind.monitoring.report.writer;

import com.rbmhtechnology.vind.monitoring.report.Report;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created on 01.03.18.
 */
public class HtmlReportWriter implements ReportWriter{

    public static final String DEFAULT_HTML_TEMPLATE = "report.ftlh";
    public static final String DEFAULT_ENCODING = "UTF-8";
    final private Configuration cfg;
    final private Template reportTemplate;

    public HtmlReportWriter() {

        this.cfg = new Configuration(Configuration.VERSION_2_3_23);

        this.cfg.setDefaultEncoding(DEFAULT_ENCODING);
        this.cfg.setTemplateExceptionHandler(TemplateExceptionHandler.HTML_DEBUG_HANDLER/*RETHROW_HANDLER*/);//TODO: update when finished
        this.cfg.setLogTemplateExceptions(false);

        this.reportTemplate = this.loadTemplate(this.getClass().getClassLoader().getResource(DEFAULT_HTML_TEMPLATE).getPath());
    }

    @Override
    public String write(Report report) {

        try (final StringWriter writer = new StringWriter()) {
            reportTemplate.process(report, writer);
            return writer.toString();
        } catch (TemplateException e) {
            throw
                new RuntimeException("Error populating default report HTML template '"+ reportTemplate.getName() +"': " + e.getMessage(), e);
        } catch (IOException e) {
            throw
                new RuntimeException("Error writing HTML template to string: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean write(Report report, String reportFile) {
        return this.write(report,reportFile, this.reportTemplate);
    }

    public boolean write(Report report, String reportFile, String freeMarkerTemplate) {

        final Template customTemplate = loadTemplate(freeMarkerTemplate);
        return this.write(report, reportFile, customTemplate);
    }

    private Template loadTemplate(String freeMarkerTemplate) {

        final Path templatePath = Paths.get(freeMarkerTemplate);
        final String templateFolder = templatePath.getParent().toString();

        try {
            this.cfg.setDirectoryForTemplateLoading(new File(templateFolder));
        } catch (IOException e) {
            throw
                    new RuntimeException("Error accessing template folder'"+ templateFolder +"': " + e.getMessage(), e);
        }

        final Template template;
        try {
            template = this.cfg.getTemplate(templatePath.getFileName().toString());
        } catch (IOException e) {
            throw
                    new RuntimeException("Error loading report HTML template '"+ templatePath.toString() +"': " + e.getMessage(), e);
        }

        return template;
    }

    private boolean write(Report report, String reportFile, Template freeMarkerTemplate) {

        try (FileWriter writer = new FileWriter(reportFile) ){
            try {
                freeMarkerTemplate.process(report, writer);
                return true;
            } catch (TemplateException e) {
                throw
                        new RuntimeException("Error populating report HTML template '"+ freeMarkerTemplate.getName() +"': " + e.getMessage(), e);
            } catch (IOException e) {
                throw
                        new RuntimeException("Error writing HTML template  to file '"+ reportFile +"': " + e.getMessage(), e);
            }

        } catch (IOException e) {
            throw
                    new RuntimeException("Error loading output file '"+ reportFile +"': " + e.getMessage(), e);
        }
    }
}
