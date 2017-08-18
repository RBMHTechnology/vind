package com.rbmhtechnology.vind.model.value;

import java.text.ParseException;

/**
 * @author Thomas Kurz (thomas.kurz@redlink.co)
 * @since 17.02.17.
 */
public class LatLng {

    private double lat,lng;

    public static LatLng parseLatLng(String s) throws ParseException {
        try {
            String[] values = s.split(",");
            return new LatLng(Double.parseDouble(values[0]),Double.parseDouble(values[1]));
        } catch (Exception e) {
            throw new ParseException("String cannot be parsed as LatLng",0);
        }
    }

    public LatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    @Override
    public String toString() {
        return lat + ","  +lng;
    }
}
