package com.rbmhtechnology.vind.api.query.datemath;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
