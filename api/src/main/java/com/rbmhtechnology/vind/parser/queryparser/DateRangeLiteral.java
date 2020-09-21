package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.datemath.DateMathParser;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.time.ZonedDateTime;
import java.util.Date;

public class DateRangeLiteral extends RangeLiteral{

    private DateMathExpression from;
    private DateMathExpression to;

    public DateRangeLiteral(String from, String to) {
        final DateMathParser dateMathParser = new DateMathParser();
        if (!from.equals(WILDCARD)) {
            this.from = dateMathParser.parseMath(from);
        }
        if (!to.equals(WILDCARD)) {
            this.to = dateMathParser.parseMath(to);
        }
    }

    @Override
    public DateMathExpression getFrom() {
        return from;
    }

    @Override
    public DateMathExpression getTo() {
        return to;
    }

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {
        final DateMathParser dateMathParser = new DateMathParser();

        if (ZonedDateTime.class.isAssignableFrom(descriptor.getType()) | Date.class.isAssignableFrom(descriptor.getType())) {
            if(from!=null && to!=null) {
                return Filter.between(descriptor.getName(),from, to);
            }
            if(from!=null && to==null) {
                return Filter.after(descriptor.getName(),from);
            }
            if(from==null && to!=null) {
                return Filter.before(descriptor.getName(),to);
            }
            throw new SearchServerException("Error parsingRange filter: range should have defined at least upper or lower limit" );

        }  else {
            throw new SearchServerException("Error parsingRange filter: descriptor type ["+descriptor.getType().getSimpleName()+"] does not support ranges" );
        }

    }
}
