package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.annotations.language.Language;
import com.rbmhtechnology.vind.model.value.LatLng;

import java.nio.ByteBuffer;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * Builder class which allows to instantiate the different field descriptors.
 * Any field, apart from being able to be multi-valued or single-valued, has 7 basic modifiers:
 * - stored: whether the content of the field is stored on the index or not.
 * - indexed: Whether the field is indexed or not.
 * - fulltext: whether the field is used for fulltext search or not.
 * - language: the language of the content field.
 * - boost: boosting value to give more or less relevance to the field.
 * - facet: whether the field can be used for faceting or not.
 * - suggest: whether the field can be used for suggestions or not.
 *
 * The field can also have metadata properties defined by the user.
 * By default a new field is stored and indexed with no language and a boost of 1 (no boosting).
 *
 * Created by fonso on 6/22/16.
 */
public class FieldDescriptorBuilder<T> {

    private boolean stored = true;
    private boolean indexed = true;
    private boolean fullText = false;
    private Language language = Language.None;
    private float boost = 1;
    private boolean facet = false;
    private boolean suggest = false;
    private Map<String,String> metadata = new HashMap<>();

    /**
     * Builds a field multi-valued for date content with the configured flags.
     * @param field Name of the new field.
     * @return A multivalued date field descriptor.
     */
    public MultiValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> buildMultivaluedDateField (String field){
        MultiValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> dateFieldDescriptor = new MultiValueFieldDescriptor.DateFieldDescriptor<>(field, ZonedDateTime.class);
        dateFieldDescriptor.setStored(stored);
        dateFieldDescriptor.setIndexed(indexed);
        dateFieldDescriptor.setFullText(fullText);
        dateFieldDescriptor.setMultiValue(true);
        dateFieldDescriptor.setLanguage(language);
        dateFieldDescriptor.setBoost(boost);
        dateFieldDescriptor.setFacet(facet);
        dateFieldDescriptor.setSuggest(suggest);
        dateFieldDescriptor.setMetadata(metadata);
        dateFieldDescriptor.sort = true;
        dateFieldDescriptor.setSort(c -> {
            final Iterator iterator = ((Collection)c).iterator();
            if (iterator.hasNext()) {
                return (ZonedDateTime)iterator.next();
            } else {
                return null;
            }
        });
        return dateFieldDescriptor;
    }

    /**
     * Builds a field sortable multi-valued for date content with the configured flags.
     * @param field Name of the new field.
     * @param sortLambda Lambda function to calculate the value to sort by based on the field values.
     * @return A multivalued date field descriptor.
     */
    public MultiValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> buildSortableMultivaluedDateField (String field, Function<Collection<ZonedDateTime>,ZonedDateTime> sortLambda){
        MultiValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> dateFieldDescriptor = new MultiValueFieldDescriptor.DateFieldDescriptor<>(field, ZonedDateTime.class);
        dateFieldDescriptor.setStored(stored);
        dateFieldDescriptor.setIndexed(indexed);
        dateFieldDescriptor.setFullText(fullText);
        dateFieldDescriptor.setMultiValue(true);
        dateFieldDescriptor.setLanguage(language);
        dateFieldDescriptor.setBoost(boost);
        dateFieldDescriptor.setFacet(facet);
        dateFieldDescriptor.setSuggest(suggest);
        dateFieldDescriptor.setMetadata(metadata);
        dateFieldDescriptor.setSort(sortLambda);
        return dateFieldDescriptor;
    }

    /**
     * Builds a field multi-valued for date content with the configured flags.
     * @param field Name of the new field.
     * @return A multivalued date field descriptor.
     */
    public MultiValueFieldDescriptor.UtilDateFieldDescriptor<Date> buildMultivaluedUtilDateField (String field){
        MultiValueFieldDescriptor.UtilDateFieldDescriptor<Date> dateFieldDescriptor = new MultiValueFieldDescriptor.UtilDateFieldDescriptor<>(field, Date.class);
        dateFieldDescriptor.setStored(stored);
        dateFieldDescriptor.setIndexed(indexed);
        dateFieldDescriptor.setFullText(fullText);
        dateFieldDescriptor.setMultiValue(true);
        dateFieldDescriptor.setLanguage(language);
        dateFieldDescriptor.setBoost(boost);
        dateFieldDescriptor.setFacet(facet);
        dateFieldDescriptor.setSuggest(suggest);
        dateFieldDescriptor.setMetadata(metadata);
        dateFieldDescriptor.sort = true;
        dateFieldDescriptor.setSort(c -> {
            final Iterator iterator = ((Collection)c).iterator();
            if (iterator.hasNext()) {
                return (Date)iterator.next();
            } else {
                return null;
            }
        });
        return dateFieldDescriptor;
    }

    /**
     * Builds a sortable field multi-valued for date content with the configured flags.
     * @param field Name of the new field.
     * @param sortLambda Lambda function to calculate the value to sort by based on the field values.
     * @return A multivalued date field descriptor.
     */
    public MultiValueFieldDescriptor.UtilDateFieldDescriptor<Date> buildSortableMultivaluedUtilDateField (String field, Function<Collection<Date>,Date> sortLambda){
        MultiValueFieldDescriptor.UtilDateFieldDescriptor<Date> dateFieldDescriptor = new MultiValueFieldDescriptor.UtilDateFieldDescriptor<>(field, Date.class);
        dateFieldDescriptor.setStored(stored);
        dateFieldDescriptor.setIndexed(indexed);
        dateFieldDescriptor.setFullText(fullText);
        dateFieldDescriptor.setMultiValue(true);
        dateFieldDescriptor.setLanguage(language);
        dateFieldDescriptor.setBoost(boost);
        dateFieldDescriptor.setFacet(facet);
        dateFieldDescriptor.setSuggest(suggest);
        dateFieldDescriptor.setMetadata(metadata);
        dateFieldDescriptor.setSort(sortLambda);
        return dateFieldDescriptor;
    }

    /**
     * Builds a field single-valued for date content with the configured flags.
     * @param field Name of the new field.
     * @return A single-valued date field descriptor.
     */
    public SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> buildDateField (String field){
        SingleValueFieldDescriptor.DateFieldDescriptor<ZonedDateTime> dateFieldDescriptor = new SingleValueFieldDescriptor.DateFieldDescriptor<>(field,ZonedDateTime.class);
        dateFieldDescriptor.setStored(stored);
        dateFieldDescriptor.setIndexed(indexed);
        dateFieldDescriptor.setFullText(fullText);
        dateFieldDescriptor.setMultiValue(false);
        dateFieldDescriptor.setLanguage(language);
        dateFieldDescriptor.setBoost(boost);
        dateFieldDescriptor.setFacet(facet);
        dateFieldDescriptor.setSuggest(suggest);
        dateFieldDescriptor.setMetadata(metadata);
        return dateFieldDescriptor;
    }

    /**
     * Builds a field single-valued for date content with the configured flags.
     * @param field Name of the new field.
     * @return A single-valued date field descriptor.
     */
    public SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> buildUtilDateField (String field){
        SingleValueFieldDescriptor.UtilDateFieldDescriptor<Date> dateFieldDescriptor = new SingleValueFieldDescriptor.UtilDateFieldDescriptor<>(field,Date.class);
        dateFieldDescriptor.setStored(stored);
        dateFieldDescriptor.setIndexed(indexed);
        dateFieldDescriptor.setFullText(fullText);
        dateFieldDescriptor.setMultiValue(false);
        dateFieldDescriptor.setLanguage(language);
        dateFieldDescriptor.setBoost(boost);
        dateFieldDescriptor.setFacet(facet);
        dateFieldDescriptor.setSuggest(suggest);
        dateFieldDescriptor.setMetadata(metadata);
        return dateFieldDescriptor;
    }

    /**
     * Builds a sortable numeric field multi-valued which content type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param clazz specific implementation class of Number (Integer, Double,...).
     * @param sortLambda Lambda function to calculate the value to sort by based on the field values.
     * @return A multivalued numeric field descriptor.
     */
    public <T extends Number> MultiValueFieldDescriptor.NumericFieldDescriptor<T> buildSortableMultivaluedNumericField (String field, Class<T> clazz, Function<Collection<T>,T> sortLambda){
        MultiValueFieldDescriptor.NumericFieldDescriptor<T> numericFieldDescriptor = new MultiValueFieldDescriptor.NumericFieldDescriptor<>(field, clazz);
        numericFieldDescriptor.setStored(stored);
        numericFieldDescriptor.setIndexed(indexed);
        numericFieldDescriptor.setFullText(fullText);
        numericFieldDescriptor.setMultiValue(true);
        numericFieldDescriptor.setLanguage(language);
        numericFieldDescriptor.setBoost(boost);
        numericFieldDescriptor.setFacet(facet);
        numericFieldDescriptor.setSuggest(suggest);
        numericFieldDescriptor.setMetadata(metadata);
        numericFieldDescriptor.setSort(sortLambda);
        return numericFieldDescriptor;
    }

    /**
     * Builds a numeric field multi-valued which content type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param clazz specific implementation class of Number (Integer, Double,...).
     * @return A multivalued numeric field descriptor.
     */
    public <T extends Number> MultiValueFieldDescriptor.NumericFieldDescriptor<T> buildMultivaluedNumericField (String field, Class<T> clazz){
        MultiValueFieldDescriptor.NumericFieldDescriptor<T> numericFieldDescriptor = new MultiValueFieldDescriptor.NumericFieldDescriptor<T>(field, clazz);
        numericFieldDescriptor.setStored(stored);
        numericFieldDescriptor.setIndexed(indexed);
        numericFieldDescriptor.setFullText(fullText);
        numericFieldDescriptor.setMultiValue(true);
        numericFieldDescriptor.setLanguage(language);
        numericFieldDescriptor.setBoost(boost);
        numericFieldDescriptor.setFacet(facet);
        numericFieldDescriptor.setSuggest(suggest);
        numericFieldDescriptor.setMetadata(metadata);
        numericFieldDescriptor.sort = true;
        numericFieldDescriptor.setSort(c -> {
            final Iterator iterator = ((Collection)c).iterator();
            if (iterator.hasNext()) {
                return (T)iterator.next();
            } else {
                return null;
            }
        });
        return numericFieldDescriptor;
    }

    /**
     * Builds a numeric field multi-valued which content type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @return A multivalued numeric field descriptor.
     */
    public <T extends Number> MultiValueFieldDescriptor.NumericFieldDescriptor<Number> buildMultivaluedNumericField (String field){
        return this.buildMultivaluedNumericField(field, Number.class);
    }

    /**
     * Builds a numeric field single-valued which content type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @param clazz specific implementation class of Number (Integer, Double,...).
     * @return A single valued numeric field descriptor.
     */
    public <T extends Number> SingleValueFieldDescriptor.NumericFieldDescriptor<T> buildNumericField (String field, Class<T> clazz){
        SingleValueFieldDescriptor.NumericFieldDescriptor numericFieldDescriptor = new SingleValueFieldDescriptor.NumericFieldDescriptor(field, clazz);
        numericFieldDescriptor.setStored(stored);
        numericFieldDescriptor.setIndexed(indexed);
        numericFieldDescriptor.setFullText(fullText);
        numericFieldDescriptor.setMultiValue(false);
        numericFieldDescriptor.setLanguage(language);
        numericFieldDescriptor.setBoost(boost);
        numericFieldDescriptor.setFacet(facet);
        numericFieldDescriptor.setSuggest(suggest);
        numericFieldDescriptor.setMetadata(metadata);
        return numericFieldDescriptor;
    }
    /**
     * Builds a numeric field single-valued which content type should extend Number, configured with the current flags.
     * @param field Name of the new field.
     * @return A single valued numeric field descriptor.
     */
    public SingleValueFieldDescriptor.NumericFieldDescriptor<Number> buildNumericField (String field){
        return this.buildNumericField(field, Number.class);
    }

    /**
     * Builds a text field multi-valued which content type should extend CharSequence, configured with the current flags.
     * @param field Name of the new field.
     * @return A multivalued text field descriptor.
     */
    public MultiValueFieldDescriptor.TextFieldDescriptor<String> buildMultivaluedTextField (String field){
        MultiValueFieldDescriptor.TextFieldDescriptor<String> textFieldDescriptor = new MultiValueFieldDescriptor.TextFieldDescriptor<>(field, String.class);
        textFieldDescriptor.setStored(stored);
        textFieldDescriptor.setIndexed(indexed);
        textFieldDescriptor.setFullText(fullText);
        textFieldDescriptor.setMultiValue(true);
        textFieldDescriptor.setLanguage(language);
        textFieldDescriptor.setBoost(boost);
        textFieldDescriptor.setFacet(facet);
        textFieldDescriptor.setSuggest(suggest);
        textFieldDescriptor.setMetadata(metadata);
        textFieldDescriptor.sort = true;
        textFieldDescriptor.setSort(c -> {
            final Iterator iterator = ((Collection)c).iterator();
            if (iterator.hasNext()) {
                return (String)iterator.next();
            } else {
                return null;
            }
        });
        return textFieldDescriptor;
    }

    /**
     * Builds a sortable text field multi-valued which content type should extend CharSequence, configured with the current flags.
     * @param field Name of the new field.
     * @param sortLambda Lambda function to calculate the value to sort by based on the field values.
     * @return A multivalued text field descriptor.
     */
    public MultiValueFieldDescriptor.TextFieldDescriptor<String> buildSortableMultivaluedTextField (String field, Function<Collection<String>, String> sortLambda){
        MultiValueFieldDescriptor.TextFieldDescriptor<String> textFieldDescriptor = new MultiValueFieldDescriptor.TextFieldDescriptor<>(field, String.class);
        textFieldDescriptor.setStored(stored);
        textFieldDescriptor.setIndexed(indexed);
        textFieldDescriptor.setFullText(fullText);
        textFieldDescriptor.setMultiValue(true);
        textFieldDescriptor.setLanguage(language);
        textFieldDescriptor.setBoost(boost);
        textFieldDescriptor.setFacet(facet);
        textFieldDescriptor.setSuggest(suggest);
        textFieldDescriptor.setMetadata(metadata);
        textFieldDescriptor.setSort(sortLambda);
        return textFieldDescriptor;
    }

    /**
     * Builds a text field single-valued which content type should extend CharSequence, configured with the current flags.
     * @param field Name of the new field.
     * @return A single valued text field descriptor.
     */
    public SingleValueFieldDescriptor.TextFieldDescriptor<String> buildTextField (String field){
        SingleValueFieldDescriptor.TextFieldDescriptor<String> textFieldDescriptor = new SingleValueFieldDescriptor.TextFieldDescriptor<>(field, String.class);
        textFieldDescriptor.setStored(stored);
        textFieldDescriptor.setIndexed(indexed);
        textFieldDescriptor.setFullText(fullText);
        textFieldDescriptor.setMultiValue(false);
        textFieldDescriptor.setLanguage(language);
        textFieldDescriptor.setBoost(boost);
        textFieldDescriptor.setFacet(facet);
        textFieldDescriptor.setSuggest(suggest);
        textFieldDescriptor.setMetadata(metadata);
        return textFieldDescriptor;
    }

    /**
     * Builds a binary field multi-valued which content type should extend ByteBuffer, configured with the current flags.
     * @param field Name of the new field.
     * @return A multivalued binary field descriptor.
     */
    public MultiValueFieldDescriptor.BinaryFieldDescriptor<ByteBuffer> buildMultivaluedBinaryField (String field){
        MultiValueFieldDescriptor.BinaryFieldDescriptor<ByteBuffer> binaryFieldDescriptor = new MultiValueFieldDescriptor.BinaryFieldDescriptor<>(field, ByteBuffer.class);
        binaryFieldDescriptor.setStored(true);
        binaryFieldDescriptor.setIndexed(false);
        binaryFieldDescriptor.setFullText(false);
        binaryFieldDescriptor.setMultiValue(true);
        binaryFieldDescriptor.setLanguage(language);
        binaryFieldDescriptor.setBoost(boost);
        binaryFieldDescriptor.setFacet(false);
        binaryFieldDescriptor.setSuggest(false);
        binaryFieldDescriptor.setMetadata(metadata);
        binaryFieldDescriptor.sort = false;
        return binaryFieldDescriptor;
    }

    /**
     * Builds a binary field single-valued which content type should extend ByteBuffer, configured with the current flags.
     * @param field Name of the new field.
     * @return A single valued binary field descriptor.
     */
    public SingleValueFieldDescriptor.BinaryFieldDescriptor<ByteBuffer> buildBinaryField (String field){
        SingleValueFieldDescriptor.BinaryFieldDescriptor<ByteBuffer> binaryFieldDescriptor = new SingleValueFieldDescriptor.BinaryFieldDescriptor<>(field, ByteBuffer.class);
        binaryFieldDescriptor.setStored(true);
        binaryFieldDescriptor.setIndexed(false);
        binaryFieldDescriptor.setFullText(false);
        binaryFieldDescriptor.setMultiValue(false);
        binaryFieldDescriptor.setLanguage(language);
        binaryFieldDescriptor.setBoost(boost);
        binaryFieldDescriptor.setFacet(false);
        binaryFieldDescriptor.setSuggest(false);
        binaryFieldDescriptor.setMetadata(metadata);
        binaryFieldDescriptor.sort = false;
        return binaryFieldDescriptor;
    }

    public SingleValueFieldDescriptor.LocationFieldDescriptor<LatLng> buildLocationField (String field){
        SingleValueFieldDescriptor.LocationFieldDescriptor<LatLng> locationFieldDescriptor = new SingleValueFieldDescriptor.LocationFieldDescriptor<>(field, LatLng.class);
        locationFieldDescriptor.setStored(stored);
        locationFieldDescriptor.setIndexed(indexed);
        locationFieldDescriptor.setFullText(fullText);
        locationFieldDescriptor.setMultiValue(false);
        locationFieldDescriptor.setLanguage(language);
        locationFieldDescriptor.setBoost(boost);
        locationFieldDescriptor.setFacet(facet);
        locationFieldDescriptor.setSuggest(suggest);
        locationFieldDescriptor.setMetadata(metadata);
        return locationFieldDescriptor;
    }

    public MultiValueFieldDescriptor.LocationFieldDescriptor<LatLng> buildMultivaluedLocationField (String field){
        MultiValueFieldDescriptor.LocationFieldDescriptor<LatLng> locationFieldDescriptor = new MultiValueFieldDescriptor.LocationFieldDescriptor<>(field, LatLng.class);
        locationFieldDescriptor.setStored(stored);
        locationFieldDescriptor.setIndexed(indexed);
        locationFieldDescriptor.setFullText(fullText);
        locationFieldDescriptor.setMultiValue(true);
        locationFieldDescriptor.setLanguage(language);
        locationFieldDescriptor.setBoost(boost);
        locationFieldDescriptor.setFacet(facet);
        locationFieldDescriptor.setSuggest(suggest);
        locationFieldDescriptor.setMetadata(metadata);
        return locationFieldDescriptor;
    }


    /**
     * Sets the field to be stored or not.
     * @param stored True to configure the field to be stored.
     * @return the {@link FieldDescriptorBuilder} with the new configuration.
     */
    public FieldDescriptorBuilder<T> setStored(boolean stored) {
        this.stored = stored;
        return this;
    }

    /**
     * Sets the field to be indexed or not.
     * @param indexed True to configure the field to be indexed.
     * @return the {@link FieldDescriptorBuilder} with the new configuration.
     */
    public FieldDescriptorBuilder<T> setIndexed(boolean indexed) {
        this.indexed = indexed;
        return this;
    }

    /**
     * Sets the field to be fullText or not.
     * @param fullText True to configure the field to be fullText.
     * @return the {@link FieldDescriptorBuilder} with the new configuration.
     */
    public FieldDescriptorBuilder<T> setFullText(boolean fullText) {
        this.fullText = fullText;
        return this;
    }

    /**
     * Sets the field language: German("de"), English("en"), Spanish("es") or None(null).
     * @param language Language value.
     * @return the {@link FieldDescriptorBuilder} with the new configuration.
     */
    public FieldDescriptorBuilder<T> setLanguage(Language language) {
        this.language = language;
        return this;
    }
    /**
     * Sets the boost value for the field.
     * @param boost A float value to modify the calculated score for thr field. 1 is the,'no boost value'.
     * @return the {@link FieldDescriptorBuilder} with the new configuration.
     */
    public FieldDescriptorBuilder<T> setBoost(float boost) {
        this.boost = boost;
        return this;
    }

    /**
     * Sets the field to be used for faceting or not
     * @param facet True to configure the field to be used on faceting.
     * @return the {@link FieldDescriptorBuilder} with the new configuration.
     */
    public FieldDescriptorBuilder<T> setFacet(boolean facet) {
        this.facet = facet;
        return this;
    }

    /**
     * Sets the field to be used for suggestion or not
     * @param suggest True to configure the field to be used on suggestion.
     * @return the {@link FieldDescriptorBuilder} with the new configuration.
     */
    public FieldDescriptorBuilder<T> setSuggest(boolean suggest) {
        this.suggest = suggest;
        return this;
    }

    /**
     * Add metadata to the field.
     * @param name metadata property name.
     * @param value metadata value.
     * @return the {@link FieldDescriptorBuilder} with the new metadata added.
     */
    public FieldDescriptorBuilder<T> putMetadata(String name, String value) {
        this.metadata.put(name, value);
        return this;
    }
}
