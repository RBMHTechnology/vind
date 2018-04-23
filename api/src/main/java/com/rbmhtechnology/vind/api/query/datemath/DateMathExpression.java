package com.rbmhtechnology.vind.api.query.datemath;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.rbmhtechnology.vind.api.query.datemath.DateMathExpression.TimeUnit.YEAR;

/**
 * Created by fonso on 02.02.17.
 */
public class DateMathExpression {

    public static final String OP_ADD = "+";
    public static final String OP_SUB = "-";
    public static final String OP_UNIT = "/";
    public static final String NOW = "NOW";
    private RootTime root;
    private TimeUnit unit;
    private List<DateMathOperation> add = new ArrayList<>();
    private List<DateMathOperation> sub = new ArrayList<>();

    public DateMathExpression() {
        this.root = new RootTime();
    }

    public DateMathExpression(ZonedDateTime rootTime) {
        this.root = new RootTime(rootTime);
    }

    public DateMathExpression add(long time, TimeUnit unit) {
        add.add(new DateMathOperation(time, unit));
        return this;
    }

    public DateMathExpression sub(long time, TimeUnit unit) {
        sub.add(new DateMathOperation(time, unit));
        return this;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }

    public void setRootUnit(TimeUnit unit) {
        this.root.setUnit(unit);
    }

    public RootTime getRoot() {
        return root;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public List<DateMathOperation> getAdd() {
        return add;
    }

    public List<DateMathOperation> getSub() {
        return sub;
    }

    public long getTimeStamp() {
        return this.getTimeStamp(ZonedDateTime.now());
    }

    public long getTimeStamp(ZonedDateTime referenceTime) {

        //Get the fixed base date or use the provided one
        ZonedDateTime baseTime;
        if (this.root.isRelative()){
            baseTime = referenceTime;
        } else {
            baseTime = this.root.getFixedTime();
        }

        // Truncate date to specified time unit
        if (Objects.nonNull (this.root.unit)) {
            switch (this.root.unit) {
                case YEAR: baseTime = baseTime.truncatedTo(ChronoUnit.YEARS);
                case YEARS: baseTime = baseTime.truncatedTo(ChronoUnit.YEARS);
                case MONTH: baseTime = baseTime.truncatedTo(ChronoUnit.MONTHS);
                case MONTHS: baseTime = baseTime.truncatedTo(ChronoUnit.MONTHS);
                case DAY: baseTime = baseTime.truncatedTo(ChronoUnit.DAYS);
                case DAYS: baseTime = baseTime.truncatedTo(ChronoUnit.DAYS);
                case HOUR: baseTime = baseTime.truncatedTo(ChronoUnit.HOURS);
                case HOURS: baseTime = baseTime.truncatedTo(ChronoUnit.HOURS);
                case MINUTE: baseTime = baseTime.truncatedTo(ChronoUnit.MINUTES);
                case MINUTES: baseTime = baseTime.truncatedTo(ChronoUnit.MINUTES);
                case SECOND: baseTime = baseTime.truncatedTo(ChronoUnit.SECONDS);
                case SECONDS: baseTime = baseTime.truncatedTo(ChronoUnit.SECONDS);
                case MILLI: baseTime = baseTime.truncatedTo(ChronoUnit.MILLIS);
                case MILLIS: baseTime = baseTime.truncatedTo(ChronoUnit.MILLIS);
                case MILLISECOND: baseTime = baseTime.truncatedTo(ChronoUnit.MILLIS);
                case MILLISECONDS: baseTime = baseTime.truncatedTo(ChronoUnit.MILLIS);
            }
        }

        ZonedDateTime calculatedTime = baseTime;

        //Apply all the add operations
        for (DateMathOperation addOp : this.add) {
            switch (addOp.unit) {
                case YEAR: calculatedTime = baseTime.plusYears(addOp.quantity);
                    break;
                case YEARS: calculatedTime = baseTime.plusYears(addOp.quantity);
                    break;
                case MONTH: calculatedTime = baseTime.plusMonths(addOp.quantity);
                    break;
                case MONTHS: calculatedTime = baseTime.plusMonths(addOp.quantity);
                    break;
                case DAY: calculatedTime = baseTime.plusDays(addOp.quantity);
                    break;
                case DAYS: calculatedTime = baseTime.plusDays(addOp.quantity);
                    break;
                case HOUR: calculatedTime = baseTime.plusHours(addOp.quantity);
                    break;
                case HOURS: calculatedTime = baseTime.plusHours(addOp.quantity);
                    break;
                case MINUTE: calculatedTime = baseTime.plusMinutes(addOp.quantity);
                    break;
                case MINUTES: calculatedTime = baseTime.plusMinutes(addOp.quantity);
                    break;
                case SECOND: calculatedTime = baseTime.plusSeconds(addOp.quantity);
                    break;
                case SECONDS: calculatedTime = baseTime.plusSeconds(addOp.quantity);
                    break;
                case MILLI: calculatedTime = baseTime.plus(addOp.quantity, ChronoUnit.MILLIS);
                    break;
                case MILLIS: calculatedTime = baseTime.plus(addOp.quantity, ChronoUnit.MILLIS);
                    break;
                case MILLISECOND: calculatedTime = baseTime.plus(addOp.quantity, ChronoUnit.MILLIS);
                    break;
                case MILLISECONDS: calculatedTime = baseTime.plus(addOp.quantity, ChronoUnit.MILLIS);
                    break;
            }
        }

        for(DateMathOperation subOp : this.sub) {
            switch (subOp.unit) {
                case YEAR: calculatedTime = baseTime.minusYears(subOp.quantity);
                    break;
                case YEARS: calculatedTime = baseTime.minusYears(subOp.quantity);
                    break;
                case MONTH: calculatedTime = baseTime.minusMonths(subOp.quantity);
                    break;
                case MONTHS: calculatedTime = baseTime.minusMonths(subOp.quantity);
                    break;
                case DAY: calculatedTime = baseTime.minusDays(subOp.quantity);
                    break;
                case DAYS: calculatedTime = baseTime.minusDays(subOp.quantity);
                    break;
                case HOUR: calculatedTime = baseTime.minusHours(subOp.quantity);
                    break;
                case HOURS: calculatedTime = baseTime.minusHours(subOp.quantity);
                    break;
                case MINUTE: calculatedTime = baseTime.minusMinutes(subOp.quantity);
                    break;
                case MINUTES: calculatedTime = baseTime.minusMinutes(subOp.quantity);
                    break;
                case SECOND: calculatedTime = baseTime.minusSeconds(subOp.quantity);
                    break;
                case SECONDS: calculatedTime = baseTime.minusSeconds(subOp.quantity);
                    break;
                case MILLI: calculatedTime = baseTime.minus(subOp.quantity, ChronoUnit.MILLIS);
                    break;
                case MILLIS: calculatedTime = baseTime.minus(subOp.quantity, ChronoUnit.MILLIS);
                    break;
                case MILLISECOND: calculatedTime = baseTime.minus(subOp.quantity, ChronoUnit.MILLIS);
                    break;
                case MILLISECONDS: calculatedTime = baseTime.minus(subOp.quantity, ChronoUnit.MILLIS);
                    break;
            }
        }

        return calculatedTime.toEpochSecond();
    }

    @Override
    public String toString() {
        String output = this.root.toString();

        if(!add.isEmpty()) {
            output += OP_ADD + add.stream().map(Object::toString).collect(Collectors.joining(OP_ADD));
        }
        if(!sub.isEmpty()){
            output += OP_SUB + sub.stream().map(Object::toString).collect(Collectors.joining("-"));
        }
        if(unit!=null) {
            output+= OP_UNIT + unit;
        }

        return output;
    }

    public class RootTime {
        private final boolean relative;
        private ZonedDateTime fixedTime;
        private TimeUnit unit;

        protected RootTime(){
            relative = true;
        }
        protected RootTime(ZonedDateTime time){
            relative = false;
            fixedTime = time;
        }

        public boolean isRelative() {
            return relative;
        }

        public ZonedDateTime getFixedTime() {
            return fixedTime;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public RootTime setUnit(TimeUnit unit) {
            this.unit = unit;
            return this;
        }
        @Override
        public String toString() {
            String output = "";
            if (this.relative) {
                output += NOW;
            } else {
                output += this.fixedTime.format(DateTimeFormatter.ISO_INSTANT);
            }
            if (this.unit != null) {
                output+= OP_UNIT + this.unit;
            }
            return output;
        }
    }

    protected class DateMathOperation {
        private final long quantity;
        private final TimeUnit unit;

        protected DateMathOperation(long quantity, TimeUnit unit) {
            this.quantity = quantity;
            this.unit = unit;
        }

        public long getQuantity() {
            return quantity;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        @Override
        public String toString(){
            return String.valueOf(quantity) + unit;
        }
    }
    public enum TimeUnit {
        YEAR,MONTH,DAY,HOUR,MINUTE,SECOND,MILLI,MILLISECOND,YEARS,MONTHS,DAYS,HOURS,MINUTES,SECONDS,MILLIS,MILLISECONDS
    }

}
