package com.rbmhtechnology.vind.api.query.update;

import java.util.Objects;

import static com.rbmhtechnology.vind.api.query.update.Update.*;

/**
 * Created by fonso on 31.05.17.
 */
public class UpdateOperation implements Comparable{

    private UpdateOperations type;
    private Object value;

    public UpdateOperation(UpdateOperations type,  Object value) {
        this.type = type;
        this.value = value;
    }

    public UpdateOperations getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public int compareTo(Object o) {
        if(UpdateOperation.class.isAssignableFrom(o.getClass())) {
            final UpdateOperation o1 = (UpdateOperation) o;
            if(o1.getType().equals(this.getType()) && Objects.equals(this.getValue(), o1.getValue())) {
                return 0;
            }
        }
        return 1;
    }
}
