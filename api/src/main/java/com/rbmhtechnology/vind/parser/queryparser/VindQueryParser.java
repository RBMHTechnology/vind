package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.model.DocumentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VindQueryParser {
    private static final Logger log = LoggerFactory.getLogger(VindQueryParser.class);
    private boolean strict = true;

    public FulltextSearch parse(String luceneQuery, DocumentFactory factory) {
        final FulltextSearch vindQuery = Search.fulltext();

        try {
            final Query luceneQueryModel = parse(luceneQuery);
            luceneQueryModel.forEach(q -> {
                try {
                    vindQuery.filter(q.toVindFilter(factory));
                }catch (SearchServerException e) {
                    if (strict) {
                        log.error("Error parsing lucene query [{}] to Vind query: Unable to create Vind filter out of" +
                                " input clause [{}]" , luceneQuery, e.getMessage(),e);
                        throw new SearchServerException("Error parsing lucene query ["+luceneQuery+"] to Vind query: " +
                                e.getMessage(), e);
                    } else {
                        log.info("Unable to create Vind filter out of input clause [{}]", e.getMessage());
                    }
                    luceneQueryModel.addText(q.toString());
                }
            });
            Optional.ofNullable(luceneQueryModel.getText()).ifPresent(vindQuery::text);
        } catch (ParseException e) {
            log.error("Error parsing lucene query [{}] to Vind query: {}", e.getMessage(), luceneQuery);
            throw new SearchServerException("Error parsing lucene query ["+luceneQuery+"] to Vind query: " + e.getMessage(),e);
        }
        return vindQuery;
    }

    private Query parse(String s) throws ParseException {
        QueryParser parser = new QueryParser(toStream(s), StandardCharsets.UTF_8);
        return parser.run();
    }

    private InputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes());
    }

    public static Logger getLog() {
        return log;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }
}
