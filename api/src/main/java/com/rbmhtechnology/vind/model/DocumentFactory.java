package com.rbmhtechnology.vind.model;

import com.google.common.base.Preconditions;
import com.rbmhtechnology.vind.api.Document;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.nonNull;

/**
 * Factory class build to instantiate {@link Document} with a common schema.
 * A Document factory
 */
public class DocumentFactory {

    private Logger log = LoggerFactory.getLogger(getClass());

    public static final String ID = "_id_";
    public static final String TYPE = "_type_";

    private final String type;

    private final boolean updatable;

    protected final Map<String, FieldDescriptor<?>> fields;

    /**
     * Creates a new instance of {@link DocumentFactory} with a given type name.
     * @param type type of the {@link Document} instantiated by this factory.
     * @param updatable flag to define a document factory which will produce documents suporting partial updates.
     * @param fields a {@link Map} of {@link String} field name and {@link FieldDescriptor} objects.
     */
    protected DocumentFactory(String type,boolean updatable, Map<String, FieldDescriptor<?>> fields) {
        this.type = type;
        this.updatable = updatable;
        this.fields = fields;
    }

    /**
     * Gets a copy of the list of fields configured in the document factory.
     * @return A collection of field descriptors.
     */
    public Collection<FieldDescriptor<?>> listFields() {
        // Document factory should be immutable, so we give a copy of the list in order to avoid modifications
        return Collections.unmodifiableCollection(fields.values());
    }



    /**
     * Checks whether the factory has an specific field or not.
     * @param name Name of the field to check for.
     * @return True if the field is defined in the factory or false otherwise.
     */
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    /**
     * Gets the {@link FieldDescriptor} of a specific field.
     * @param name Name of the field.
     * @return A {@link FieldDescriptor}.
     */
    public FieldDescriptor<?> getField(String name) {
        return fields.get(name);
    }

    //TODO: Documentation
    public Map<String, FieldDescriptor<?>> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    /**
     * Creates an instance of a {@link Document} within the schema of the actual configuration of the factory.
     *
     * @param id Identification string of the new document.
     * @return A {@link Document} with the configured {@link FieldDescriptor}.
     */
    public Document createDoc(String id) {
        return new DocumentImpl(id, this.type);
    }

    /**
     * Gets the type of the factory.
     * @return document factory type.
     */
    public String getType() {
        return type;
    }

    /**
     * Gets if the factory creates documents which support partial updates.
     * @return true if the documents are updatable, false otherwise.
     */
    public boolean isUpdatable() {
        return updatable;
    }

    @Override
    public String toString(){
        final String serialiceString = "{" +
                "\"type\":\"%s\"," +
                "\"updatable\":%s," +
                "\"fields\":%s" +
                "}";
        return String.format(serialiceString,
                this.type,
                this.updatable,
                MapUtils.isNotEmpty(this.fields)? "{"+this.fields.entrySet().stream()
                        .map(e ->"\""+e.getKey()+"\":"+e.getValue())
                        .collect(Collectors.joining(", ")) + "}" :this.fields
        );
    }

    /**
     * Implementation of the {@link Document} interface.
     */
    class DocumentImpl implements Document {
        private Logger log = LoggerFactory.getLogger(getClass());
        private final Map<String, Map<String, Object>> values = new HashMap<>();
        private final Map<String, String> context = new HashMap<>();
        private final Set<Document> children = new HashSet<>();
        private final String id;
        private final String type;
        private float score;
        private float distance;
        private Integer childCount;

        private DocumentImpl(String id, String type) {
            //this.values.put(DocumentFactory.ID, id);
            this.id = id;
            this.type = type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Collection<Document> getChildren() {
            return children; //TODO: unmodifieable?
        }

        @Override
        public boolean hasChildren() {
            return !children.isEmpty();
        }

        @Override
        @Deprecated
        public Document setChildren(Document... documents) {
            Preconditions.checkNotNull(documents);
            Preconditions.checkArgument(documents.length > 0);

            children.clear();
            addChild(documents);

            return this;
        }

        @Override
        public Document addChild(Document... document) {
            Preconditions.checkNotNull(document);
            Preconditions.checkArgument(document.length > 0);

            Collections.addAll(children, document);

            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document setValue(String field, Object value) {

            return setContextualizedValue(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document setContextualizedValue(String field, String context, Object value) {
            if (this.hasField(field)) {
                FieldDescriptor descriptor = this.listFieldDescriptors().get(field);
                checkField(field,value);
                Map<String, Object> fieldMap = this.values.get(field);
                if (fieldMap ==null) {
                    fieldMap = new HashMap<>();
                }
                if (descriptor.isMultiValue()) {
                    if(Collection.class.isAssignableFrom(value.getClass())) {
                        fieldMap.put(context,value);
                        this.values.put(field,fieldMap);
                    } else {
                        Collection<Object> validValues = new ArrayList<>();
                        validValues.add(value);
                        fieldMap.put(context, validValues);
                        this.values.put(field,fieldMap);
                    }
                } else {
                    fieldMap.put(context, value);
                    this.values.put(field,fieldMap);
                }
            } else {
                log.error("There is already a field defined with the same name: {}", field);
                throw new IllegalArgumentException("There is already a field defined with the same name: {}" + field);
            }

            //set field as contextualized.
            if (Objects.nonNull(context)) {
                fields.get(field).setContextualized(true);
            }

            // TODO
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setValue(FieldDescriptor<T> field, T value) {
            return this.setContextualizedValue(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setContextualizedValue(FieldDescriptor<T> field, String context, T value) {
            checkField(field.getName(), value);
            return this.setContextualizedValue(field.getName(), context, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document setValues(String field, Object... values) {
            return this.setContextualizedValues(field,null, values);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document setContextualizedValues(String field, String context, Object... values) {
            if (this.hasField(field)) {
                if (this.listFieldDescriptors().get(field).isMultiValue()) {
                    Collection<Object> validValues = new ArrayList<>();
                    for(Object value: values) {
                        checkField(field, value);
                        validValues.add(value);
                    }
                    Map<String, Object> fieldMap = this.values.get(field);
                    if (fieldMap ==null) {
                        fieldMap = new HashMap<>();
                    }
                    fieldMap.put(context,validValues);
                    this.values.put(field,fieldMap);
                } else {
                    log.error("Invalid operation: Field {} is not multivalued", field);
                    throw new IllegalArgumentException("Invalid operation: Field "+field+" is not multivalued");
                }
            } else {
                log.error("There is no such a field name [{}] for this document", field);
                throw new IllegalArgumentException("There is no such a field name [" + field+"] for this document");
            }

            //set field as contextualized.
            if (Objects.nonNull(context)) {
                fields.get(field).setContextualized(true);
            }

            // TODO
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setValues(MultiValueFieldDescriptor<T> field, T... value) {
            return this.setContextualizedValues(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setContextualizedValues(MultiValueFieldDescriptor<T> field, String context, T... value) {
            return this.setContextualizedValues(field.getName(), context, value);
        }

        @Override
        public <T> Document setContextualizedValues(MultiValuedComplexField<T, ?, ?> field, String context, T... value) {
            return this.setContextualizedValues(field.getName(), context, value);        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setValues(MultiValuedComplexField<T, ?, ?> field, T... value) {
            return this.setValues(field.getName(), value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document setValues(String field, Collection<?> values) {
            return this.setContextualizedValues(field, null, values);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document setContextualizedValues(String field, String context, Collection<?> values) {
            checkField(field, values);
            Map<String, Object> fieldMap = this.values.get(field);
            if (fieldMap ==null) {
                fieldMap = new HashMap<>();
            }
            fieldMap.put(context, values);
            this.values.put(field, fieldMap);

            //setting field as contextualized
            if (Objects.nonNull(context)) {
                fields.get(field).setContextualized(true);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setValues(MultiValueFieldDescriptor<T> field, Collection<T> value) {
            return this.setContextualizedValues(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setContextualizedValues(MultiValueFieldDescriptor<T> field, String context, Collection<T> value) {
            return this.setContextualizedValues(field.getName(), context, value);
        }

        @Override
        public <T> Document setContextualizedValues(MultiValuedComplexField<T, ?, ?> field, String context, Collection<T> value) {
            return this.setContextualizedValues(field.getName(), context, value);        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document setValues(MultiValuedComplexField<T, ?, ?> field, Collection<T> value) {
            return this.setValues(field.getName(), value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document clear(String field) {
            checkField(field, null);
            values.remove(field);
            context.remove(field);
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document clear(FieldDescriptor<?> field) {
            return this.clear(field.getName());
        }

        protected void checkField(String field, Object val) {

            if(field == null || ID.equals(field) || TYPE.equals(field)){
                log.error("Invalid field name {}: unable to modify the aforementioned field.", field);
                throw new IllegalArgumentException("Invalid field name "+field+": unable to modify the aforementioned field.");
            }
            if (!this.listFieldDescriptors().containsKey(field)) {
                log.error("The field {} does not exist in this factory",field);
                throw new IllegalArgumentException("The field " + field + " does not exist in this factory");
            } else if (val != null) {
                FieldDescriptor<?> fieldDescriptor = this.listFieldDescriptors().get(field);
                //Check if it is a multivalued parameter
                if(Collection.class.isAssignableFrom(val.getClass())) {
                    //Find elements in the collection which are not valid types
                    final Collection<Object> valueCollection = (Collection) val;

                    Optional notValidOptional;

                    if(ComplexFieldDescriptor.class.isAssignableFrom(fieldDescriptor.getClass())) {
                        Class storeType = ((ComplexFieldDescriptor) fieldDescriptor).getStoreType();
                        notValidOptional = valueCollection.stream()
                                .filter(t -> !(storeType.isAssignableFrom(t.getClass()) || fieldDescriptor.getType().isAssignableFrom(t.getClass())))
                                .findAny();
                    } else {
                        notValidOptional = valueCollection.stream()
                                .filter(t -> !fieldDescriptor.getType().isAssignableFrom(t.getClass()))
                                .findAny();
                    }
                    //check if the fieldDescriptor is set as multivalued and there are no invalid elements on the parameter collection
                    if (fieldDescriptor.isMultiValue()) {
                        if (notValidOptional.isPresent()) {
                            Object invalidVal = notValidOptional.get();
                            log.error("Value: '{}' of type [{}] is not assignable to field descriptor '{}' of type [{}]", invalidVal, invalidVal.getClass(), fieldDescriptor.getName(), fieldDescriptor.getType());
                            throw new IllegalArgumentException(MessageFormat.format(
                                    "Value: '{0}' of type [{1}] is not assignable to field descriptor '{2}' of type [{3}]",
                                    invalidVal, invalidVal.getClass(), fieldDescriptor.getName(), fieldDescriptor.getType()));
                        }
                    } else {
                        log.error("A collection can not be assigned to Field descriptor '{}', it is not a multivalued field", fieldDescriptor.getName());
                        throw new IllegalArgumentException("A collection can not be assigned to Field descriptor '" + fieldDescriptor.getName() +
                                "', it is not a multivalued field");
                    }

                }else{
                    if(ComplexFieldDescriptor.class.isAssignableFrom(fieldDescriptor.getClass())) {
                        if (!fieldDescriptor.getType().isAssignableFrom(val.getClass())){
                            Class storeType = ((ComplexFieldDescriptor) fieldDescriptor).getStoreType();
                            if (storeType != null && !(storeType.isAssignableFrom(val.getClass()) )) {
                                log.error("Value: '{}' of type [{}] is not assignable to field descriptor '{}' of type [{}]",val, val.getClass(), fieldDescriptor.getName(),storeType);
                                throw new IllegalArgumentException(MessageFormat.format(
                                        "Value: '{}' of type [{}] is not assignable to field descriptor '{}' of type [{}]",
                                        val, val.getClass(), fieldDescriptor.getName(), storeType));
                            }
                        }
                    } else
                    if (!fieldDescriptor.getType().isAssignableFrom(val.getClass())) {
                        log.error("Value: '{}' of type [{}] is not assignable to field descriptor '{}' of type [{}]",val, val.getClass(), fieldDescriptor.getName(),fieldDescriptor.getType());
                        throw new IllegalArgumentException(MessageFormat.format(
                                "Value: '{}' of type [{}] is not assignable to field descriptor '{}' of type [{}]",
                                val, val.getClass(), fieldDescriptor.getName(), fieldDescriptor.getType()));
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document addValue(String field, Object value) {
            return this.addContextualizedValue(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document addContextualizedValue(String field, String context, Object value) {
            checkField(field, value);
            FieldDescriptor fieldDescriptor = this.listFieldDescriptors().get(field);
            if (!fieldDescriptor.isMultiValue()) {
                log.error("Invalid operation: The field {} is not multivalued.", field);
                throw new IllegalArgumentException("Invalid operation: The field "+field+" is not multivalued.");
            }

            Map<String,Object> contexts = this.values.get(field);
            if (contexts == null) {
                contexts = new HashMap<>();
            }
            Collection<Object> values = (Collection) contexts.get(context);
            if (values == null) {
                values = new ArrayList<>();
            }
            values.add(value);
            contexts.put(context,values);
            this.values.put(field, contexts);

            if (Objects.nonNull(context)) {
                fieldDescriptor.setContextualized(true);
            }
            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document addValue(MultiValueFieldDescriptor<T> field, T value) {
            return this.addValue(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document addValue(MultiValueFieldDescriptor<T> field, String context, T value) {
            return this.addContextualizedValue(field.getName(), context, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document addValue(MultiValuedComplexField<T, ?, ?> field, T value) {
            return this.addValue(field.getName(), value);
        }

        @Override
        public <T> Document addContextualizedValue(MultiValueFieldDescriptor<T> field, String context, T value) {
            return this.addContextualizedValue(field.getName(),context,value);
        }

        @Override
        public <T> Document addContextualizedValue(MultiValuedComplexField<T, ?, ?> field, String context, T value) {
            return this.addContextualizedValue(field.getName(),context,value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document removeValue(String field, Object value) {
            return this.removeContextualizedValue(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Document removeContextualizedValue(String field, String context, Object value) {
            checkField(field, value);
            Map<String, Object> contextMap = this.values.get(field);
            Collection values = (Collection) contextMap.get(context);
            if (values == null) {
                return this;
            }
            values.remove(value);
            contextMap.put(context,values);
            this.values.put(field, contextMap);

            return this;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document removeValue(FieldDescriptor<T> field, T value) {
            return this.removeContextualizedValue(field, null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> Document removeContextualizedValue(FieldDescriptor<T> field, String context, T value) {
            return this.removeContextualizedValue(field.getName(), null, value);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getValue(String field) {
            return this.getContextualizedValue(field, null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object getContextualizedValue(String field, String context) {
            if (ID.equals(field)) return getId(); //TODO should this work?
            if (TYPE.equals(field)) return getType();//TODO should this work?
            checkField(field, null);
            Map<String, Object> contextMap = this.values.get(field);
            if (contextMap ==null) {
                return null;
            }
            return contextMap.get(context);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T getValue(FieldDescriptor<T> descriptor) {
            return (T) this.getContextualizedValue(descriptor, null);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T getContextualizedValue(FieldDescriptor<T> descriptor, String context) {
            String field = descriptor.getName();
            return (T) this.getContextualizedValue(field, context);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T getValue(String field, Class<T> clazz) {
          return this.getContextualizedValue(field,null,clazz);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> T getContextualizedValue(String field,String context, Class<T> clazz) {
            Object objectValue = this.getContextualizedValue(field, context);
            if (clazz.isAssignableFrom(objectValue.getClass())) {
                return clazz.cast(objectValue);
            } else {
                log.error("Incompatible types: Field type [{}] does not mach parameter type [{}]",objectValue.getClass(),clazz);
                throw new IllegalArgumentException("Incompatible types: Field type ["+objectValue.getClass()+"] does not mach parameter type ["+clazz+"]");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<String> getFieldContexts(String field) {
            checkField(field, null);
            return this.values.get(field).keySet();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Set<String> getFieldContexts(FieldDescriptor descriptor) {
            return this.getFieldContexts(descriptor.getName());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getId() {
            return this.id;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getType() {
            return type;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public float getDistance() {
            return distance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setDistance(float distance) {
            this.distance = distance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public float getScore() {
            return score;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setScore(float score) {
            this.score = score;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Integer getChildCount() {
            return childCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setChildCount(Integer childCount) {
            this.childCount = childCount;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasValue(String field) {
            return ID.equals(field) || TYPE.equals(field) || (nonNull(values.get(field)) && nonNull(values.get(field).values()));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public <T> boolean hasValue(FieldDescriptor<T> descriptor) {
            return hasValue(descriptor.getName());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean hasField(String fieldName) {
            return ID.equals(fieldName) || TYPE.equals(fieldName) || this.listFieldDescriptors().containsKey(fieldName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public FieldDescriptor<?> getFieldDescriptor(String fieldName) {
            return this.listFieldDescriptors().get(fieldName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, FieldDescriptor<?>> listFieldDescriptors() {
            return unmodifiableMap(DocumentFactory.this.fields);
        }

        @Override
        public Map<String, Object> getValues() {
            Map<String, Object> collect = this.values.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey, entry -> entry.getValue().get(null)));
            return collect;
        }

        @Override
        public String toString() {
            return "DocumentImpl{" +
                    "values=" + values +
                    ", id='" + getValue(DocumentFactory.ID, String.class) + '\'' +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
