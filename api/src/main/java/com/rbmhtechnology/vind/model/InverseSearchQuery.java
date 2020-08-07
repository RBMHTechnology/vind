package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.api.query.inverseSearch.InverseSearchQueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.rbmhtechnology.vind.model.DocumentFactory.ID;
import static com.rbmhtechnology.vind.model.DocumentFactory.TYPE;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.nonNull;

public class InverseSearchQuery {
    private Logger log = LoggerFactory.getLogger(getClass());
    private final Filter query;
    private final Map<String, Object> values = new HashMap<>();
    private final String id;
    private final String type;
    private float score;
    private final DocumentFactory factory;

    protected InverseSearchQuery(String id, Filter query, DocumentFactory factory) {
        this.id = id;
        this.type = factory.getType();
        this.query = query;
        this.factory = factory;
        final byte[] serializedQuery;
        try {
            serializedQuery = serializeQuery(this.query);
        } catch (IOException e) {
            throw new RuntimeException("Error serializing query to byte []: " + e.getMessage(),e);
        }
        this.setValue(InverseSearchQueryFactory.BINARY_QUERY_FIELD, ByteBuffer.wrap(serializedQuery));
    }

    public static byte[] serializeQuery(Filter query) throws IOException
    {
        final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
        oos.writeObject(query);
        oos.flush();
        byte[] bytes = bytesOut.toByteArray();
        bytesOut.close();
        oos.close();
        return bytes;
    }

    public InverseSearchQuery setValue(String field, Object value) {
        if (this.hasField(field)) {
            FieldDescriptor descriptor = this.listFieldDescriptors().get(field);
            checkField(field,value);
            if (descriptor.isMultiValue()) {
                if(Collection.class.isAssignableFrom(value.getClass())) {
                    this.values.put(field,value);
                } else {
                    Collection<Object> validValues = new ArrayList<>();
                    validValues.add(value);
                    this.values.put(field,validValues);
                }
            } else {
                this.values.put(field,value);
            }
        } else {
            log.error("There is no such field name [{}] for this document", field);
            throw new IllegalArgumentException("There is no such field name [" + field + "] for this document");
        }
        return this;
    }

    public <T> InverseSearchQuery setValue(FieldDescriptor<T> field, T value) {
        checkField(field.getName(), value);
        return this.setValue(field.getName(), value);
    }

    public InverseSearchQuery setValues(String field, Object... values) {
        if (this.hasField(field)) {
            if (this.listFieldDescriptors().get(field).isMultiValue()) {
                Collection<Object> validValues = new ArrayList<>();
                for(Object value: values) {
                    checkField(field, value);
                    validValues.add(value);
                }
                this.values.put(field, validValues);
            } else {
                log.error("Invalid operation: Field {} is not multivalued", field);
                throw new IllegalArgumentException("Invalid operation: Field "+field+" is not multivalued");
            }
        } else {
            log.error("There is no such a field name [{}] for this document", field);
            throw new IllegalArgumentException("There is no such a field name [" + field+"] for this document");
        }
        return this;
    }

    public <T> InverseSearchQuery setValues(MultiValueFieldDescriptor<T> field, T... value) {
        return this.setValues(field, value);
    }

    public <T> InverseSearchQuery setValues(MultiValuedComplexField<T, ?, ?> field, T... value) {
        return this.setValues(field.getName(), value);
    }

    public InverseSearchQuery setValues(String field,  Collection<?> values) {
        checkField(field, values);
        this.values.put(field, values);
        return this;
    }

    public <T> InverseSearchQuery setValues(MultiValueFieldDescriptor<T> field, Collection<T> value) {
        return this.setValues(field, value);
    }

    public <T> InverseSearchQuery setContextualizedValues(MultiValuedComplexField<T, ?, ?> field, Collection<T> value) {
        return this.setValues(field.getName(), value);        }

    public <T> InverseSearchQuery setValues(MultiValuedComplexField<T, ?, ?> field, Collection<T> value) {
        return this.setValues(field.getName(), value);
    }

    public InverseSearchQuery clear(String field) {
        checkField(field, null);
        values.remove(field);
        return this;
    }

    public InverseSearchQuery clear(FieldDescriptor<?> field) {
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
                                "Value: ''{0}'' of type [{1}] is not assignable to field descriptor ''{2}'' of type [{3}]",
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
                                    "Value: ''{0}'' of type [{1}] is not assignable to field descriptor ''{2}'' of type [{3}]",
                                    val, val.getClass(), fieldDescriptor.getName(), storeType));
                        }
                    }
                } else
                if (!fieldDescriptor.getType().isAssignableFrom(val.getClass())) {
                    log.error("Value: '{}' of type [{}] is not assignable to field descriptor '{}' of type [{}]",val, val.getClass(), fieldDescriptor.getName(),fieldDescriptor.getType());
                    throw new IllegalArgumentException(MessageFormat.format(
                            "Value: ''{0}'' of type [{1}] is not assignable to field descriptor ''{2}'' of type [{3}]",
                            val, val.getClass(), fieldDescriptor.getName(), fieldDescriptor.getType()));
                }
            }
        }
    }

    public InverseSearchQuery addValue(String field, Object value) {
        checkField(field, value);
        final FieldDescriptor fieldDescriptor = this.listFieldDescriptors().get(field);
        if (!fieldDescriptor.isMultiValue()) {
            log.error("Invalid operation: The field {} is not multivalued.", field);
            throw new IllegalArgumentException("Invalid operation: The field "+field+" is not multivalued.");
        }

        Collection<Object> values = (Collection) this.values.get(field);
        if (values == null) {
            values = new ArrayList<>();
        }
        this.values.put(field, values);
        return this;
    }

    public <T> InverseSearchQuery addValue(MultiValueFieldDescriptor<T> field, T value) {
        return this.addValue(field, value);
    }

    public <T> InverseSearchQuery addValue(MultiValuedComplexField<T, ?, ?> field, T value) {
        return this.addValue(field.getName(), value);
    }

    public InverseSearchQuery removeValue(String field, Object value) {
        checkField(field, value);
        Collection values = (Collection) this.values.get(field);
        if (values == null) {
            return this;
        }
        values.remove(value);
        this.values.put(field, values);

        return this;
    }


    public <T> Document removeValue(FieldDescriptor<T> field, T value) {
        return this.removeValue(field, value);
    }

    public Object getValue(String field) {
        if (ID.equals(field)) return getId(); //TODO should this work?
        if (TYPE.equals(field)) return getType();//TODO should this work?
        checkField(field, null);
        return this.values.get(field);
    }

    public <T> T getValue(FieldDescriptor<T> descriptor) {
        String field = descriptor.getName();
        return (T) this.getValue(field);
    }

    public <T> T getValue(String field, Class<T> clazz) {
        Object objectValue = this.getValue(field);
        if (clazz.isAssignableFrom(objectValue.getClass())) {
            return clazz.cast(objectValue);
        } else {
            log.error("Incompatible types: Field type [{}] does not mach parameter type [{}]",objectValue.getClass(),clazz);
            throw new IllegalArgumentException("Incompatible types: Field type ["+objectValue.getClass()+"] does not mach parameter type ["+clazz+"]");
        }
    }

    public String getId() {
        return this.id;
    }

    public String getType() {
        return type;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public boolean hasValue(String field) {
        return ID.equals(field) || TYPE.equals(field) || (nonNull(values.get(field)));
    }

    public <T> boolean hasValue(FieldDescriptor<T> descriptor) {
        return hasValue(descriptor.getName());
    }

    public boolean hasField(String fieldName) {
        return ID.equals(fieldName) || TYPE.equals(fieldName) || this.listFieldDescriptors().containsKey(fieldName);
    }

    public FieldDescriptor<?> getFieldDescriptor(String fieldName) {
        return this.listFieldDescriptors().get(fieldName);
    }

    public Map<String, FieldDescriptor<?>> listFieldDescriptors() {
        return unmodifiableMap(factory.getInverseSearchMetaFields());
    }

    public Map<String, Object> getValues() {
        Map<String, Object> collect = this.values.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, entry -> entry.getValue()));
        return collect;
    }

    public DocumentFactory getFactory() {
        return factory;
    }

    public Filter getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return "DocumentImpl{" +
                "query= "+query+
                "metadata= " + values +
                ", id='" + getValue(DocumentFactory.ID, String.class) + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
