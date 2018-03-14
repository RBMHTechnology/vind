package com.rbmhtechnology.vind.demo.dynamic;

import com.rbmhtechnology.vind.api.SearchServer;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.result.SuggestionResult;
import com.rbmhtechnology.vind.log.writer.LogReportWriter;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.report.ReportingSearchServer;
import com.rbmhtechnology.vind.report.logger.ReportWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class SearchApplication {

    private static Logger log = LoggerFactory.getLogger("DEMO");

    public static void main(String... args) {
        log.info("Building Field-Descriptors");
        final FieldDescriptor<String> title = new FieldDescriptorBuilder()
                .setFullText(true)
                .setSuggest(true)
                .buildTextField("title");
        final MultiValueFieldDescriptor.TextFieldDescriptor<String> category = new FieldDescriptorBuilder()
                .setFullText(false)
                .setSuggest(true)
                .setFacet(true)
                .buildMultivaluedTextField("category");

        log.info("Building DocumentFactory");
        final DocumentFactory news = new DocumentFactoryBuilder("news")
                .addField(title)
                .addField(category)
                .build();


        //get an instance of a server (in this case a embedded solr server)
        try (SearchServer server = SearchServer.getInstance()) {
            final ReportWriter writer = new LogReportWriter();
            final ReportingSearchServer reportingSearchServer = new ReportingSearchServer(server, writer);
            log.info("Indexing some data");
            server.index(news.createDoc("1")
                            .setValue(title, "Headline 1")
                            .setValues(category, "Sport", "Economic")
            );
            server.index(news.createDoc("2")
                            .setValue(title, "Headline 2")
                            .setValues(category, "Arts")
            );
            server.commit();


            final SuggestionResult fullSuggest = reportingSearchServer.execute(Search.suggest().fields(title, category), news);
            log.info("Suggestion: {}", fullSuggest.get(title));
            log.info("Suggestion: {}", fullSuggest.get(category));

            final SuggestionResult titleSuggestions = reportingSearchServer.execute(Search.suggest().text("Ar").fields(title, category), news);
            log.info("Suggestion: {}", titleSuggestions.get(title));

        }

    }
}
