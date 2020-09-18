package com.rbmhtechnology.vind.parser.queryparser;

import com.rbmhtechnology.vind.SearchServerException;
import com.rbmhtechnology.vind.api.query.datemath.DateMathParser;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.FieldDescriptor;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.ZonedDateTime;
import java.util.Date;

public class RangeLiteral extends SimpleLiteral{
    private final String from;
    private final String to;

    public RangeLiteral(String from, String to) {
        this.from = from;
        this.to = to;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "[ " + from + " TO "+ to + " ]";
    }


    @Override
    public Filter toVindFilter(FieldDescriptor descriptor) {
        final DateMathParser dateMathParser = new DateMathParser();

        if (ZonedDateTime.class.isAssignableFrom(descriptor.getType()) | Date.class.isAssignableFrom(descriptor.getType())) {

            return Filter.between(descriptor.getName(),
                    dateMathParser.parseMath(this.from),
                    dateMathParser.parseMath(this.to));

        } else if (Number.class.isAssignableFrom(descriptor.getType())) {
            return Filter.between(descriptor.getName(),
                    NumberUtils.createNumber(from),
                    NumberUtils.createNumber(to));
        } else {
            throw new SearchServerException("Error parsingRange filter: descriptor type ["+descriptor.getType().getSimpleName()+"] does not suport ranges" );
        }

    }
}
