package com.rbmhtechnology.vind.api.query.filter.parser;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.DocumentFactory;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterLuceneParser implements FilterStringParser {
    private StreamTokenizer tokenizer;
    private final String FIELD_VALUE_PATTERN = "^(\\s*\\+?([\\w_]+):\\s*(.+)\\s*)$";


    @Override
    public Filter parse(String luceneQuery, DocumentFactory factory) throws IOException {

        final Pattern fieldValuePattern = Pattern.compile(FIELD_VALUE_PATTERN);
        final Matcher matcher = fieldValuePattern.matcher(luceneQuery);
        while (matcher.find()) {
            final String fieldName = matcher.group(2).trim();
            if (factory.hasField(fieldName)) {
                final FieldDescriptor<?> field = factory.getField(fieldName);
                final String value = matcher.group(3).trim();
                this.tokenizer =
                        new StreamTokenizer(new StringReader(value));
                final Node expression = parse();
                this.tokenizer=null;
                return expression.eval(fieldName);
            }
        }
        this.tokenizer = null;
        return null;
    }

    private Node parse() throws IOException {

        tokenizer.nextToken();
        Node result = parseExpression();
        return result;
    }

    private Node parseExpression() throws IOException {
        if(tokenizer.sval == null){
            Node left = parse();
            tokenizer.nextToken();
            BinaryOperationNode.Operator op = BinaryOperationNode.Operator.valueOf(tokenizer.sval);
            Node right = parse();
            tokenizer.nextToken();
            return new BinaryOperationNode(left, right, op);
        }
        if (tokenizer.sval.equals("NOT")) {
            return new NotNode(parse());
        }

        return new LiteralNode(tokenizer.sval);
    }

}

