package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.datemath.DateMathParser;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;

import java.time.ZonedDateTime;
import java.util.Date;

public class DateRangeLiteral extends RangeLiteral{

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
        return (DateMathExpression) from;
    }

    @Override
    public DateMathExpression getTo() {
        return (DateMathExpression) to;
    }

    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {


        if (ZonedDateTime.class.isAssignableFrom(descriptor.getType()) | Date.class.isAssignableFrom(descriptor.getType())) {
            if(from!=null && to!=null) {
                return Filter.between(descriptor.getName(),(DateMathExpression) from, (DateMathExpression) to);
            }
            if(from!=null && to==null) {
                return Filter.after(descriptor.getName(),(DateMathExpression) from);
            }
            if(from==null && to!=null) {
                return Filter.before(descriptor.getName(),(DateMathExpression) to);
            }
            throw new SearchServerException("Error parsingRange filter: range should have defined at least upper or lower limit" );

        } else if (Number.class.isAssignableFrom(descriptor.getType())) {
            if(from!=null && to!=null) {
                return Filter.between(descriptor.getName(),((DateMathExpression) from).getTimeStamp(), ((DateMathExpression) to).getTimeStamp());
            }
            if(from!=null && to==null) {
                return Filter.greaterThan(descriptor.getName(),((DateMathExpression) from).getTimeStamp());
            }
            if(from==null && to!=null) {
                return Filter.lesserThan(descriptor.getName(),((DateMathExpression) to).getTimeStamp());
            }
            throw new SearchServerException("Error parsingRange filter: range should have defined at least upper or lower limit" );

        }
        else {
            throw new SearchServerException("Error parsingRange filter: descriptor type ["+descriptor.getType().getSimpleName()+"] does not support ranges" );
        }

    }
}
