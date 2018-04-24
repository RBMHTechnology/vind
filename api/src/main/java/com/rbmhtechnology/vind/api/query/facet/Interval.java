package com.rbmhtechnology.vind.api.query.facet;

import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;

import java.time.ZonedDateTime;
import java.util.Date;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 26.07.16.
 */
public class Interval<T> {

    protected String name;

    protected boolean includeStart = true;
    protected boolean includeEnd = true;

    protected T start;
    protected T end;

    public T getStart() {
        return start;
    }

    public T getEnd() {
        return end;
    }


    public boolean includesStart() {
        return includeStart;
    }

    public boolean includesEnd() {
        return includeEnd;
    }

    public String getName() {
        return name;
    }

    public static <T extends Number> NumericInterval<T> numericInterval(String name, T start, T end) {
        return new NumericInterval<>(name, start, end);
    }

    public static <T extends Number> NumericInterval<T> numericInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {
        return new NumericInterval<>(name, start, end, includeStart, includeEnd);
    }

    public static <T extends Date> UtilDateInterval<T> dateInterval(String name, T start, T end) {
        return new UtilDateInterval<>(name, start, end);
    }

    public static <T extends Date> UtilDateInterval<T> dateInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {
        return new UtilDateInterval<>(name, start, end, includeStart, includeEnd);
    }

    public static <T extends ZonedDateTime> ZonedDateTimeInterval<T> dateInterval(String name, T start, T end) {
        return new ZonedDateTimeInterval<>(name, start, end);
    }

    public static <T extends ZonedDateTime> ZonedDateTimeInterval<T> dateInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {
        return new ZonedDateTimeInterval<>(name, start, end, includeStart, includeEnd);
    }

    public static <T extends DateMathExpression> DateMathInterval<T> dateInterval(String name, T start, T end) {
        return new DateMathInterval<>(name, start, end);
    }

    public static <T extends DateMathExpression> DateMathInterval<T> dateInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {
        return new DateMathInterval<>(name, start, end, includeStart, includeEnd);
    }

    @Override
    public String toString(){
        final String serializeFacet = "" +
                "\"%s\":{" +
                "\"type\":\"%s\","+
                "\"start\":\"%s\","+
                "\"includeStart\":%s,"+
                "\"end\":\"%s\","+
                "\"includeEnd\":%s"+
                "}";
        return String.format(serializeFacet,
                this.name,
                this.getClass().getSimpleName(),
                this.start,
                this.includeStart,
                this.end,
                this.includeEnd
        );
    }

    public static class NumericInterval<T extends Number> extends Interval<T> {
        protected NumericInterval(String name, T start, T end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
        protected NumericInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.includeStart = includeStart;
            this.includeEnd = includeEnd;
        }
    }

    public static class UtilDateInterval<T extends Date> extends Interval<T> {
        public UtilDateInterval(String name, T start, T end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
        public UtilDateInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.includeStart = includeStart;
            this.includeEnd = includeEnd;
        }

        public long getTimeStampStart() {
            return ((Date)this.getStart()).toInstant().getEpochSecond();
        }

        public long getTimeStampEnd() {
            return ((Date)getEnd()).toInstant().getEpochSecond();
        }
    }

    public static class ZonedDateTimeInterval<T extends ZonedDateTime> extends Interval<T> {
        public ZonedDateTimeInterval(String name, T start, T end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }
        public ZonedDateTimeInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {
            this.name = name;
            this.start = start;
            this.end = end;
            this.includeStart = includeStart;
            this.includeEnd = includeEnd;
        }

        public long getTimeStampStart() {
            return ((ZonedDateTime)this.getStart()).toInstant().getEpochSecond();
        }

        public long getTimeStampEnd() {
            return ((ZonedDateTime)getEnd()).toInstant().getEpochSecond();
        }
    }

    public static class DateMathInterval<T extends DateMathExpression> extends Interval<T> {
        public DateMathInterval(String name, T start, T end) {
            this.name = name;
            this.start = start;
            this.end = end;
        }

        public DateMathInterval(String name, T start, T end, boolean includeStart, boolean includeEnd) {

            this.name = name;
            this.start = start;
            this.end = end;
            this.includeStart = includeStart;
            this.includeEnd = includeEnd;
        }

        public long getTimeStampStart() {
            return this.getStart().getTimeStamp();
        }

        public long getTimeStampEnd() {
            return getEnd().getTimeStamp();
        }

        public static class UtilDateDateMathInterval<T extends Date> extends DateMathInterval {
            public UtilDateDateMathInterval(String name, DateMathExpression start, DateMathExpression end) {
                super(name,start,end);
            }

            public UtilDateDateMathInterval(String name, DateMathExpression start, DateMathExpression end, boolean includeStart, boolean includeEnd) {
                super(name,start,end,includeStart,includeEnd);
            }
        }

        public static class ZoneDateTimeDateMathInterval<T extends ZonedDateTime>  extends DateMathInterval {
            public ZoneDateTimeDateMathInterval(String name, DateMathExpression start, DateMathExpression end) {
                super(name,start,end);
            }

            public ZoneDateTimeDateMathInterval(String name, DateMathExpression start, DateMathExpression end, boolean includeStart, boolean includeEnd) {
                super(name,start,end,includeStart,includeEnd);
            }
        }
    }
}
