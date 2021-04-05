package com.rbmhtechnology.vind.elasticsearch.backend.util;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.update.UpdateOperation;
import com.rbmhtechnology.vind.model.ComplexFieldDescriptor;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValuedComplexField;
import com.rbmhtechnology.vind.model.SingleValuedComplexField;
import org.apache.commons.lang3.NotImplementedException;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.rbmhtechnology.vind.elasticsearch.backend.util.DocumentUtil.toElasticType;

public class PainlessScript {

    private static final Logger log = LoggerFactory.getLogger(PainlessScript.class);

    private final List<Statement> scriptStatements = new ArrayList<>();
    public final HashMap<String, Object> scriptParameters = new HashMap<>();

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

        private static final String PAINLESS_ADD_TEMPLATE = "ctx._source[params.%s].addAll(params.%s)";
        private static final String PAINLESS_SET_TEMPLATE = "ctx._source[params.%s]=params.%s";
        private static final String PAINLESS_INC_TEMPLATE = "ctx._source[params.%s]+=params.%s";
        private static final String PAINLESS_REMOVE_ITEM_TEMPLATE = "ctx._source[params.%s].removeAll(params.%s)";
        private static final String PAINLESS_REMOVE_TEMPLATE = "ctx._source.remove(params.%s)";
        //private static final String PAINLESS_REMOVE_REGEX_TEMPLATE = "Pattern prefix = /%s/; ctx._source.%s.removeIf(item -> prefix. )";

        private final Operator op;
        private final String subject;
        private final Object predicate;
        private final Class<?> predicateType;
        private HashMap<String, Object> scriptParameters;

        private Statement(Operator op, String subject, Object predicate, Class<?> predicateType,HashMap<String,Object> scriptParameters) {
            this.op = op;
            this.subject = subject;
            this.predicate = predicate;
            this.predicateType = predicateType;
            this.scriptParameters = scriptParameters;
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

            final Object elasticPredicate = toElasticType(predicate);
            if(String.class.isAssignableFrom(elasticPredicate.getClass())) {
                return "'" + elasticPredicate.toString().replaceAll("'", "\\\\'") + "'";
            }
            if(Date.class.isAssignableFrom(elasticPredicate.getClass())) {
                final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return "'" + formatter.format(elasticPredicate).replaceAll("'", "\\\\'") + "'";
            }


            return   elasticPredicate.toString() ;
        }

        protected static Object getPredicate(Object predicate, Class<?> predicateType){

            if(Objects.isNull(predicate)){
                return null;
            }

            if (Collection.class.isAssignableFrom(predicate.getClass())) {
                final Object[] predicateArray = ((Collection<Object>)predicate).stream()
                        .map(pr -> Statement.getPredicate(pr, predicateType))
                        .filter(Objects::nonNull)
                        .toArray();
                if (predicateArray.length == 0) return null;
                return predicateArray;
            }

            if (predicate.getClass().isArray()) {
                final Object[] predicateArray = Stream.of((Object[]) predicate)
                        .map(pr -> Statement.getPredicate(pr, predicateType))
                        .filter(Objects::nonNull)
                        .toArray();
                if (predicateArray.length == 0) return null;
                return predicateArray;
            }

            final Object elasticPredicate = toElasticType(predicate);
            if(String.class.isAssignableFrom(elasticPredicate.getClass())) {
                return elasticPredicate.toString()/*.replaceAll("'", "\\\\'")*/;
            }

            return elasticPredicate ;
        }

        @Override
        public String toString() {
            switch (op){
                case add:
                    final String addSubjectParamName = "param_" + scriptParameters.size();
                    scriptParameters.put(addSubjectParamName, subject);
                    final String addPredicateParamName = "param_" + scriptParameters.size();
                    scriptParameters.put(addPredicateParamName, Statement.getPredicate(predicate, predicateType));
                    return String.format(PAINLESS_ADD_TEMPLATE, addSubjectParamName, addPredicateParamName);
                case inc:
                    final String incSubjectParamName = "param_" + scriptParameters.size();
                    scriptParameters.put(incSubjectParamName, subject);
                    final String incPredicateParamName = "param_" + scriptParameters.size();
                    scriptParameters.put(incPredicateParamName, Statement.getPredicate(predicate, predicateType));
                    return String.format(PAINLESS_INC_TEMPLATE, incSubjectParamName, incPredicateParamName);
                case set:
                    if(Objects.nonNull(predicate)) {
                        final String subjectParamName = "param_" + scriptParameters.size();
                        scriptParameters.put(subjectParamName, subject);
                        final String predicateParamName = "param_" + scriptParameters.size();
                        scriptParameters.put(predicateParamName, Statement.getPredicate(predicate, predicateType));
                        return String.format(PAINLESS_SET_TEMPLATE, subjectParamName, predicateParamName );
                    } else {
                        final String subjectParamName = "param_" + scriptParameters.size();
                        scriptParameters.put(subjectParamName, subject);
                        return String.format(PAINLESS_REMOVE_TEMPLATE, subjectParamName);
                    }
                case remove:
                    if(Objects.nonNull(predicate)) {
                        final String subjectParamName = "param_" + scriptParameters.size();
                        scriptParameters.put(subjectParamName, subject);
                        final String predicateParamName = "param_" + scriptParameters.size();
                        scriptParameters.put(predicateParamName, Statement.getPredicate(predicate, predicateType));
                        return String.format(PAINLESS_REMOVE_ITEM_TEMPLATE, subjectParamName, predicateParamName);
                    } else {
                        final String subjectParamName = "param_" + scriptParameters.size();
                        scriptParameters.put(subjectParamName, subject);
                        return String.format(PAINLESS_REMOVE_TEMPLATE, subjectParamName);
                    }
                case removeregex:
                default:
                    throw new NotImplementedException("Operation '" + op +"' is not supported");
            }
        }

    }

    public static class ScriptBuilder {
        private final PainlessScript painlessScript;

        public ScriptBuilder() {
            painlessScript = new PainlessScript();
        }

        public ScriptBuilder addOperations(FieldDescriptor<?> field, Map<String, SortedSet<UpdateOperation>> ops,
                                           List<String> indexFootPrint) {
           if(ComplexFieldDescriptor.class.isAssignableFrom(field.getClass())) {
               return addComplexFieldOperations((ComplexFieldDescriptor<? extends Object, ?, ?>) field,ops, indexFootPrint);
           }
           return addSimpleFieldOperations(field, ops, indexFootPrint);
        }

        private ScriptBuilder addSimpleFieldOperations(FieldDescriptor<?> field, Map<String,
                SortedSet<UpdateOperation>> ops, List<String> indexFootPrint) {
            ops.forEach((key, value) -> {
                FieldUtil.getFieldName(field, key, indexFootPrint)
                        .ifPresent( fieldName -> {
                            value.forEach(op ->{
                                checkValidPainlessSentence(field, op);
                                painlessScript.addSentence(
                                        new Statement(
                                                Operator.valueOf(op.getType().name()),
                                                fieldName,
                                                op.getValue(),
                                                field.getType(),
                                                painlessScript.scriptParameters));
                            });
                        });
            });
            return this;
        }

        public <T> ScriptBuilder addComplexFieldOperations(ComplexFieldDescriptor<T,?,?> descriptor, Map<String,
                SortedSet<UpdateOperation>> ops, List<String> indexFootPrint) {
            ops.forEach((key, value) -> {
                    for( FieldDescriptor.UseCase useCase : FieldDescriptor.UseCase.values()) {
                        FieldUtil.getFieldName(descriptor, useCase, key, indexFootPrint)
                                .ifPresent( name -> {
                                    Function<T, ? extends Object> useCaseFunction = null;
                                    Class<?> useCaseType = null;
                                    switch (useCase) {
                                        case Suggest:
                                            if(descriptor.isSuggest() && descriptor.getSuggestFunction() != null) {
                                                useCaseFunction = descriptor.getSuggestFunction();
                                                useCaseType = String.class;
                                            }
                                            break;
                                        case Facet:
                                            if(descriptor.isFacet() && descriptor.getFacetFunction() != null) {
                                                useCaseFunction = descriptor.getFacetFunction();
                                                useCaseType = descriptor.getFacetType();
                                            }
                                            break;
                                        case Filter:
                                            if(descriptor.isAdvanceFilter() && descriptor.getAdvanceFilter() != null) {
                                                useCaseFunction =  descriptor.getAdvanceFilter();
                                                useCaseType =  descriptor.getFacetType();
                                            }
                                            break;
                                        case Stored:
                                            if(descriptor.isStored() && descriptor.getStoreFunction() != null) {
                                                useCaseFunction = descriptor.getStoreFunction();
                                                useCaseType = descriptor.getStoreType();
                                            }
                                            break;
                                        case Sort:
                                            if(descriptor.isSort()){
                                                descriptor.getStoreType();
                                                if (descriptor.isMultiValue())
                                                    useCaseFunction = ((MultiValuedComplexField)descriptor).getSortFunction();

                                                else
                                                    useCaseFunction = ((SingleValuedComplexField)descriptor).getSortFunction();
                                            }
                                            break;
                                        case Fulltext:
                                            if(descriptor.isFullText() && descriptor.getFullTextFunction() != null) {
                                                useCaseFunction =  descriptor.getFullTextFunction();
                                                useCaseType = String.class;
                                            }
                                            break;
                                    }

                                    if( Objects.nonNull(useCaseFunction)) {
                                        final Function<T, ? extends Object> function = useCaseFunction;
                                        final Class<?> type = useCaseType;
                                        value.forEach(op ->{
                                            checkValidPainlessSentence(descriptor, op);
                                            painlessScript.addSentence(
                                                    new Statement(
                                                            Operator.valueOf(op.getType().name()),
                                                            name,
                                                            toElasticType(op.getValue(),descriptor, useCase),
                                                            type,
                                                            painlessScript.scriptParameters));
                                        });
                                    }
                                });
                    }

            });
            return this;
        }

        private Script build(Map<String, Object> parameters) {
            return new Script(
                    ScriptType.INLINE,
                    "painless",
                    painlessScript.toString(),
                    parameters);
        }

        public Script build() {
            return this.build(painlessScript.scriptParameters);
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
                        throw new NotImplementedException("Update operation '" + op +"' is not supported");
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


