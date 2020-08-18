package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.update.UpdateOperation;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PainlessScript {

    private static final Logger log = LoggerFactory.getLogger(PainlessScript.class);

    private final List<Statement> scriptStatements = new ArrayList<>();

    private PainlessScript() {
    }

    protected PainlessScript addSentence(Statement statement) {
        scriptStatements.add(statement);
        return this;
    }
    public List<Statement> getScriptStatements() {
        return scriptStatements;
    }

    public String toString() {
        return scriptStatements.stream()
                .map(Statement::toString)
                .collect(Collectors.joining(";"));
    }



    public static class Statement {

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

        private Statement(Operator op, String subject, Object predicate, Class<?> predicateType) {
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
                        .map(pr -> Statement.getStringPredicate(pr, predicateType))
                        .toArray());
            }

            if (predicate.getClass().isArray()) {
                return Arrays.toString(Stream.of((Object[]) predicate)
                        .map(pr -> Statement.getStringPredicate(pr, predicateType))
                        .toArray());
            }

            final Object elasticPredicate = DocumentUtil.toElasticType(predicate);
            if(String.class.isAssignableFrom(elasticPredicate.getClass())) {
                return "'" + elasticPredicate + "'";
            }
            if(Date.class.isAssignableFrom(elasticPredicate.getClass())) {
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return "'" + formatter.format(elasticPredicate) + "'";
            }


            return   elasticPredicate.toString() ;
        }

        @Override
        public String toString() {
            switch (op){
                case add:
                    return String.format(PAINLESS_ADD_TEMPLATE, subject, Statement.getStringPredicate(predicate, predicateType));
                case inc:
                    return String.format(PAINLESS_INC_TEMPLATE, subject, Statement.getStringPredicate(predicate, predicateType));
                case set:
                    if(Objects.nonNull(predicate)) {
                        return String.format(PAINLESS_SET_TEMPLATE, subject, Statement.getStringPredicate(predicate, predicateType));
                    } else {
                        return String.format(PAINLESS_REMOVE_TEMPLATE, subject);
                    }
                case remove:
                    if(Objects.nonNull(predicate)) {
                        return String.format(PAINLESS_REMOVE_ITEM_TEMPLATE, subject, Statement.getStringPredicate(predicate, predicateType));
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
                value.forEach(op ->{
                        checkValidPainlessSentence(field, op);
                        painlessScript.addSentence(
                                new Statement(
                                        Operator.valueOf(op.getType().name()),
                                        fieldName,
                                        op.getValue(),
                                        field.getType()));
                });
            });
            return this;
        }

        public Script build(Map<String, Object> parameters) {
            return new Script(
                    ScriptType.INLINE,
                    "painless",
                    painlessScript.toString(),
                    parameters);
        }

        public Script build() {
            final Map<String, Object> parameters = new HashMap<>();
            return this.build(parameters);
        }

        public void checkValidPainlessSentence(FieldDescriptor<?> field, UpdateOperation op) {
            final List<String> errors = new ArrayList<>();
            if( Objects.isNull(field)) {
                log.warn("Provided field must not be null");
                errors.add("Provided field must not be null");
            }

            if(!Objects.nonNull(op)){
                log.warn("Provided operation must not be null.");
                errors.add("Provided operation must not be null");
            }

            if(Objects.nonNull(op) && Objects.nonNull(field)) {
                if(!field.isUpdate()) {
                    log.warn("Provided field cannot be updated: field {} is not set as updatable", field.getName());
                    errors.add(String.format(
                            "Provided field cannot be updated: field %s is not set as updatable",
                            field.getName()));
                }

                switch (op.getType()) {
                    case set:
                        break;
                    case inc:
                        if(!Number.class.isAssignableFrom(field.getType())) {
                            log.warn("Provided field cannot be increased: field {} is not number based", field.getName());
                            errors.add(String.format(
                                    "Provided field cannot be increased: field %s is not number based",
                                    field.getName()));
                        }
                        if(field.isMultiValue()) {
                            log.warn("Provided field cannot be increased: field {} must not be multivalued" , field.getName());
                            errors.add(String.format(
                                    "Provided field cannot be increased: field %s must not be multivalued",
                                    field.getName()));
                        }
                        break;
                    case add:
                        if(!field.isMultiValue()) {
                            log.warn("Provided field cannot be added values: field {} is not multivalued", field.getName());
                            errors.add(String.format(
                                    "Provided field cannot be added values: field %s is not multivalued",
                                    field.getName()));
                        }
                        break;
                    case remove:
                        if(Objects.nonNull(op.getValue())){
                            if(!field.isMultiValue()) {
                                log.warn("Provided field cannot be removed values: field {} is not multivalued" , field.getName());
                                errors.add(String.format(
                                        "Provided field cannot be removed values: field %s is not multivalued",
                                        field.getName()));
                            }
                        }
                        break;
                    case removeregex:
                        //TODO: find a way to implement removeregex in painless script
                    default:
                        throw new NotImplementedException();
                }
            }

            if(!errors.isEmpty()) {
                final String errorMsg = errors.stream()
                        .collect(Collectors.joining(", "));
                throw new SearchServerException(String.format("Invalid update operation: %s",errorMsg));
            }
        }
    }

    public enum Operator {
        add, set, inc, remove, removeregex
    }
}


