package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.FulltextSearch;
import com.rbmhtechnology.vind.api.query.Search;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class VindQueryParser {
    private static final Logger log = LoggerFactory.getLogger(VindQueryParser.class);

    public FulltextSearch parse(String luceneQuery, DocumentFactory factory) throws IOException {
        final FulltextSearch vindQuery = Search.fulltext();

        try {
            final Query luceneQueryModel = parse(luceneQuery);
            Optional.ofNullable(luceneQueryModel.getText()).ifPresent(vindQuery::text);
            luceneQueryModel.forEach(q -> vindQuery.filter(q.toVindFilter(factory)));
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

}
