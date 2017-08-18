package com.rbmhtechnology.vind.utils.mam;

import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.facet.Facet;
import com.rbmhtechnology.vind.api.query.facet.Facets;
import com.rbmhtechnology.vind.api.query.facet.Interval;
import com.rbmhtechnology.vind.api.query.filter.Filter;
import com.rbmhtechnology.vind.model.*;
import org.apache.solr.util.DateFormatUtil;
import org.apache.solr.util.DateMathParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.rbmhtechnology.vind.api.query.datemath.DateMathExpression.*;

/**
 * Created by fonso on 02.02.17.
 */
public class FacetMapper {
    private static final Logger log = LoggerFactory.getLogger(FacetMapper.class);

    private static final String INTERVAL_TO = "\\[ *(?<start>%s|\\*) +TO +(?<end>%s|\\*) *\\]";


    private static final String SOLR_DATE = "[0-9][0-9][0-9][0-9]\\-(0[1-9]|1[0-2])\\-(0[1-9]|[1-2][0-9]|3[0-1])T";
    private static final String SOLR_TIME = "[0-2][0-9]:[0-5][0-9]:[0-5][0-9](\\.\\d(\\d(\\d)?)?)?Z";
    private static final String SOLR_DATE_TIME = String.format("%s%s",SOLR_DATE,SOLR_TIME);

    private static final String SOLR_NUM = "\\d+";
    private static final String SOLR_CHAR = "\\w+";
    private static final String SOLR_TIME_UNIT = "(YEAR|MONTH|DAY|HOUR|MINUTE|SECOND|MILLI|MILLISECOND)S?";
    public static final String SOLR_MATH_UNITS_GROUP = "(?<mathUnit%s>/"+SOLR_TIME_UNIT+")?";
    public static final String SOLR_ROOT_UNITS_GROUP = "(?<rootUnit%s>/"+SOLR_TIME_UNIT+")?";
    private static final String SOLR_DATE_MATH = SOLR_ROOT_UNITS_GROUP+"((?<timeOps%s>((\\+|\\-)\\d+"+ SOLR_TIME_UNIT +")+)"+ SOLR_MATH_UNITS_GROUP + ")?";
    private static final String SOLR_DATE_MATH_GROUP ="(?<dateMath%s>" + SOLR_DATE_MATH + ")?";
    private static final String SOLR_DATE_ROOT = "(NOW|(?<date%s>"+SOLR_DATE_TIME+"))";

    private static final String SOLR_DATE_EXPRESSION = SOLR_DATE_ROOT +SOLR_DATE_MATH_GROUP;
    private static final String INTERVAL_START_DATE = String.format(SOLR_DATE_EXPRESSION,"Start","Start","Start","Start","Start");
    private static final String INTERVAL_END_DATE = String.format(SOLR_DATE_EXPRESSION,"End","End","End","End","End");
    private static final Pattern INTERVAL_DATE_FACET = Pattern.compile(String.format(INTERVAL_TO,INTERVAL_START_DATE, INTERVAL_END_DATE));

    private static Pattern INTERVAL_NUMERIC_FACET = Pattern.compile(String.format(INTERVAL_TO,SOLR_NUM,SOLR_NUM));

    public static <T> Facet stringQuery2FacetMapper(FieldDescriptor<T> field,String facetName, Map<String,String> query) {

        final String stringQuery = query.values().iterator().next();

        Class<T> type = field.getType();
        if (ComplexFieldDescriptor.class.isAssignableFrom(field.getClass())) {
            type = ((ComplexFieldDescriptor)field).getFacetType();
        }

        if (query.size() == 1 && !intervalDateFacetMatcher(stringQuery) && !INTERVAL_NUMERIC_FACET.matcher(stringQuery).matches()) {
            return queryFacetMapper(facetName,field.getName(), stringQuery);
        } else {
            if (Date.class.isAssignableFrom(type) || ZonedDateTime.class.isAssignableFrom(type)) {

                Interval.DateMathInterval[] intervals = query.keySet().stream()
                        .map(key -> intervalDateFacetMapper(key, query.get(key)))
                        .toArray(Interval.DateMathInterval[]::new);

                if (SingleValueFieldDescriptor.DateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (SingleValueFieldDescriptor.DateFieldDescriptor) field, intervals);
                } else if (SingleValueFieldDescriptor.UtilDateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (SingleValueFieldDescriptor.UtilDateFieldDescriptor) field, intervals);
                } else if (MultiValueFieldDescriptor.DateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (MultiValueFieldDescriptor.DateFieldDescriptor) field, intervals);
                } else if (MultiValueFieldDescriptor.UtilDateFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (MultiValueFieldDescriptor.UtilDateFieldDescriptor) field, intervals);
                }if (MultiValuedComplexField.UtilDateComplexField.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (MultiValuedComplexField.UtilDateComplexField) field, intervals);
                }if (SingleValuedComplexField.UtilDateComplexField.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (SingleValuedComplexField.UtilDateComplexField) field, intervals);
                }if (MultiValuedComplexField.DateComplexField.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (MultiValuedComplexField.DateComplexField) field, intervals);
                }if (SingleValuedComplexField.DateComplexField.class.isAssignableFrom(field.getClass())) {
                    return Facets.interval(facetName, (SingleValuedComplexField.DateComplexField) field, intervals);
                }
                else {
                    throw new RuntimeException("Invalid interval query string definition: '"+query+"'");
                }

            } else if (Number.class.isAssignableFrom(type)){
                Interval.NumericInterval[] intervals = query.keySet().stream()
                        .map(key -> intervalNumericFacetMapper(key, (FieldDescriptor<Number>) field, query.get(key)))
                        .toArray(Interval.NumericInterval[]::new);

                return Facets.interval(facetName,(FieldDescriptor<Number>)field,intervals);
            } else {
                throw new RuntimeException("Invalid facet query string definition: '"+query+"'");
            }
        }
    }

    private static boolean intervalDateFacetMatcher(String query) {
        Matcher dateMatcher = INTERVAL_DATE_FACET.matcher(query);
        if (dateMatcher.matches()) {
            DateMathParser solrDateMathParser = new DateMathParser();
            try {
                String startDateMath = dateMatcher.group("dateMathStart");
                if (startDateMath != null) {
                    solrDateMathParser.parseMath(startDateMath);
                }

                String endDateMath = dateMatcher.group("dateMathStart");
                if (endDateMath != null) {
                    solrDateMathParser.parseMath(endDateMath);
                }
            } catch (ParseException e) {
                return false;
            }
            try {
                String startDate = dateMatcher.group("dateStart");
                if (startDate != null) {
                    DateFormatUtil.parseDate(startDate);
                }

                String endDate = dateMatcher.group("dateEnd");
                if (endDate != null) {
                    DateFormatUtil.parseDate(endDate);
                }

            } catch (ParseException e) {
                return false;
            }
            return true;
        } else {
            return false;
        }

    }

    public static <T> Interval<DateMathExpression> intervalDateFacetMapper(String name, String query) {
        final Matcher dateMatcher = INTERVAL_DATE_FACET.matcher(query);
        if (dateMatcher.matches()) {
            final DateMathParser solrDateMathParser = new DateMathParser();

            DateMathExpression startDateMath = null;
            if(!dateMatcher.group("start").equals("*")) {
                try {
                    String startDateMathText = dateMatcher.group("dateMathStart");
                    if (startDateMathText != null) {
                        solrDateMathParser.parseMath(startDateMathText);
                    }

                    String startDate = dateMatcher.group("dateStart");
                    if (startDate != null) {
                        final Date date = DateFormatUtil.parseDate(startDate);

                        startDateMath = new DateMathExpression(ZonedDateTime.ofInstant(date.toInstant(),
                                ZoneId.systemDefault()));
                    } else {
                        startDateMath = new DateMathExpression();
                    }
                } catch (ParseException e) {
                    return null;
                }

                String rootUnitStart = dateMatcher.group("rootUnitStart");
                if (rootUnitStart != null) {
                    startDateMath.setRootUnit(TimeUnit.valueOf(rootUnitStart.replace("/", "")));
                }

                String mathUnitStart = dateMatcher.group("mathUnitStart");
                if (mathUnitStart != null) {
                    startDateMath.setUnit(TimeUnit.valueOf(mathUnitStart.replace("/","")));
                }

                String timeOpsStart = dateMatcher.group("timeOpsStart");
                if (timeOpsStart != null) {
                    String[] timeOps = timeOpsStart.split("(?=[\\+\\-])");
                    for(String op : timeOps){
                        Long quantity = Long.valueOf(op.replaceAll("[^\\d]", ""));
                        TimeUnit unit = TimeUnit.valueOf(op.replaceAll("[^A-Z]", ""));
                        if (op.startsWith("+")) {
                            startDateMath.add(quantity, unit);
                        }
                        if (op.startsWith("-")) {
                            startDateMath.sub(quantity, unit);
                        }
                    }
                }
            }

            DateMathExpression endDateMath = null ;
            if (!dateMatcher.group("end").equals("*")) {
                try {
                    String endMath = dateMatcher.group("dateMathEnd");
                    if (endMath != null) {
                        solrDateMathParser.parseMath(endMath);
                    }
                    String endDate = dateMatcher.group("dateEnd");
                    if (endDate != null) {
                        final Date date = DateFormatUtil.parseDate(endDate);
                        endDateMath = new DateMathExpression(ZonedDateTime.ofInstant(date.toInstant(),
                                ZoneId.systemDefault()));
                    } else {
                        endDateMath = new DateMathExpression();
                    }

                } catch (ParseException e) {
                    return null;
                }
                String rootUnitEnd = dateMatcher.group("rootUnitEnd");
                if (rootUnitEnd != null) {
                    endDateMath.setRootUnit(TimeUnit.valueOf(rootUnitEnd.replace("/","")));
                }

                String mathUnitEnd = dateMatcher.group("mathUnitEnd");
                if (mathUnitEnd != null) {
                    endDateMath.setUnit(TimeUnit.valueOf(mathUnitEnd.replace("/","")));
                }


                String timeOpsEnd = dateMatcher.group("timeOpsEnd");
                if (timeOpsEnd != null) {
                    String[] timeOps = timeOpsEnd.split("(?=[\\+\\-])");
                    for(String op : timeOps){
                        if (!op.isEmpty()) {
                            Long quantity = Long.valueOf(op.replaceAll("[^\\d]", ""));
                            TimeUnit unit = TimeUnit.valueOf(op.replaceAll("[^A-Z]", ""));
                            if (op.startsWith("+")) {
                                endDateMath.add(quantity, unit);
                            }
                            if (op.startsWith("-")) {
                                endDateMath.sub(quantity, unit);
                            }
                        }
                    }
                }
            }
            //DateMath parsing

            return Interval.dateInterval(name, startDateMath, endDateMath);
        } else {
            throw  new RuntimeException("Invalid interval query string definition: '"+query+"'");
        }
    }

    public static <T extends Number> Interval intervalNumericFacetMapper(String name, FieldDescriptor<T> field, String query) {
        final Matcher numberMatcher = INTERVAL_NUMERIC_FACET.matcher(query);
        if (numberMatcher.matches()) {
            final T start;
            final T end;

            String startValue = numberMatcher.group("start");
            String endValue = numberMatcher.group("end");


            Class<T> type = field.getType();
            if (ComplexFieldDescriptor.class.isAssignableFrom(field.getClass())) {
                type = ((ComplexFieldDescriptor)field).getFacetType();
            }
            if(Long.class.isAssignableFrom(type)) {
                start = !startValue.equals("*")? (T) Long.valueOf(startValue) : null;
                end =!endValue.equals("*")? (T) Long.valueOf(endValue) : null;
            } else if(Integer.class.isAssignableFrom(type)) {
                start = !startValue.equals("*")? (T) Integer.valueOf(startValue) : null;
                end = !endValue.equals("*")? (T) Integer.valueOf(endValue) : null;
            } else if(Double.class.isAssignableFrom(type)) {
                start = !startValue.equals("*")? (T) Double.valueOf(startValue) : null;
                end = !endValue.equals("*")? (T) Double.valueOf(endValue) : null;
            } else  {
                start = !startValue.equals("*")? (T) Float.valueOf(startValue) : null;
                end = !endValue.equals("*")? (T) Float.valueOf(endValue) : null;
            }

            return Interval.numericInterval(name, start, end);
        } else {
            throw new RuntimeException("Invalid number interval string definition: '"+query+"'");
        }
    }

    public static Facet queryFacetMapper(String name, String fieldName, String query) {

        return Facets.query(name, Filter.eq(fieldName,query),null);
    }
}

