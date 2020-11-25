package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.ZonedDateTime;
import java.util.Date;

public class NumericRangeLiteral extends RangeLiteral{

    public NumericRangeLiteral(String from, String to) {
        if (!from.equals(WILDCARD)) {
            this.from = NumberUtils.createNumber(from);
        }
        if (!to.equals(WILDCARD)) {
            this.to = NumberUtils.createNumber(to);
        }
    }

    @Override
    public Number getFrom() {
        return (Number)from;
    }

    @Override
    public Number getTo() {
        return (Number)to;
    }

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {

        if (Number.class.isAssignableFrom(descriptor.getType())) {
            if(from!=null && to!=null) {
                return Filter.between(descriptor.getName(),(Number)from, (Number)to);
            }
            if(from!=null && to==null) {
                return Filter.greaterThan(descriptor.getName(),(Number)from);
            }
            if(from==null && to!=null) {
                return Filter.lesserThan(descriptor.getName(),(Number)to);
            } else return descriptor.isNotEmpty();
        } else  if (from ==null && to ==null  &&
                (ZonedDateTime.class.isAssignableFrom(descriptor.getType()) || Date.class.isAssignableFrom(descriptor.getType()))){
            return descriptor.isNotEmpty();
        }
        throw new SearchServerException("Error parsingRange filter: descriptor type ["+descriptor.getType().getSimpleName()+"] does not suport ranges" );
    }
}
