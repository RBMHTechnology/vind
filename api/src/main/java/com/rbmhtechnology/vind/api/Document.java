package com.rbmhtechnology.vind.api;

import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValueFieldDescriptor;
import com.rbmhtechnology.vind.model.MultiValuedComplexField;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Interface to be implemented by specific documents.
 */
public interface Document {

    /**
     * Sets a value in a document field. If the field had already a value it is overwritten by the new value.
     * @param field Name of the field to set.
     * @param value Value to set on the field.
     * @return The document instance with the new value for the field.
     */
    Document setValue(String field, Object value); //TODO not typesave -> should trow a Cast Exception?


    /**
     * Sets a value in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
     * For non contextualized values use null context.
     * @param field Name of the field to set.
     * @param value Value to set on the field.
     * @param context {@link String} with the context name.
     * @return The document instance with the new value for the field.
     */
    Document setContextualizedValue(String field, String context, Object value);

    /**
     * Sets a value in a document field. If the field had already a value it is overwritten by the new value.
     * @param field descriptor of the field to set.
     * @param value Value to set on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the new value for the field.
     */
    <T> Document setValue(FieldDescriptor<T> field, T value); //TODO should we keep generics?

    /**
     * Sets a value in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
     * For non contextualized values use null context.
     * @param field Name of the field to set.
     * @param value Value to set on the field.
     * @param context {@link String} with the context name.
     * @param  <T> Type of the content field.
     * @return The document instance with the new value for the field.
     */
    <T> Document setContextualizedValue(FieldDescriptor<T> field, String context, T value);

    /**
     * Sets a group of values in a document field. If the field had already values they are overwritten by the new set.
     * @param field Name of the field to set.
     * @param values group of values to set on the field.
     * @return The document instance with the new values for the field.
     */
    Document setValues(String field, Object... values);

    /**
     * Sets a group of values in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
     * For non contextualized values use null context.
     * @param field Name of the field to set.
     * @param values group of values to set on the field.
     * @param context {@link String} with the context name.
     * @return The document instance with the new value for the field.
     */
    Document setContextualizedValues(String field, String context, Object... values);

    /**
     * Sets a group of values in a document field. If the field had already values they are overwritten by the new ones.
     * @param field descriptor of the field to set. It must be a {@link MultiValueFieldDescriptor}
     * @param value Values to set on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the new values for the field.
     */
    <T> Document setValues(MultiValueFieldDescriptor<T> field, T... value); //TODO check for multivalue fields: are they actually allowed!


    /**
     * Sets a group of values in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
     * For non contextualized values use null context.
     * @param field descriptor of the field to set. It must be a {@link MultiValueFieldDescriptor}
     * @param value Values to set on the field.
     * @param context {@link String} with the context name.
     * @param  <T> Type of the content field.
     * @return The document instance with the new value for the field.
     */
    <T> Document setContextualizedValues(MultiValueFieldDescriptor<T> field, String context, T... value);

    /**
     * Sets a group of values in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
    * For non contextualized values use null context.
    * @param field descriptor of the field to set. It must be a {@link MultiValuedComplexField}
    * @param value Values to set on the field.
    * @param context {@link String} with the context name.
    * @param  <T> Type of the content field.
    * @return The document instance with the new value for the field.
    */
    <T> Document setContextualizedValues(MultiValuedComplexField<T,?,?> field, String context, T... value);

    /**
     * Sets a group of values in a document field. If the field had already values they are overwritten by the new ones.
     * @param field descriptor of the field to set. It must be a {@link MultiValuedComplexField}
     * @param value Values to set on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the new values for the field.
     */
    <T> Document setValues(MultiValuedComplexField<T,?,?> field, T... value); //TODO check for multivalue fields: are they actually allowed!

    /**
     * Sets a group of values in a document field. If the field had already values they are overwritten by the new set.
     * @param field Name of the field to set.
     * @param values group of values to set on the field.
     * @return The document instance with the new values for the field.
     */
    Document setValues(String field, Collection<?> values);

    /**
     * Sets a group of values in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
     * For non contextualized values use null context.
     * @param field Name of the field to set.
     * @param values group of values to set on the field.
     * @param context {@link String} with the context name.
     * @return The document instance with the new value for the field.
     */
    Document setContextualizedValues(String field, String context, Collection<?> values);

    /**
     * Sets a group of values in a document field. If the field had already values they are overwritten by the new ones.
     * @param field descriptor of the field to set. It must be a {@link MultiValueFieldDescriptor}
     * @param value Values to set on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the new values for the field.
     */
    <T> Document setValues(MultiValueFieldDescriptor<T> field, Collection<T> value);

    /**
     * Sets a group of values in a document field. If the field had already values they are overwritten by the new ones.
     * @param field descriptor of the field to set. It must be a {@link MultiValuedComplexField}
     * @param value Values to set on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the new values for the field.
     */
    <T> Document setValues(MultiValuedComplexField<T,?,?> field, Collection<T> value);

    /**
     * Sets a group of values in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
     * For non contextualized values use null context.
     * @param field descriptor of the field to set. It must be a {@link MultiValueFieldDescriptor}
     * @param value Values to set on the field.
     * @param context {@link String} with the context name.
     * @param  <T> Type of the content field.
     * @return The document instance with the new value for the field.
     */
    <T> Document setContextualizedValues(MultiValueFieldDescriptor<T> field, String context, Collection<T> value);
    /**
     * Sets a group of values in a document field for an specific context. If the field had already a value for the given context it is overwritten by the new value.
     * For non contextualized values use null context.
     * @param field descriptor of the field to set. It must be a {@link MultiValuedComplexField}
     * @param value Values to set on the field.
     * @param context {@link String} with the context name.
     * @param  <T> Type of the content field.
     * @return The document instance with the new value for the field.
     */
    <T> Document setContextualizedValues(MultiValuedComplexField<T,?,?> field, String context, Collection<T> value);

    /**
     * Removes the values of a field.
     * @param field Name of the field to be cleared.
     * @return The document instance with the field cleared.
     */
    Document clear(String field);

    /**
     * Removes the values of a field.
     * @param field descriptor of the field to be cleared.
     * @return The document instance with the field cleared.
     */
    Document clear(FieldDescriptor<?> field);

    /**
     * Adds a value to a document field. The field should be multivalued.
     * @param field Name of the field to add.
     * @param value Value to add on the field.
     * @return The document instance with the added value in the field.
     */
    Document addValue(String field, Object value);

    /**
     * Adds a contextualized value to a document field. The field should be multivalued.
     * @param field Name of the field to add.
     * @param value Value to add on the field.
     * @param context {@link String} with the context name.
     * @return The document instance with the added value in the field.
     */
    Document addContextualizedValue(String field, String context, Object value);

    Document addChild(Document... document);

    @Deprecated
    Document setChildren(Document... documents);

    /**
     * Adds a value to a document field.
     * @param field descriptor of the field to add to.
     * @param value Value to add on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the added value in the field.
     */

    <T> Document addValue(MultiValueFieldDescriptor<T> field, T value);


    /**
     * Adds a contextualized value to a document field.
     * @param field descriptor of the field to add to.
     * @param value Value to add on the field.
     * @param context {@link String} with the context name.
     * @param  <T> Type of the content field.
     * @return The document instance with the added value in the field.
     */
    <T> Document addValue(MultiValueFieldDescriptor<T> field, String context, T value);

    /**
     * Adds a value to a document field.
     * @param field descriptor of the field to add to.
     * @param value Value to add on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the added value in the field.
     */

    <T> Document addValue(MultiValuedComplexField<T,?,?> field, T value);

    /**
     * Adds a value to a document field in an specific context.
     * @param field descriptor of the field to add to.
     * @param value Value to add on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the added value in the field.
     */
    <T> Document addContextualizedValue(MultiValueFieldDescriptor<T>  field, String context,T value);

    /**
     * Adds a value to a document field in an specific context.
     * @param field descriptor of the field to add to.
     * @param value Value to add on the field.
     * @param  <T> Type of the content field.
     * @return The document instance with the added value in the field.
     */
    <T> Document addContextualizedValue(MultiValuedComplexField<T,?,?> field, String context,T value);

    /**
     * Removes a value from a document field.
     * @param field Name of the field to remove from.
     * @param value Value to remove from the field.
     * @return The document instance without the value in the field.
     */
    Document removeValue(String field, Object value);

    /**
     * Removes a value from a document field.
     * @param field Name of the field to remove from.
     * @param value Value to remove from the field.
     * @param context {@link String} with the context name.
     * @return The document instance without the value in the field.
     */
    Document removeContextualizedValue(String field, String context, Object value);

    /**
     * Removes a value from a document field.
     * @param field descriptor of the field to remove from.
     * @param value Value to remove from the field.
     * @param  <T> Type of the content field.
     * @return The document instance without the value in the field.
     */
    <T> Document removeValue(FieldDescriptor<T> field, T value);

    /**
     * Removes a value from a document field.
     * @param field descriptor of the field to remove from.
     * @param value Value to remove from the field.
     * @param context {@link String} with the context name.
     * @param  <T> Type of the content field.
     * @return The document instance without the value in the field.
     */
    <T> Document removeContextualizedValue(FieldDescriptor<T> field, String context, T value);

    /**
     * Gets the document identification string.
     * @return document id.
     */
    String getId();

    /**
     * Gets the document type.
     * @return document type.
     */
    String getType();

    /**
     * Gets the document distance.
     * @return a distance for the document.
     */
    float getDistance();

    /**
     * Sets the document score.
     * @param distance distance value for the document instance.
     */
    void setDistance(float distance);

    /**
     * Gets the document score.
     * @return a score for the document.
     */
    float getScore();

    /**
     * Sets the document score.
     * @param score score value for the document instance.
     */
    void setScore(float score);


    /**
     * Gets the child count (only for subdoc queries?)
     * @return number of (matching) childs
     */
    Integer getChildCount();

    /**
     * Sets the child count
     * @param childCount number of matching childs
     */
    void setChildCount(Integer childCount);

    /**
     * Gets the content of a field.
     * @param field Name of the field.
     * @return An object with the field value.
     */
    Object getValue(String field);

    /**
     * Gets the content of a field in a given context.
     * @param field Name of the field.
     * @param context {@link String} with the context name.
     * @return An object with the field value.
     */
    Object getContextualizedValue(String field, String context);

    /**
     * Gets the content of a field.
     * @param descriptor descriptor of the field.
     * @return An object with the field value.
     */
    <T> T getValue(FieldDescriptor<T> descriptor);

    /**
     * Gets the content of a field in a given context.
     * @param descriptor descriptor of the field.
     * @param context {@link String} with the context name.
     * @return An object with the field value.
     */
    <T> T getContextualizedValue(FieldDescriptor<T> descriptor, String context);

    /**
     * Gets the content of a field.
     * @param field Name of the field.
     * @param clazz Spectated content type.
     * @return An object with the field value.
     */
    <T> T getValue(String field, Class<T> clazz);

    /**
     * Gets the content of a field in a given context.
     * @param field Name of the field.
     * @param context {@link String} with the context name.
     * @param clazz Spectated content type.
     * @return An object with the field value.
     */
    <T> T getContextualizedValue(String field,String context, Class<T> clazz);

    /**
     * Gets the contexts defined for the given document field.
     * @param field {@link String} name of the field to get the context.
     * @return context names or null if there is no context set.
     */
    Set<String> getFieldContexts(String field);

    /**
     * Gets the contexts defined for the given document field.
     * @param descriptor {@link FieldDescriptor} to get the context.
     * @return context names or null if there is no context set.
     */
    Set<String> getFieldContexts(FieldDescriptor descriptor);

    /**
     * Checks if a document has vale for a field
     * @param field Name of the field to check
     * @return true if there is a value.
     */
    boolean hasValue(String field);

    /**
     * Checks if a document has vale for a field
     * @param descriptor descriptor of the field to check
     * @return true if there is a value.
     */
    <T> boolean hasValue(FieldDescriptor<T> descriptor);


    boolean hasChildren();

    Collection<Document> getChildren();

    /**
     * Checks if a document has a field
     * @param fieldName Name of the field to check
     * @return true if there is a field.
     */
    boolean hasField(String fieldName);

    /**
     * Gets the field descriptor of a field.
     * @param fieldName Name of the field.
     * @return {@link FieldDescriptor} of a field.
     */
    FieldDescriptor<?> getFieldDescriptor(String fieldName);

    /**Gets the list of {@link FieldDescriptor} in the schema of the documment.
     * @return A List of field descriptors.
     */
    Map<String, FieldDescriptor<?>> listFieldDescriptors();

    /**
     * Gets all the values of the document.
     * @return A map of field name as key and field value as values.
     */

    Map<String, Object> getValues();

}
