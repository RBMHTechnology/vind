package com.rbmhtechnology.vind.api.query.datemath;

import com.rbmhtechnology.vind.SearchServerException;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class DateMathParser {
    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final TimeZone DEFAULT_MATH_TZ;
    public static final DateTimeFormatter PARSER;
    public static final Map<String, ChronoUnit> CALENDAR_UNITS;
    private TimeZone zone;
    private Locale loc;
    private Date now;
    private static Pattern splitter;

    private static Map<String, ChronoUnit> makeUnitsMap() {
        Map<String, ChronoUnit> units = new HashMap(13);
        units.put("YEAR", ChronoUnit.YEARS);
        units.put("YEARS", ChronoUnit.YEARS);
        units.put("MONTH", ChronoUnit.MONTHS);
        units.put("MONTHS", ChronoUnit.MONTHS);
        units.put("DAY", ChronoUnit.DAYS);
        units.put("DAYS", ChronoUnit.DAYS);
        units.put("DATE", ChronoUnit.DAYS);
        units.put("HOUR", ChronoUnit.HOURS);
        units.put("HOURS", ChronoUnit.HOURS);
        units.put("MINUTE", ChronoUnit.MINUTES);
        units.put("MINUTES", ChronoUnit.MINUTES);
        units.put("SECOND", ChronoUnit.SECONDS);
        units.put("SECONDS", ChronoUnit.SECONDS);
        units.put("MILLI", ChronoUnit.MILLIS);
        units.put("MILLIS", ChronoUnit.MILLIS);
        units.put("MILLISECOND", ChronoUnit.MILLIS);
        units.put("MILLISECONDS", ChronoUnit.MILLIS);
        return units;
    }

    private static DateMathExpression add(DateMathExpression expression, int val, String unit) {
        ChronoUnit uu = (ChronoUnit)CALENDAR_UNITS.get(unit);
        if (null == uu) {
            throw new IllegalArgumentException("Adding Unit not recognized: " + unit);
        } else {
            expression.add(val, DateMathExpression.TimeUnit.valueOf(unit));
            return expression;
        }
    }
    private static DateMathExpression sub(DateMathExpression expression, int val, String unit) {
        ChronoUnit uu = (ChronoUnit)CALENDAR_UNITS.get(unit);
        if (null == uu) {
            throw new IllegalArgumentException("Adding Unit not recognized: " + unit);
        } else {
            expression.sub(val, DateMathExpression.TimeUnit.valueOf(unit));
            return expression;
        }
    }

    private static DateMathExpression round(DateMathExpression expression, String unit) {
        ChronoUnit uu = (ChronoUnit)CALENDAR_UNITS.get(unit);
        if (null == uu) {
            throw new SearchServerException("Rounding Unit not recognized: " + unit);
        } else {
            expression.setUnit(DateMathExpression.TimeUnit.valueOf(unit));
            return expression;
        }
    }

    private static ZonedDateTime parseNoMath(String val) {
        return  ZonedDateTime.ofInstant(((Instant)PARSER.parse(val, Instant::from)), ZoneId.of(UTC.getID()));
    }

    public TimeZone getTimeZone() {
        return Optional.ofNullable(this.zone).orElse(UTC);
    }

    public void setNow(Date n) {
        this.now = n;
    }

    public Date getNow() {
        if (this.now == null) {
            this.now = new Date();
        }
        return (Date)this.now.clone();
    }

    public DateMathExpression parseMath(String math) {
        if (0 == math.length()) {
            return new DateMathExpression();
        } else {
            String[] ops = splitter.split(math);
            int pos = 0;

            DateMathExpression expression = new DateMathExpression();
            if (ops[0].length() > 1) {
                if (!ops[0].equals("NOW")) {
                    expression = new DateMathExpression(parseNoMath(ops[0]));
                }
                pos++;

                if (ops[pos].equals("/")) {
                    pos++;
                    expression.setRootUnit(DateMathExpression.TimeUnit.valueOf(ops[pos++]));
                }
            }

            while(pos < ops.length) {
                if (1 != ops[pos].length()) {
                    throw new SearchServerException("Multi character command found: \"" + ops[pos] + "\"");
                }

                char command = ops[pos++].charAt(0);
                switch(command) {
                    case '+':
                    case '-':
                        if (ops.length < pos + 2) {
                            throw new SearchServerException("Need a value and unit for command: \"" + command + "\"");
                        }

                        boolean var7 = false;

                        int val;
                        try {
                            val = Integer.parseInt(ops[pos++]);
                        } catch (NumberFormatException var10) {
                            throw new SearchServerException("Not a Number: \"" + ops[pos - 1] + "\"");
                        }

                        String unit = ops[pos++];
                        if ('-' == command) {
                            sub(expression, val, unit);
                        } else {
                            add(expression, val, unit);
                        }
                        break;
                    case ',':
                    case '.':
                    default:
                        throw new SearchServerException("Unrecognized command: \"" + command + "\"");
                    case '/':
                        if (ops.length < pos + 1) {
                            throw new SearchServerException("Need a unit after command: \"" + command + "\"");
                        }

                        try {
                            round(expression, ops[pos++]);
                        } catch (IllegalArgumentException var11) {
                            throw new SearchServerException("Unit not recognized: \"" + ops[pos - 1] + "\"");
                        }
                }
            }

            return expression;
        }
    }

    public Duration parseMathGap(String math) {
        Duration gap = Duration.ofMillis(0);
        if (0 == math.length()) {
            return gap;
        } else {
            String[] ops = splitter.split(math);
            int pos = 0;

            while(pos < ops.length) {
                if (1 != ops[pos].length()) {
                    throw new SearchServerException("Multi character command found: \"" + ops[pos] + "\"");
                }

                char command = ops[pos++].charAt(0);
                switch(command) {
                    case '+':
                    case '-':
                        if (ops.length < pos + 2) {
                            throw new SearchServerException("Need a value and unit for command: \"" + command + "\"");
                        }

                        boolean var7 = false;

                        int val;
                        try {
                            val = Integer.parseInt(ops[pos++]);
                        } catch (NumberFormatException var10) {
                            throw new SearchServerException("Not a Number: \"" + ops[pos - 1] + "\"");
                        }

                        String unit = ops[pos++];
                        if ('-' == command) {
                            gap = gap.minus(Duration.of(val,CALENDAR_UNITS.get(unit)));
                        } else {
                            gap = gap.plus(Duration.of(val,CALENDAR_UNITS.get(unit)));
                        }
                        break;
                    case ',':
                    case '.':
                    default:
                        throw new SearchServerException("Unrecognized command: \"" + command + "\"");
                }
            }
            return gap;
        }
    }

    static {
        DEFAULT_MATH_TZ = UTC;
        PARSER = (new DateTimeFormatterBuilder()).parseCaseInsensitive().parseLenient().appendInstant().toFormatter(Locale.ROOT);
        CALENDAR_UNITS = makeUnitsMap();
        splitter = Pattern.compile("\\b|(?<=\\d)(?=\\D)");
    }
}
