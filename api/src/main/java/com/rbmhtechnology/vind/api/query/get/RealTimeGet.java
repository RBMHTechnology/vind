package com.rbmhtechnology.vind.api.query.get;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class to define a realTime get query document/s by id.
 * Created by fonso on 10/18/16.
 */
public class RealTimeGet<T> {

    private final List<T> values = new ArrayList<>();

    public RealTimeGet get(T ... value) {
        if(value!=null && value.length > 0){
            for (T v : value){
                this.values.add(v);
            }
        }
        return this;
    }

    public RealTimeGet get(List<T> value) {
        if(value!=null && value.size() > 0){
            for (T v : value){
                this.values.add(v);
            }
        }
        return this;
    }

    public List<T> getValues() {
        return Collections.unmodifiableList(values);
    }
}
