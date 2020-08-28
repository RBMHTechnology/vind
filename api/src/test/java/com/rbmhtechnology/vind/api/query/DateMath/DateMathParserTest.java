package com.rbmhtechnology.vind.api.query.DateMath;

import com.rbmhtechnology.vind.api.query.datemath.DateMathExpression;
import com.rbmhtechnology.vind.api.query.datemath.DateMathParser;
import org.junit.Test;

import java.text.ParseException;
import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateMathParserTest {

    public static final String NOW_DAY_1_DAY = "NOW/DAY+1DAY";
    public static final String NOW_DAY_14_DAYS = "NOW/DAY-14DAYS";

    @Test
    public void dateMathParserTest() throws ParseException {
        final DateMathParser dateMathParser = new DateMathParser();

        final DateMathExpression tomorrowExpression = dateMathParser.parseMath(NOW_DAY_1_DAY);
        assertEquals(NOW_DAY_1_DAY, tomorrowExpression.toString());

        final DateMathExpression fifteenDaysAgo = dateMathParser.parseMath(NOW_DAY_14_DAYS);
        assertEquals(NOW_DAY_14_DAYS, fifteenDaysAgo.toString());


        final Duration plusOneDay = dateMathParser.parseMathGap("+1DAY");
        assertTrue(true);
    }
}
