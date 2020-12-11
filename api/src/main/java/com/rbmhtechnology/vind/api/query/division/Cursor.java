package com.rbmhtechnology.vind.api.query.division;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API class for query cursor result (offset + slice size).
 * @author Alfonso Noriega Meneses
 * @since 11.12.20.
 */
public class Cursor extends ResultSubset {

    private Logger log = LoggerFactory.getLogger(getClass());

    private long minutesKeptAlive;
    private String cursor;

    public Cursor(long minutesKeptAlive) {
        if(minutesKeptAlive <= 0 ) {
            log.error("Minutes kept alive can not be lower or equals than 0: {}", minutesKeptAlive);
            throw new IllegalArgumentException("Minutes kept alive can not be lower or equals than 0: " + minutesKeptAlive);
        }
       this.minutesKeptAlive = minutesKeptAlive;
        type = DivisionType.cursor;
    }

    public Cursor(String cursor, long minutesKeptAlive) {
        if(minutesKeptAlive <= 0 ) {
            log.error("Minutes kept alive can not be lower or equals than 0: {}", minutesKeptAlive);
            throw new IllegalArgumentException("Minutes kept alive can not be lower or equals than 0: " + minutesKeptAlive);
        }
        this.minutesKeptAlive = minutesKeptAlive;
        this.cursor = cursor;
        type = DivisionType.cursor;
    }


    public long getMinutesKeptAlive() {
        return minutesKeptAlive;
    }

    public void setMinutesKeptAlive(long minutesKeptAlive) {
        this.minutesKeptAlive = minutesKeptAlive;
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    @Override
    public ResultSubset copy() {
        return new Cursor(this.cursor, this.minutesKeptAlive);
    }

    @Override
    public String toString(){
        final String scoreString = "{" +
                "\"cursor\":%s," +
                "\"minutsKeptAlive\":%s" +
                "}";
        return String.format(scoreString,this.cursor,this.minutesKeptAlive);
    }
}
