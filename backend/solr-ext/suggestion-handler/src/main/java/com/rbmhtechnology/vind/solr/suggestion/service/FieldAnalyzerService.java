package com.rbmhtechnology.vind.solr.suggestion.service;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.core.SolrCore;

import java.io.IOException;
import java.io.StringReader;

/**
 * ...
 * <p/>
 * Author: Thomas Kurz (tkurz@apache.org)
 */
public class FieldAnalyzerService {

    /**
     * analyzes string like the given field
     * @param field the name of the field
     * @param value the string to analyze
     * @return the analyzed string
     */
    public static String analyzeString(SolrCore core, String field, String value) {
        try {
            StringBuilder b = new StringBuilder();
            try (TokenStream ts = core.getLatestSchema().getFieldType(field).getQueryAnalyzer().tokenStream(field, new StringReader(value))) {
                ts.reset();
                while (ts.incrementToken()) {
                    b.append(" ");
                    CharTermAttribute attr = ts.getAttribute(CharTermAttribute.class);
                    b.append(attr);
                }
            }

            return b.toString().trim();
        } catch (IOException e) {
            //FIXME: This error should be properly logged!
            e.printStackTrace();
            return value;
        }
    }

}
