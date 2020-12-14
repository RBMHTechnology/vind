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

    private Object[] searchAfter;
    private final Integer windowSize;
    private final long minutesKeptAlive;

    public Cursor(long minutesKeptAlive, Integer windowSize) {
        if(minutesKeptAlive <= 0 ) {
            log.error("Minutes kept alive can not be lower or equals than 0: {}", minutesKeptAlive);
            throw new IllegalArgumentException("Minutes kept alive can not be lower or equals than 0: " + minutesKeptAlive);
        }
        if(windowSize <= 0 ) {
            log.error("Cursor window size can not be lower or equals than 0: {}", windowSize);
            throw new IllegalArgumentException("Cursor window size can not be lower or equals than 0: " + windowSize);
        }
        this.minutesKeptAlive = minutesKeptAlive;
        this.windowSize = windowSize;
        type = DivisionType.cursor;
    }

    public Cursor(Object[] searchAfter, long minutesKeptAlive, Integer windowSize) {
        if(minutesKeptAlive <= 0 ) {
            log.error("Minutes kept alive can not be lower or equals than 0: {}", minutesKeptAlive);
            throw new IllegalArgumentException("Minutes kept alive can not be lower or equals than 0: " + minutesKeptAlive);
        }
        if(windowSize <= 0 ) {
            log.error("Cursor window size can not be lower or equals than 0: {}", windowSize);
            throw new IllegalArgumentException("Cursor window size can not be lower or equals than 0: " + windowSize);
        }
        this.searchAfter = searchAfter;
        this.windowSize = windowSize;
        this.minutesKeptAlive = minutesKeptAlive;
        type = DivisionType.cursor;
    }


    public long getMinutesKeptAlive() {
        return minutesKeptAlive;
    }

    public Object[] getSearchAfter() {
        return searchAfter;
    }

    public void setSearchAfter(Object[] searchAfter) {
        this.searchAfter = searchAfter;
    }

    public int getWindowSize() {
        return windowSize;
    }

    @Override
    public ResultSubset copy() {
        return new Cursor(this.searchAfter, this.minutesKeptAlive, this.windowSize);
    }

    @Override
    public String toString(){
        final String scoreString = "{" +
                "\"cursor\":%s," +
                "\"windowSize\":%s," +
                "\"minutsKeptAlive\":%s" +
                "}";
        return String.format(scoreString,this.searchAfter, this.windowSize, this.minutesKeptAlive);
    }
}
