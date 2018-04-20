package com.rbmhtechnology.vind.api.query.update;

import com.rbmhtechnology.vind.model.*;

import java.util.*;

/**
 * Class to define the updates to be perform on a document.
 *
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 12.07.16.
 */
public class Update {

    private String id;
    private HashMap<FieldDescriptor<?>, HashMap<String, SortedSet<UpdateOperation>>> contextualizedOptions = new HashMap<>();
    private String context;

    /**
     * Creates a new instance of {@link Update} object for an specific Document.
     * @param id String unique identification value of the document to be updated.
     */
    public Update(String id) {
        this.id = id;
    }

    /**
     *  Prepares a field to be removed from the document.
     * @param descriptor {@link FieldDescriptor} indicates the field to ve removed.
     * @return {@link Update} object set to remove the specified field.
     */
    public Update remove(FieldDescriptor<?> descriptor) {
        return this.remove(this.context, descriptor);
    }

    /**
     *  Prepares a field to be removed from the document. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link FieldDescriptor} indicates the field to ve removed.
     * @param context String name of the context where the value will be updated.
     * @return {@link Update} object set to remove the specified field.
     */
    @Deprecated
    public Update remove(FieldDescriptor<?> descriptor, String context) {
        return remove(context, descriptor);
    }
    /**
     *  Prepares a field to be removed from the document.
     * @param descriptor {@link FieldDescriptor} indicates the field to ve removed.
     * @param context String name of the context where the value will be updated.
     * @return {@link Update} object set to remove the specified field.
     */
    public Update remove(String context, FieldDescriptor<?> descriptor) {
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.set, null));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);
        return this;
    }

    /**
     * Prepares a group of values to be removed from a specific field of the document. If no values are provided (null or
     * empty list) the field is removed.
     * @param descriptor {@link FieldDescriptor} indicates the field  where the values are removed from.
     * @param t values to be deleted from the document field.
     * @param <T> Type of content the field can store.
     * @return {@link Update} object set to remove the specified values from the field.
     */
    public <T> Update remove(FieldDescriptor<T> descriptor, T... t) {
        return this.remove(this.context, descriptor, t);
    }

    /**
     * Prepares a group of values to be removed from a specific field of the document. If no values are provided (null or
     * empty list) the field is removed. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link FieldDescriptor} indicates the field  where the values are removed from.
     * @param context String name of the context where the value will be updated.
     * @param t values to be deleted from the document field.
     * @param <T> Type of content the field can store.
     * @return {@link Update} object set to remove the specified values from the field.
     */
    @Deprecated
    public <T> Update remove(FieldDescriptor<T> descriptor, String context, T... t) {
        return remove(context, descriptor, t);
    }

    /**
     * Prepares a group of values to be removed from a specific field of the document. If no values are provided (null or
     * empty list) the field is removed.
     * @param descriptor {@link FieldDescriptor} indicates the field  where the values are removed from.
     * @param context String name of the context where the value will be updated.
     * @param t values to be deleted from the document field.
     * @param <T> Type of content the field can store.
     * @return {@link Update} object set to remove the specified values from the field.
     */
    public <T> Update remove(String context, FieldDescriptor<T> descriptor, T... t) {
        if(Objects.isNull(t) || t.length<=0) {
            return remove(context, descriptor);
        }
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.remove, t));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);
        return this;
    }

    /**
     * Prepares to remove those values, from a multivalued field, which matches with the given regex.
     * @param descriptor {@link MultiValueFieldDescriptor} indicates the field  where the values are removed from.
     * @param s String regex matching the values to be deleted.
     * @return {@link Update} object set to remove the specified regex matching values from the field.
     */
    public Update removeRegex(MultiValueFieldDescriptor<?> descriptor, String s) {
        return this.removeRegex(this.context, descriptor,s);
    }

    /**
     * Prepares to remove those values, from a multivalued field, which matches with the given regex. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link MultiValueFieldDescriptor} indicates the field  where the values are removed from.
     * @param context String name of the context where the value will be updated.
     * @param s String regex matching the values to be deleted.
     * @return {@link Update} object set to remove the specified regex matching values from the field.
     */
    @Deprecated
    public Update removeRegex(MultiValueFieldDescriptor<?> descriptor,String context, String s) {
        return removeRegex(context,descriptor,s);
    }
    /**
     * Prepares to remove those values, from a multivalued field, which matches with the given regex.
     * @param descriptor {@link MultiValueFieldDescriptor} indicates the field  where the values are removed from.
     * @param context String name of the context where the value will be updated.
     * @param s String regex matching the values to be deleted.
     * @return {@link Update} object set to remove the specified regex matching values from the field.
     */
    public Update removeRegex(String context, MultiValueFieldDescriptor<?> descriptor, String s) {
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.removeregex, s));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);

        return this;
    }

    /**
     * Prepares the {@link Update} object to increment a numeric field in an specified quantity.
     * @param descriptor {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.NumericFieldDescriptor} indicating the field to be incremented.
     * @param t Quantity to increase the field.
     * @param <T> Type of the field. T must extend Number.
     * @return {@link Update} object set to increment in the specified amount the field.
     */
    public <T extends Number> Update increment(SingleValueFieldDescriptor.NumericFieldDescriptor<T> descriptor, T t) {
        return this.increment(descriptor, this.context, t);
    }

    /**
     * Prepares the {@link Update} object to increment a numeric field in an specified quantity. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.NumericFieldDescriptor} indicating the field to be incremented.
     * @param context String name of the context where the value will be updated.
     * @param t Quantity to increase the field.
     * @param <T> Type of the field. T must extend Number.
     * @return {@link Update} object set to increment in the specified amount the field.
     */
    @Deprecated
    public <T extends Number> Update increment(SingleValueFieldDescriptor.NumericFieldDescriptor<T> descriptor, String context, T t) {
        return increment(context, descriptor, t);
    }
    /**
     * Prepares the {@link Update} object to increment a numeric field in an specified quantity.
     * @param descriptor {@link com.rbmhtechnology.vind.model.SingleValueFieldDescriptor.NumericFieldDescriptor} indicating the field to be incremented.
     * @param context String name of the context where the value will be updated.
     * @param t Quantity to increase the field.
     * @param <T> Type of the field. T must extend Number.
     * @return {@link Update} object set to increment in the specified amount the field.
     */
    public <T extends Number> Update increment(String context, SingleValueFieldDescriptor.NumericFieldDescriptor<T> descriptor, T t) {
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.inc, t));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);

        return this;
    }

    /**
     * Prepares the {@link Update} object to set a value on a single valued field. The old value, if existing, is
     * over-written. If no no value is given (null) the field is removed.
     * @param descriptor {@link SingleValueFieldDescriptor} indicating the field to be set.
     * @param t Value to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the value on the field.
     */
    public <T> Update set(SingleValueFieldDescriptor<T> descriptor, T t) {
        return this.set(this.context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to set a value on a single valued field. The old value, if existing, is
     * over-written. If no no value is given (null) the field is removed. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link SingleValueFieldDescriptor} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Value to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the value on the field.
     */
    @Deprecated
    public <T> Update set(SingleValueFieldDescriptor<T> descriptor, String context, T t) {
        return set(context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to set a value on a single valued field. The old value, if existing, is
     * over-written. If no no value is given (null) the field is removed.
     * @param descriptor {@link SingleValueFieldDescriptor} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Value to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the value on the field.
     */
    public <T> Update set( String context, SingleValueFieldDescriptor<T> descriptor, T t) {
        if(Objects.isNull(t)) {
            return remove(context, descriptor);
        }
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.set, t));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);

        return this;
    }

    /**
     * Prepares the {@link Update} object to set a value on a single valued field. The old value, if existing, is
     * over-written. If no no value is given (null) the field is removed.
     * @param descriptor {@link SingleValuedComplexField} indicating the field to be set.
     * @param t Value to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the value on the field.
     */
    public <T> Update set(SingleValuedComplexField<T,?,?> descriptor, T t) {
        return this.set(descriptor, this.context, t);
    }

    /**
     * Prepares the {@link Update} object to set a value on a single valued field. The old value, if existing, is
     * over-written. If no no value is given (null) the field is removed. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link SingleValuedComplexField} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Value to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the value on the field.
     */
    @Deprecated
    public <T> Update set(SingleValuedComplexField<T,?,?> descriptor, String context, T t) {
        return set(context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to set a value on a single valued field. The old value, if existing, is
     * over-written. If no no value is given (null) the field is removed.
     * @param descriptor {@link SingleValuedComplexField} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Value to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the value on the field.
     */
    public <T> Update set(String context, SingleValuedComplexField<T,?,?> descriptor, T t) {
        if(Objects.isNull(t)) {
            return remove(context, descriptor);
        }
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.set, t));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);
        return this;
    }

    /**
     * Prepares the {@link Update} object to set a group of values on a multivalued field. The old values, if existing,
     * are over-written. If no no values are given (null or empty) the field is removed.
     * @param descriptor {@link MultiValueFieldDescriptor} indicating the field to be set.
     * @param t Values to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the values on the field.
     */
    public <T> Update set(MultiValueFieldDescriptor<T> descriptor, T ... t) {
        return this.set(this.context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to set a group of values on a multivalued field. The old values, if existing,
     * are over-written. If no no values are given (null or empty) the field is removed. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link MultiValueFieldDescriptor} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the values on the field.
     */
    @Deprecated
    public <T> Update set(MultiValueFieldDescriptor<T> descriptor, String context, T ... t) {
        return set(context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to set a group of values on a multivalued field. The old values, if existing,
     * are over-written. If no no values are given (null or empty) the field is removed.
     * @param descriptor {@link MultiValueFieldDescriptor} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the values on the field.
     */
    public <T> Update set(String context, MultiValueFieldDescriptor<T> descriptor, T ... t) {
        if(Objects.isNull(t) || t.length<=0) {
            return remove(context, descriptor);
        }
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.set, Arrays.asList(t)));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);
        return this;
    }

    /**
     * Prepares the {@link Update} object to set a group of values on a multivalued field. The old values, if existing,
     * are over-written. If no no values are given (null or empty) the field is removed.
     * @param descriptor {@link MultiValuedComplexField} indicating the field to be set.
     * @param t Values to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the values on the field.
     */
    public <T> Update set(MultiValuedComplexField<T,?,?> descriptor, T ... t) {
        return this.set(this.context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to set a group of values on a multivalued field. The old values, if existing,
     * are over-written. If no no values are given (null or empty) the field is removed. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link MultiValuedComplexField} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the values on the field.
     */
    @Deprecated
    public <T> Update set(MultiValuedComplexField<T,?,?> descriptor, String context, T ... t) {
        return set(context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to set a group of values on a multivalued field. The old values, if existing,
     * are over-written. If no no values are given (null or empty) the field is removed.
     * @param descriptor {@link MultiValuedComplexField} indicating the field to be set.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be set.
     * @param <T> Type of the field.
     * @return {@link Update} object set to set the values on the field.
     */
    public <T> Update set(String context, MultiValuedComplexField<T,?,?> descriptor, T ... t) {
        if(Objects.isNull(t) || t.length<=0) {
            return remove(context, descriptor);
        }
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.set, Arrays.asList(t)));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);
        return this;
    }

    /**
     * Prepares the {@link Update} object to add a group of values to a multivalued field.
     * @param descriptor {@link MultiValueFieldDescriptor} indicating the field to be added to.
     * @param t Values to be added.
     * @param <T> Type of the field.
     * @return {@link Update} object set to add the values on the field.
     */
    public <T> Update add(MultiValueFieldDescriptor<T> descriptor, T ... t) {
        return this.add(context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to add a group of values to a multivalued field. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link MultiValueFieldDescriptor} indicating the field to be added to.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be added.
     * @param <T> Type of the field.
     * @return {@link Update} object set to add the values on the field.
     */
    @Deprecated
    public <T> Update add(MultiValueFieldDescriptor<T> descriptor, String context, T ... t) {
        return add(context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to add a group of values to a multivalued field.
     * @param descriptor {@link MultiValueFieldDescriptor} indicating the field to be added to.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be added.
     * @param <T> Type of the field.
     * @return {@link Update} object set to add the values on the field.
     */
    public <T> Update add(String context, MultiValueFieldDescriptor<T> descriptor, T ... t) {
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.add, Arrays.asList(t)));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);
        return this;
    }

    /**
     * Prepares the {@link Update} object to add a group of values to a multivalued field.
     * @param descriptor {@link MultiValuedComplexField} indicating the field to be added to.
     * @param t Values to be added.
     * @param <T> Type of the field.
     * @return {@link Update} object set to add the values on the field.
     */
    public <T> Update add(MultiValuedComplexField<T,?,?> descriptor, T ... t) {
        return this.add(this.context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to add a group of values to a multivalued field. <strong>Deprecated: may cause ambigous method call</strong>
     * @param descriptor {@link MultiValuedComplexField} indicating the field to be added to.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be added.
     * @param <T> Type of the field.
     * @return {@link Update} object set to add the values on the field.
     */
    @Deprecated
    public <T> Update add(MultiValuedComplexField<T,?,?> descriptor,String context,T ... t) {
        return add(context, descriptor, t);
    }

    /**
     * Prepares the {@link Update} object to add a group of values to a multivalued field.
     * @param descriptor {@link MultiValuedComplexField} indicating the field to be added to.
     * @param context String name of the context where the value will be updated.
     * @param t Values to be added.
     * @param <T> Type of the field.
     * @return {@link Update} object set to add the values on the field.
     */
    public <T> Update add(String context,MultiValuedComplexField<T,?,?> descriptor,T ... t) {
        final HashMap<String, SortedSet<UpdateOperation>> fieldOperations =
                Objects.isNull(contextualizedOptions.get(descriptor)) ? new HashMap<>() : contextualizedOptions.get(descriptor);

        final SortedSet<UpdateOperation> updateOperations =
                Objects.isNull(fieldOperations.get(context)) ? new TreeSet<>() : fieldOperations.get(context);

        updateOperations.add(new UpdateOperation(UpdateOperations.add, Arrays.asList(t)));
        fieldOperations.put(context,updateOperations);
        contextualizedOptions.put(descriptor,fieldOperations);
        return this;
    }

    /**
     * Gets the document identification string.
     * @return String document id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get the updates prepared in the {@link Update} instance.
     * @return  {@code HashMap<FieldDescriptor<?>, HashMap<String, Object>>} A map of {@link FieldDescriptor} as key and as value a map with key {@link UpdateOperations} and value
     * the modifier values.
     */
    public HashMap<FieldDescriptor<?>, HashMap<String, SortedSet<UpdateOperation>>> getOptions() {
        return contextualizedOptions;
    }

    /**
     *  Sets the update context.
     * @param context String context to be searched in.
     * @return This {@link Update} instance with the new context.
     */
    public Update context(String context) {
        this.context = context;
        return this;
    }

    /**
     * Gets the context of the update.
     * @return String containing the context target.
     */
    public String getUpdateContext() {
        return this.context;
    }

    public enum UpdateOperations {
        add, inc, remove, removeregex, set
    }

}
