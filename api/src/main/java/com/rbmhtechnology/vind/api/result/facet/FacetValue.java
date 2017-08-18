package com.rbmhtechnology.vind.api.result.facet;

/**
 * Generic facet value.
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 27.06.16.
 */
public class FacetValue<T> {

    private T value;
    private long count;

    /**
     * Creates a new instance of {@link FacetValue}.
     * @param value Faceted value of type T.
     * @param count Count of documents grouped in this facet.
     */
    public FacetValue(T value, long count) {
        this.value = value;
        this.count = count;
    }

    /**
     * Gets the faceted value.
     * @return T value of the facet.
     */
    public T getValue() {
        return value;
    }

    /**
     * Gets the number of documents grouped in this facet value.
     * @return long number of documents.
     */
    public long getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "FacetValue{" +
                "value=" + value +
                ", count=" + count +
                '}';
    }
}
