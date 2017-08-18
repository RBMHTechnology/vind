package com.rbmhtechnology.vind.api.query.distance;

import com.rbmhtechnology.vind.model.FieldDescriptor;
import com.rbmhtechnology.vind.model.value.LatLng;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.02.17.
 */
public class Distance {

    private final String fieldName;
    private LatLng location;
    private FieldDescriptor field;

    public Distance(FieldDescriptor field, LatLng location) {
        this.location = location;
        this.field = field;
        this.fieldName = field.getName();
    }

    public Distance(String fieldName, LatLng location) {
        this.location = location;
        this.fieldName = fieldName;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public FieldDescriptor getField() {
        return field;
    }

    public void setField(FieldDescriptor field) {
        this.field = field;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString(){
        final String scoreString = "{" +
                "\"location\":\"%s\"," +
                "\"field\":\"%s\"" +
                "}";
        return String.format(scoreString,this.location,this.fieldName);
    }
}
