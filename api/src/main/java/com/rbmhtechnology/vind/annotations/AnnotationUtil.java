package com.rbmhtechnology.vind.annotations;

import com.rbmhtechnology.vind.annotations.util.FunctionHelpers;
import com.rbmhtechnology.vind.api.Document;
import com.rbmhtechnology.vind.model.*;
import com.rbmhtechnology.vind.model.value.LatLng;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.rbmhtechnology.vind.annotations.ComplexField.*;

/**
 * This class provides the means to generate {@link DocumentFactory}, {@link Document} and {@link FieldDescriptor} objects from annotated
 * java classes/objects and the opposite.
 * */
public class AnnotationUtil {

    private static Logger log = LoggerFactory.getLogger(AnnotationUtil.class);

    /**
     * TODO duplicate code with #createDocument
     * Creates a DocumentFactory from a given annotated class.
     * @param clazz Class from which an instance of DocumentFactory will be created.
     * @param <T> class type of the document factory.
     * @return DocumentFactory based on the fields from the given Class.
     */
    public static <T> DocumentFactory createDocumentFactory(Class<T> clazz) {
        final String typeVal = getType(clazz);
        final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder(typeVal);

        for (Field field : getFields(clazz)) {
            final FieldDescriptor fd = createFieldDescriptor(field);
            if (fd == null) continue;

            field.setAccessible(true);
            docFactoryBuilder.addField(fd);
        }

        return docFactoryBuilder.build();
    }

    /**
     * Creates a new Document based on the given Object.
     * @param pojo Object from which values an instance of Document will be created.
     * @return Document based on the fields and values from the given pojo Object.
     */
    public static Document createDocument(Object pojo) {
        try {
            final Class<?> pojoClass = pojo.getClass();

            final Field idField = getIdField(pojoClass);
            final String typeVal = getType(pojoClass);

            final DocumentFactoryBuilder docFactoryBuilder = new DocumentFactoryBuilder(typeVal);

            final Map<FieldDescriptor, Object> valueCache = new HashMap<>();
            for (Field field : getFields(pojoClass)) {
                final FieldDescriptor fd = createFieldDescriptor(field);
                if (fd == null) continue;

                field.setAccessible(true);
                final Object val = field.get(pojo);
                docFactoryBuilder.addField(fd);
                valueCache.put(fd, val);
            }

            final DocumentFactory docFactory = docFactoryBuilder.build();
            final Id id = idField.getAnnotation(Id.class);
            final String composedId = id.generator().newInstance().compose((String) idField.get(pojo), idField, pojoClass);

            final Document doc = docFactory.createDoc(composedId);

            // FIXME: Does this work as expected with collections?
            valueCache.forEach((key, val) -> {
                if (key.isMultiValue())
                    doc.setValues(key.getName(), (Collection) val);
                else
                    doc.setValue(key.getName(), val);
            });

            return doc;
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Unable to create Document from pojo", e);
            throw new RuntimeException("Unable to create Document from pojo", e);
        }
    }

    /**
     * Returns the Fields of a given class, including the inherited ones.
     * @param pojoClass  Class from which the fields are wanted to be known.
     * @return Array of Fields which belong to the given class
     */
    private static Field[] getFields(Class<?> pojoClass) {
        if (pojoClass == null) return new Field[0];
        else return ArrayUtils.addAll(pojoClass.getDeclaredFields(), getFields(pojoClass.getSuperclass()));
    }

    /**
     * Returns the type of a given Class.
     * @param pojoClass  Class from which it is wanted to know the type.
     * @return String describing the type of the class if the @Type annotation empty.If not the simple name of the class
     */
    private static String getType(Class<?> pojoClass) {
        String typeVal;
        final Type type = pojoClass.getAnnotation(Type.class);
        if (type != null && StringUtils.isNotBlank(type.name())) {
            typeVal = type.name();
        } else {
            typeVal = pojoClass.getSimpleName();
        }
        return typeVal;
    }

    /**
     * Builds a new FieldDescriptor object based on a given Class Field
     * @param field Field a Class used as base to create a new FieldDescriptor.
     * @return new FieldDescriptor build based on the Field parameter annotations.
     */
    private static FieldDescriptor createFieldDescriptor(Field field) {
        if (field.isAnnotationPresent(Ignore.class)) return null;
        if (field.isAnnotationPresent(Id.class)) return null;
        if (field.isAnnotationPresent(Score.class)) return null;


        if (field.isAnnotationPresent(ComplexField.class)) {

            final String fieldName;
            final ComplexField cf = field.getAnnotation(ComplexField.class);
            if (StringUtils.isNotBlank(cf.name())) {
                fieldName = cf.name();
            } else {
                fieldName = field.getName();
            }

            Class<?> type = field.getType();
            boolean multiValue = false;
            if (Collection.class.isAssignableFrom(type)) {
                type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                multiValue = true;
            } else if (type.isPrimitive()) {
                type = ClassUtils.primitiveToWrapper(type);
            }

            final Operator storeOperator = cf.store();
            final Operator facetOperator = cf.facet();
            final Operator suggestionOperator = cf.suggestion();
            final Operator fullTextOperator = cf.fullText();
            final Operator filterOperator = cf.advanceFilter();
            final Operator sortOperator = cf.sort();


            final ComplexFieldDescriptorBuilder builder = new ComplexFieldDescriptorBuilder();

            Class<?> facetType = null;
            if (!NullFunction.class.isAssignableFrom(facetOperator.function())) {
                try {
                    facetType = facetOperator.returnType();
                    final Function facetLambda = ((FunctionHelpers.ParameterFunction)facetOperator.function().newInstance()).setParameters(Arrays.asList(facetOperator.fieldName()));
                    builder.setFacet(true, facetLambda);

                }catch (InstantiationException | IllegalAccessException e) {
                    log.error("Unable to find/access constructor method for function class [{}]", facetOperator.function().getName(), e);
                    throw new RuntimeException("Unable to find/access constructor method for function class ["+facetOperator.function().getName()+"]");
                }
            }

            Class<?> storeType = null;
            if (!NullFunction.class.isAssignableFrom(storeOperator.function())) {
                try {
                    storeType = storeOperator.returnType();
                    final Function storeLambda = ((FunctionHelpers.ParameterFunction)storeOperator.function().newInstance()).setParameters(Arrays.asList(storeOperator.fieldName()));
                    builder.setStored(true, storeLambda);
                }catch (InstantiationException | IllegalAccessException e) {
                    log.error("Unable to find/access constructor method for function class [{}]", storeOperator.function().getName(), e);
                    throw new RuntimeException("Unable to find/access constructor method for function class ["+storeOperator.function().getName()+"]");
                }
            }

            if (!NullFunction.class.isAssignableFrom(suggestionOperator.function())) {
                try {
                    final Function suggestLambda = ((FunctionHelpers.ParameterFunction)suggestionOperator.function().newInstance()).setParameters(Arrays.asList(suggestionOperator.fieldName()));
                    builder.setSuggest(true, suggestLambda);
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("Unable to find/access constructor method for function class [{}]", suggestionOperator.function().getName(), e);
                    throw new RuntimeException("Unable to find/access constructor method for function class ["+suggestionOperator.function().getName()+"]");
                }
            }

            if (!NullFunction.class.isAssignableFrom(fullTextOperator.function())) {
                try {
                    final Function fullTextLambda = ((FunctionHelpers.ParameterFunction)fullTextOperator.function().newInstance()).setParameters(Arrays.asList(fullTextOperator.fieldName()));
                    builder.setFullText(true, fullTextLambda);
                    builder.setLanguage(cf.language());
                    builder.setBoost(cf.boost());
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("Unable to find/access constructor method for function class [{}]", fullTextOperator.function().getName(), e);
                    throw new RuntimeException("Unable to find/access constructor method for function class ["+fullTextOperator.function().getName()+"]");
                }
            }

            if (!NullFunction.class.isAssignableFrom(filterOperator.function())) {
                try {
                    final Function filterLambda = ((FunctionHelpers.ParameterFunction)filterOperator.function().newInstance()).setParameters(Arrays.asList(filterOperator.fieldName()));
                    builder.setAdvanceFilter(true, filterLambda);
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("Unable to find/access constructor method for function class [{}]", filterOperator.function().getName(), e);
                    throw new RuntimeException("Unable to find/access constructor method for function class ["+filterOperator.function().getName()+"]");
                }
            }

            //set metadata
            final Metadata metadata = field.getAnnotation(Metadata.class);
            if (metadata != null) {
                for (Entry entry : metadata.value()) {
                    builder.putMetadata(entry.name(), entry.value());
                }
            }

            return buildComplex(builder,fieldName, type,facetType,storeType,multiValue,sortOperator.function());

        }

        final String fieldName;
        final com.rbmhtechnology.vind.annotations.Field f = field.getAnnotation(com.rbmhtechnology.vind.annotations.Field.class);
        if (f != null && StringUtils.isNotBlank(f.name())) {
            fieldName = f.name();
        } else {
            fieldName = field.getName();
        }

        Class<?> type = field.getType();
        boolean multiValue = false;
        if (Collection.class.isAssignableFrom(type)) {
            type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
            multiValue = true;
        } else if (type.isPrimitive()) {
            type = ClassUtils.primitiveToWrapper(type);
        }

        final FieldDescriptorBuilder builder = new FieldDescriptorBuilder();

        if (f != null) {
            builder.setStored(f.stored());
            builder.setIndexed(f.indexed());
        } else {
            //Should not be needed, it has this value as default
            builder.setStored(true);
            builder.setIndexed(true);
        }

        final FullText fullText = field.getAnnotation(FullText.class);
        if (fullText != null) {
            if (!CharSequence.class.isAssignableFrom(type)) {
                log.error("@FullText only allowed on CharSequence or String fields");
                throw new IllegalArgumentException("@FullText only allowed on CharSequence or String fields");
            }
            builder.setFullText(true);
            builder.setLanguage(fullText.language());
            builder.setBoost(fullText.boost());
        } else {
            //Should not be needed, it has this value as default
            builder.setFullText(false);
        }

        final Facet facet = field.getAnnotation(Facet.class);
        if (facet != null) {
            builder.setFacet(true);
            builder.setSuggest(facet.suggestion());
        } else {
            //Should not be needed, it has this value as default
            builder.setFacet(false);
        }

        //set metadata
        final Metadata metadata = field.getAnnotation(Metadata.class);
        if (metadata != null) {
            for(Entry entry : metadata.value()) {
                builder.putMetadata(entry.name(), entry.value());
            }
        }

        return build(builder, fieldName, type, multiValue);
    }

    /**
     * Generates a new FieldDescriptor object based on the input parameters.
     * @param builder FieldDescriptorBuilder object used to create a new FieldDescriptor.
     * @param field Name of the new field to be created.
     * @param clazz Class of the content to be stored in the new FieldDescriptor.
     * @param multiValue Boolean value to indicate whether the new field can store one o more values (true multivalued, false single valued)
     * @param <T> Type of the content to be stored on the resultant field.
     * @return new FieldDescriptor<T> object
     */
    private static <T> FieldDescriptor<T> build(FieldDescriptorBuilder builder, String field, Class<T> clazz, boolean multiValue){

        if(Number.class.isAssignableFrom(clazz)){
            if(Long.class.isAssignableFrom(clazz)) {
                return  multiValue? (MultiValueFieldDescriptor<T>)builder.buildMultivaluedNumericField(field, Long.class) :
                        (SingleValueFieldDescriptor<T>)builder.buildNumericField(field, Long.class);
            }
            if(Integer.class.isAssignableFrom(clazz)) {
                return  multiValue? (MultiValueFieldDescriptor<T>)builder.buildMultivaluedNumericField(field, Integer.class) :
                        (SingleValueFieldDescriptor<T>)builder.buildNumericField(field, Integer.class);
            }
            if(Double.class.isAssignableFrom(clazz)) {
                return  multiValue? (MultiValueFieldDescriptor<T>)builder.buildMultivaluedNumericField(field, Double.class) :
                        (SingleValueFieldDescriptor<T>)builder.buildNumericField(field, Double.class);
            }
            return  multiValue? (MultiValueFieldDescriptor<T>)builder.buildMultivaluedNumericField(field) :
                    (SingleValueFieldDescriptor<T>)builder.buildNumericField(field);
        }
        if(ZonedDateTime.class.isAssignableFrom(clazz)) {
            return multiValue? (MultiValueFieldDescriptor<T>)builder.buildMultivaluedDateField(field) :
                    (SingleValueFieldDescriptor<T>)builder.buildDateField(field);
        }
        if(Date.class.isAssignableFrom(clazz)) {
            return multiValue? (MultiValueFieldDescriptor<T>)builder.buildMultivaluedUtilDateField(field) :
                    (SingleValueFieldDescriptor<T>)builder.buildUtilDateField(field);
        }
        if(LatLng.class.isAssignableFrom(clazz)) {
            return multiValue ? (MultiValueFieldDescriptor<T>) builder.buildMultivaluedLocationField(field) :
                    (SingleValueFieldDescriptor<T>) builder.buildLocationField(field);
        }
        if(CharSequence.class.isAssignableFrom(clazz)) {
            return multiValue? (MultiValueFieldDescriptor<T>)builder.buildMultivaluedTextField(field):
                    (SingleValueFieldDescriptor<T>)builder.buildTextField(field);
        }
        //TODO Objects
        else {
            log.error("Unable to build FieldDescriptor: type [{}] is not valid for FieldDescriptors",clazz.getName());
            throw new RuntimeException("Unable to build FieldDescriptor: type ["+clazz.getName()+"] is not supported by FieldDescriptors");
        }
    }

    /**
     * Generates a new FieldDescriptor object based on the input parameters.
     * @param builder FieldDescriptorBuilder object used to create a new FieldDescriptor.
     * @param field Name of the new field to be created.
     * @param clazz Class of the content to be stored in the new FieldDescriptor.
     * @param multiValue Boolean value to indicate whether the new field can store one o more values (true multivalued, false single valued)
     * @param <T> Type of the content to be stored on the resultant field.
     * @return new FieldDescriptor<T> object
     */
    private static <T,F,S> ComplexFieldDescriptor<T,F,S> buildComplex(ComplexFieldDescriptorBuilder builder, String field, Class<T> clazz, Class<F> facet, Class<S> store, boolean multiValue, Class<? extends Function> sortFunctionClass){

        Function sortFunction = null;
        if (!NullFunction.class.isAssignableFrom(sortFunctionClass)) {
            try {
                sortFunction = sortFunctionClass.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if(Number.class.isAssignableFrom(facet)){
            if(Long.class.isAssignableFrom(facet)) {
                if(Objects.nonNull(sortFunction)){
                    return  multiValue? (MultiValuedComplexField<T,F,S>)builder.buildSortableMultivaluedNumericComplexField(field, clazz, Long.class, store, sortFunction) :
                            (SingleValuedComplexField<T,F,S>)builder.buildSortableNumericComplexField(field, clazz, Long.class, store, sortFunction);
                } else {
                return  multiValue? (MultiValuedComplexField<T,F,S>)builder.buildMultivaluedNumericComplexField(field, clazz, Long.class, store) :
                        (SingleValuedComplexField<T,F,S>)builder.buildNumericComplexField(field,clazz, Long.class, store);
                }
            }
            if(Integer.class.isAssignableFrom(facet)) {
                if(Objects.nonNull(sortFunction)){
                    return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildSortableMultivaluedNumericComplexField(field, clazz, Integer.class, store, sortFunction) :
                            (SingleValuedComplexField<T, F, S>) builder.buildSortableNumericComplexField(field, clazz, Integer.class, store, sortFunction);
                } else {
                    return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildMultivaluedNumericComplexField(field, clazz, Integer.class, store) :
                            (SingleValuedComplexField<T, F, S>) builder.buildNumericComplexField(field, clazz, Integer.class, store);
                }
            }
            if(Double.class.isAssignableFrom(facet)) {
                if(Objects.nonNull(sortFunction)){
                    return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildSortableMultivaluedNumericComplexField(field, clazz, Double.class, store, sortFunction) :
                            (SingleValuedComplexField<T, F, S>) builder.buildNumericComplexField(field, clazz, Long.class, store);
                } else {
                    return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildSortableMultivaluedNumericComplexField(field, clazz, Double.class, store, sortFunction) :
                            (SingleValuedComplexField<T, F, S>) builder.buildNumericComplexField(field, clazz, Long.class, store);
                }
            }
            if(Objects.nonNull(sortFunction)){
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildSortableMultivaluedNumericComplexField(field, clazz, Double.class, store, sortFunction) :
                        (SingleValuedComplexField<T, F, S>) builder.buildSortableNumericComplexField(field, clazz, Long.class, store, sortFunction);
            } else {
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildMultivaluedNumericComplexField(field, clazz, Double.class, store) :
                        (SingleValuedComplexField<T, F, S>) builder.buildNumericComplexField(field, clazz, Long.class, store);
            }
        }
        if(ZonedDateTime.class.isAssignableFrom(facet)) {
            if(Objects.nonNull(sortFunction)){
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildSortableMultivaluedDateComplexField(field, clazz, ZonedDateTime.class, store, sortFunction) :
                        (SingleValuedComplexField<T, F, S>) builder.buildSortableDateComplexField(field, clazz, ZonedDateTime.class, store, sortFunction);
            } else {
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildMultivaluedDateComplexField(field, clazz, ZonedDateTime.class, store) :
                        (SingleValuedComplexField<T, F, S>) builder.buildDateComplexField(field, clazz, ZonedDateTime.class, store);
            }
        }
        if(Date.class.isAssignableFrom(facet)) {
            if(Objects.nonNull(sortFunction)){
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildSortableMultivaluedUtilDateComplexField(field, clazz, Date.class, store, sortFunction) :
                        (SingleValuedComplexField<T, F, S>) builder.buildSortableUtilDateComplexField(field, clazz, Date.class, store, sortFunction);
            } else {
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildMultivaluedUtilDateComplexField(field, clazz, Date.class, store) :
                        (SingleValuedComplexField<T, F, S>) builder.buildUtilDateComplexField(field, clazz, Date.class, store);
            }
        }

        if(LatLng.class.isAssignableFrom(facet)) {
            return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildMultivaluedLocationComplexField(field, clazz, LatLng.class, store) :
                        (SingleValuedComplexField<T, F, S>) builder.buildLocationComplexField(field, clazz, LatLng.class, store);
        }

        if(CharSequence.class.isAssignableFrom(facet)) {
            if(Objects.nonNull(sortFunction)){
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildSortableMultivaluedTextComplexField(field, clazz, String.class, store, sortFunction) :
                        (SingleValuedComplexField<T, F, S>) builder.buildSortableTextComplexField(field, clazz, String.class, store, sortFunction);
            } else {
                return multiValue ? (MultiValuedComplexField<T, F, S>) builder.buildMultivaluedTextComplexField(field, clazz, String.class, store) :
                        (SingleValuedComplexField<T, F, S>) builder.buildTextComplexField(field, clazz, String.class, store);
            }
        }
        //TODO Objects
        else {
            log.error("Unable to build FieldDescriptor: type [{}] is not valid for FieldDescriptors",facet.getName());
            throw new RuntimeException("Unable to build FieldDescriptor: type ["+facet.getName()+"] is not supported by FieldDescriptors");
        }
    }

    /**
     * Retrieves the field annotated as @Id in a given class.
     * @param pojoClass Class from which it is wanted to know the @Id annotated field.
     * @return the field with the @Id annotation.
     */
    private static Field getIdField(Class<?> pojoClass) {
        final List<Field> idFields = Arrays.stream(getFields(pojoClass))
                .filter(f -> f.isAnnotationPresent(Id.class))
                .collect(Collectors.toList());
        switch (idFields.size()) {
            case 0:
                log.error("No @Id-field found in {}", pojoClass);
                throw new IllegalArgumentException("No @Id-field found in " + pojoClass);
            case 1:
                final Field f = idFields.get(0);
                if (!CharSequence.class.isAssignableFrom(f.getType())) {
                    log.error("@Id-annotated field must be a CharSequence implementation");
                    throw new IllegalArgumentException("@Id-annotated field must be CharSequence of String");
                }
                f.setAccessible(true);
                return f;
            default:
                log.error("Multiple @Id-fields found in {}", pojoClass);
                throw new IllegalArgumentException("Multiple @Id-fields found in " + pojoClass);
        }
    }

    /**
     * Retrieves the field annotated as @Score in a given class.
     * @param pojoClass Class from which it is wanted to know the @Score annotated field.
     * @return the field with the @Score annotation or null if there is no such an annotation.
     */
    private static Field getScoreField(Class<?> pojoClass) {
        final List<Field> idFields = Arrays.stream(getFields(pojoClass))
                .filter(f -> f.isAnnotationPresent(Score.class))
                .collect(Collectors.toList());
        switch (idFields.size()) {
            case 0:
                return null;
            case 1:
                final Field f = idFields.get(0);
                if (!Float.class.isAssignableFrom(f.getType()) && !float.class.isAssignableFrom(f.getType())) {
                    log.error("@Score-annotated field must be of kind float");
                    throw new IllegalArgumentException("@Score-annotated field must be of kind float");
                }
                f.setAccessible(true);
                return f;
            default:
                log.error("Multiple @Score-fields found in {}", pojoClass);
                throw new IllegalArgumentException("Multiple @Score-fields found in " + pojoClass);
        }
    }

    /**
     * Instantiates a pojo Object of the specified class based on the values in the input document.
     * @param doc internal object to be parsed as specific pojo
     * @param clazz spectated class of the resultant pojo
     * @param <T> spectated class of the resultant pojo
     * @return generated pojo from given document typed as the specified class
     */
    public static <T> T createPojo(Document doc, Class<T> clazz) {
        try {
            final String typeVal = getType(clazz);
            if (!StringUtils.equals(typeVal, doc.getType())) {
                log.error("@Type does not match. Expected {} but found {}",typeVal, doc.getType());
                throw new IllegalArgumentException("@Type does not match. Expected " + typeVal + " but found " + doc.getType());
            }

            final Map<String, Field> fieldMap = new HashMap<>();
            for (Field field : getFields(clazz)) {
                final FieldDescriptor fd = createFieldDescriptor(field);
                //TODO: MBDN-496 check if complex field is binary stored to instantiate original value
                if (fd == null || ComplexFieldDescriptor.class.isAssignableFrom(fd.getClass())) {
                    continue;
                }
                fieldMap.put(fd.getName(), field);
            }
            final Field idField = getIdField(clazz);
            final Field scoreField = getScoreField(clazz);
            final T instance ;

            try {
                instance = clazz.newInstance();
            } catch (InstantiationException e) {
                log.error("Unable to instantiate class {}",clazz.getSimpleName(), e);
                throw new RuntimeException("Unable to instantiate class "+clazz.getSimpleName(), e);
            }

            idField.set(instance, doc.getId());
            if(scoreField != null) {
                scoreField.set(instance, doc.getScore());
            }

            for (String fName: fieldMap.keySet()) {
                final Field f = fieldMap.get(fName);
                f.setAccessible(true);

                final Object value = doc.getValue(fName);
                if (value instanceof Collection && Collection.class.isAssignableFrom(f.getType())) { // TODO: Improve Collection/Multi-Value detection
                    Collection c;
                    if (Modifier.isAbstract(f.getType().getModifiers())) {
                        log.error("Annotated field {} can not be abstract", f.getName());
                        throw new IllegalArgumentException("Annotated field "+f.getName()+" can not be abstract");
                    } else {
                        try {
                            c = (Collection) f.getType().newInstance();
                        } catch (InstantiationException e) {
                            log.error("Unable to instantiate collection field {} of type [{}]",f.getName(), f.getType(), e);
                            throw new RuntimeException("Unable to instantiate collection field "+f.getName()+" of type ["+f.getType()+"]", e);
                        }
                    }
                    c.addAll((Collection) value);
                    f.set(instance, c);
                } else {
                    f.set(instance, value);
                }
            }
            return instance;
        } catch (IllegalAccessException e) {
            log.error("Unable to access pojo field",e);
            throw new RuntimeException("Unable to access pojo field",e);
        }
    }

}
