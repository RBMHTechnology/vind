package com.rbmhtechnology.vind.model;

import com.rbmhtechnology.vind.annotations.language.Language;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.value.LatLng;

import java.io.Serializable;
import java.nio.ByteBuffer;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;

/**
 * General type-independent field descriptor. It is implemented by type specific field descriptors.
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 15.06.16.
 */
public abstract class FieldDescriptor<T> {

    /**
     * Class types supported as field descriptor content.
     */
    private static final Set<Class<?>> supportedTypes;
    static {
        Set<Class<?>> types = new HashSet<>();

        types.add(CharSequence.class);
        types.add(Number.class);
        types.add(ZonedDateTime.class);
        types.add(Date.class);
        types.add(LatLng.class);
        types.add(Serializable.class);
        types.add(ByteBuffer.class);
        types.add(ByteBuffer.class);
        types.add(LatLng.class);

        supportedTypes = Collections.unmodifiableSet(types);
    }

    private String name;
    private final Class<T> type;
    private Language language;
    private float boost;
    private Map<String,String> metadata;
    protected Function<?, T> sortFunction;

    //flags
    private boolean update;
    private boolean stored;
    private boolean indexed;
    private boolean fullText;
    private boolean facet;
    private boolean multiValue;
    private boolean suggest;
    protected boolean sort;
    private boolean contextualized;

    protected FieldDescriptor(String fieldName, Class<T> type) {
        if (!checkFieldType(type)) {
            // TODO
            throw new IllegalArgumentException();
        }
        name = fieldName;
        this.type = type;
        this.metadata = new HashMap<>();
    }

    /**
     * Checks if a class is a valid field content type.
     * @param type Class to be checked.
     * @return True if it is a valid content, false otherwise.
     */
    public static boolean checkFieldType(Class<?> type) {
        for (Class<?> supportedType : supportedTypes) {
            if (supportedType.isAssignableFrom(type)) {
                return true;
            }
        }
        return type.isPrimitive();
    }

    /**
     * Gets the field Name.
     * @return Field name.
     */
    public String getName() {
        return name;
    }

    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the field type of content.
     * @return Field type.
     */
    public Class<T> getType() {
        return type;
    }

    protected void setStored(boolean stored) {
        this.stored = stored;
    }

    /**
     * Checks if a field is stored or not.
     * @return True if it is configured to be stored.
     */
    public boolean isStored() {
        return stored;
    }

    protected void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }

    /**
     * Checks if a field is indexed or not.
     * @return True if it is configured to be indexed.
     */
    public boolean isIndexed() {
        return indexed;
    }

    protected void setFullText(boolean fullText) {
        this.fullText = fullText;
    }

    /**
     * Checks if a field is fullText searchable or not.
     * @return True if it is configured to be fullText searchable.
     */
    public boolean isFullText() {
        return fullText;
    }

    protected void setLanguage(Language language) {
        this.language = language;
    }

    /**
     * Gets the field language configured.
     * @return Field Language
     */
    public Language getLanguage() {
        return language;
    }

    protected void setBoost(float boost) {
        this.boost = boost;
    }

    /**
     * Gets the field configured boosting value.
     * @return Field boost.
     */
    public float getBoost() {
        return boost;
    }

    protected void setFacet(boolean facet) {
        this.facet = facet;
    }

    /**
     * Checks if a field is use for faceting or not.
     * @return True if it is configured to be use for facets.
     */
    public boolean isFacet() {
        return facet;
    }

    protected void setSuggest(boolean suggest) {
        this.suggest = suggest;
    }
    /**
     * Checks if a field is use for suggesting or not.
     * @return True if it is configured to be use for suggestion.
     */
    public boolean isSuggest() {
        return suggest;
    }

    /**
     * Checks if a field is multivalued or not.
     * @return True if it is configured to be multivalued.
     */
    public boolean isMultiValue() {
        return multiValue;
    }

    protected void setMultiValue(boolean multiValue) {
        this.multiValue = multiValue;
    }

    /**
     * Gets the field configured metadata.
     * @return Field metadata
     */
    public Map<String, String> getMetadata() {
        //Field descriptor should be immutable, so we give a copy of the list in order to avoid modifications
        return new HashMap<>(metadata);
    }

    protected void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }


    public boolean isSort() {
        return sort;
    }

    public FieldDescriptor<T> setSort(Function<Collection<T>,T> lambda){
        this.sortFunction = lambda;
        this.sort = true;
        return this;
    }

    public boolean isUpdate() {
        return update;
    }

    protected void setUpdate(boolean update) {
        this.update = update;
    }

    public boolean isContextualized() {
        return contextualized;
    }

    public void setContextualized(boolean contextualized) {
        this.contextualized = contextualized;
    }

    /**
     * Instantiates a new {@link Filter} to checking if a field value is not empty.
     * @return A configured filter for the field.
     */
    public Filter isNotEmpty() {
        if(CharSequence.class.isAssignableFrom(this.type)) {
            return new Filter.NotEmptyTextFilter(this.getName(), Filter.Scope.Facet);
        }
        if(LatLng.class.isAssignableFrom(this.type)) {
            return new Filter.NotEmptyLocationFilter(this.getName(), Filter.Scope.Facet);
        }

        return new Filter.NotEmptyFilter(this.getName(), Filter.Scope.Facet);
    }

    /**
     * Instantiates a new {@link Filter} to checking if a field value is empty.
     * @return A configured filter for the field.
     */
    public Filter isEmpty() {
        return new Filter.NotFilter(this.isNotEmpty());
    }


    @Override
    public String toString() {

        final String serializationString = "{" +
                "\"fieldName\":\"%s\"," +
                "\"type\":\"%s\"," +
                "\"storedFlag\":%s," +
                "\"indexedFlag\":%s," +
                "\"fulltextFlag\":%s," +
                "\"facetFlag\":%s," +
                "\"suggestFlag\":%s," +
                "\"multivaluedFlag\":%s," +
                "\"updateFlag\":%s," +
                "\"sortedFlag\":%s," +
                "\"contextualizedFlag\":%s," +
                "\"boost\":%s," +
                "\"metadata\":%s" +
                "}";

        return String.format(serializationString,
                this.name,
                this.type.getSimpleName(),
                this.stored,
                this.indexed,
                this.fullText,
                this.facet,
                this.suggest,
                this.multiValue,
                this.update,
                this.sort,
                this.contextualized,
                this.boost,
                this.metadata);
    }
}
