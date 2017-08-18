package com.rbmhtechnology.vind.api.query.delete;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

/**
 * Class to define deletion by defined filters
 * Created by fonso on 12/23/16.
 */
public class Delete {

    private Filter query = null;
    private String context;

    /**
     * Creates an instance of the Deletion query with a given filter definition.
     * @param filter {@link Filter} filter to be applied on the delete query.
     */
    public Delete(Filter filter) {
        query = filter;
    }

    /**
     * Sets the filter of the Deletion query.
     * @param filter {@link Filter} filter to be applied on the delete query.
     * @return This instance with the new filter defined.
     */
    public Delete filter(Filter filter) {
        query = filter;
        return this;
    }

    /**
     * Sets the filter of the delete query to a basic equals comparison for the given field and value.
     * @param field {@link FieldDescriptor} specifies the field to filter by.
     * @param value Specifies the value to compare with in the filter.
     * @return This instance with the new filter defined.
     */
    public Delete filter(FieldDescriptor field, String value){
        filter(Filter.eq(field,value));
        return this;
    }

    /**
     * Sets the filter of the delete query to a basic equals comparison for the given field and value.
     * @param field {@link String} specifies the field to filter by.
     * @param value Specifies the value to compare with in the filter.
     * @return This instance with the new filter defined.
     */
    public Delete filter(String field, String value){
        filter(Filter.eq(field,value));
        return this;
    }

    /**
     * Returns the filter to be applied on the delete query.
     * @return {@link Filter} filter to be applied.
     */
    public Filter getQuery() {
        return query;
    }

    /**
     *  Sets the deletion context.
     * @param context String context to be used on deletion.
     * @return This {@link Delete} instance with the new context.
     */
    public Delete context(String context) {
        this.context = context;
        return this;
    }

    /**
     * Gets the context of the delete.
     * @return String containing the context target.
     */
    public String getUpdateContext() {
        return this.context;
    }
}
