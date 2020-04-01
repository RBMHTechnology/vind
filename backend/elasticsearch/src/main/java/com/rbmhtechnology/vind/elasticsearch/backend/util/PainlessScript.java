package com.rbmhtechnology.vind.elasticsearch.backend.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PainlessScript {

    private final List<Sentence> scriptSentences = new ArrayList<>();

    private PainlessScript() {}

    protected PainlessScript addSentence(Sentence sentence) {
        scriptSentences.add(sentence);
        return this;
    }
    public List<Sentence> getScriptSentences() {
        return scriptSentences;
    }

    public String toString() {
        return scriptSentences.stream().map(Sentence::toString).collect(Collectors.joining(";"));
    }



    public static class Sentence {
        private final Operator op;
        private final String subject;
        private final Object predicate;
        private final Class<?> predicateType;

        private Sentence(Operator op, String subject, Object predicate, Class<?> predicateType) {
            this.op = op;
            this.subject = subject;
            this.predicate = predicate;
            this.predicateType = predicateType;
        }

        public Operator getOp() {
            return op;
        }

        public String getSubject() {
            return subject;
        }

        public Object getPredicate() {
            return predicate;
        }

        public Class<?> getPredicateType() {
            return predicateType;
        }

//        private String getStringPredicate(){
//            switch (predicateType.getSimpleName()) {
//
//            }
//        }
//        public String toString() {
//            return scriptSentences.stream().map(Sentence::toString).collect(Collectors.joining());
//        }

    }

    public static class ScriptBuilder {

        private static final String PAINLESS_SET_STRING_TEMPLATE = " ctx._source.%s = \"%s\"";
        private static final String PAINLESS_SET_NON_STRING_TEMPLATE = " ctx._source.%s = %s";

        private static final String PAINLESS_ADD_STRING_TEMPLATE = " ctx._source.%s.add(\"%s\")";
        private static final String PAINLESS_ADD_NON_STRING_TEMPLATE = " ctx._source.%s.add(%s)";
        private static final String PAINLESS_ADD_MULTI_STRING_TEMPLATE = " ctx._source.%s.addAll(%s)";

        private List<String> scriptUpdates = new ArrayList<>();


    }

    public static enum Operator {
        add, set, inc, remove, removeregex
    }
}


