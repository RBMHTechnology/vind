package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.api.query.update.UpdateOperation;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PainlessScript {

    private final List<Sentence> scriptSentences = new ArrayList<>();

    private PainlessScript() {
    }

    protected PainlessScript addSentence(Sentence sentence) {
        scriptSentences.add(sentence);
        return this;
    }
    public List<Sentence> getScriptSentences() {
        return scriptSentences;
    }

    public String toString() {
        return scriptSentences.stream()
                .map(Sentence::toString)
                .collect(Collectors.joining(";"));
    }



    public static class Sentence {

        private static final String PAINLESS_ADD_TEMPLATE = "ctx._source.%s.addAll(%s)";
        private static final String PAINLESS_SET_TEMPLATE = "ctx._source.%s=%s";
        private static final String PAINLESS_INC_TEMPLATE = "ctx._source.%s+=%s";
        private static final String PAINLESS_REMOVE_ITEM_TEMPLATE = "ctx._source.%s.removeAll(%s)";
        private static final String PAINLESS_REMOVE_TEMPLATE = "ctx._source.remove(\"%s\")";
        //private static final String PAINLESS_REMOVE_REGEX_TEMPLATE = "Pattern prefix = /%s/; ctx._source.%s.removeIf(item -> prefix. )";

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

        protected static String getStringPredicate(Object predicate, Class<?> predicateType){

            if(Objects.isNull(predicate)){
                return "";
            }

            if (Collection.class.isAssignableFrom(predicate.getClass())) {
                return Arrays.toString(((Collection<Object>) predicate).stream()
                        .map(pr -> Sentence.getStringPredicate(pr, predicateType))
                        .toArray());
            }

            if (predicate.getClass().isArray()) {
                return Arrays.toString(Stream.of((Object[]) predicate)
                        .map(pr -> Sentence.getStringPredicate(pr, predicateType))
                        .toArray());
            }

            if(String.class.isAssignableFrom(predicateType)) {
                return "\"" + predicate + "\"";
            }
            return predicate.toString();
        }

        @Override
        public String toString() {
            switch (op){
                case add:
                    return String.format(PAINLESS_ADD_TEMPLATE, subject, Sentence.getStringPredicate(predicate, predicateType));
                case inc:
                    return String.format(PAINLESS_INC_TEMPLATE, subject, Sentence.getStringPredicate(predicate, predicateType));
                case set:
                    if(Objects.nonNull(predicate)) {
                        return String.format(PAINLESS_SET_TEMPLATE, subject, Sentence.getStringPredicate(predicate, predicateType));
                    } else {
                        return String.format(PAINLESS_REMOVE_TEMPLATE, subject);
                    }
                case remove:
                    if(Objects.nonNull(predicate)) {
                        return String.format(PAINLESS_REMOVE_ITEM_TEMPLATE, subject, Sentence.getStringPredicate(predicate, predicateType));
                    } else {
                        return String.format(PAINLESS_REMOVE_TEMPLATE, subject);
                    }
                case removeregex:
                default:
                    throw new NotImplementedException();
            }
        }

    }

    public static class ScriptBuilder {

        private final PainlessScript painlessScript;

        public ScriptBuilder() {
            painlessScript = new PainlessScript();
        }

        public ScriptBuilder addOperations(FieldDescriptor<?> field, Map<String, SortedSet<UpdateOperation>> ops) {
            ops.forEach((key, value) -> {
                final String fieldName = FieldUtil.getFieldName(field, key);
                value.forEach(op ->
                        painlessScript.addSentence(
                                new Sentence(
                                        Operator.valueOf(op.getType().name()),
                                        fieldName,
                                        op.getValue(),
                                        field.getType())));
            });
            return this;
        }

        public Script build() {

            final Map<String, Object> parameters = new HashMap<>();

            return new Script(
                    ScriptType.INLINE,
                    "painless",
                    painlessScript.toString(),
                    parameters);
        }
    }

    public enum Operator {
        add, set, inc, remove, removeregex
    }
}


