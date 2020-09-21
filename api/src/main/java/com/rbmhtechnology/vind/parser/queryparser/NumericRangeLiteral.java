package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.lang3.math.NumberUtils;

public class NumericRangeLiteral extends RangeLiteral{
    private Number from;
    private Number to;

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
        return from;
    }

    @Override
    public Number getTo() {
        return to;
    }

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {

        if (Number.class.isAssignableFrom(descriptor.getType())) {
            if(from!=null && to!=null) {
                return Filter.between(descriptor.getName(),from, to);
            }
            if(from!=null && to==null) {
                return Filter.greaterThan(descriptor.getName(),from);
            }
            if(from==null && to!=null) {
                return Filter.lesserThan(descriptor.getName(),to);
            }
            throw new SearchServerException("Error parsingRange filter: range should have defined at least upper or lower limit" );
        } else {
            throw new SearchServerException("Error parsingRange filter: descriptor type ["+descriptor.getType().getSimpleName()+"] does not suport ranges" );
        }

    }
}
