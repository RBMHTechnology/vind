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

    private String searchAfter;
    private final Integer size;
    private final long minutesKeptAlive;

    public Cursor(long minutesKeptAlive, Integer size) {
        if(minutesKeptAlive <= 0 ) {
            log.error("Minutes kept alive can not be lower or equals than 0: {}", minutesKeptAlive);
            throw new IllegalArgumentException("Minutes kept alive can not be lower or equals than 0: " + minutesKeptAlive);
        }
        if(size <= 0 ) {
            log.error("Cursor window size can not be lower or equals than 0: {}", size);
            throw new IllegalArgumentException("Cursor window size can not be lower or equals than 0: " + size);
        }
        this.minutesKeptAlive = minutesKeptAlive;
        this.size = size;
        type = DivisionType.cursor;
    }

    public Cursor(String searchAfter, long minutesKeptAlive, Integer size) {
        if(minutesKeptAlive <= 0 ) {
            log.error("Minutes kept alive can not be lower or equals than 0: {}", minutesKeptAlive);
            throw new IllegalArgumentException("Minutes kept alive can not be lower or equals than 0: " + minutesKeptAlive);
        }
        if(size <= 0 ) {
            log.error("Cursor window size can not be lower or equals than 0: {}", size);
            throw new IllegalArgumentException("Cursor window size can not be lower or equals than 0: " + size);
        }
        this.searchAfter = searchAfter;
        this.size = size;
        this.minutesKeptAlive = minutesKeptAlive;
        type = DivisionType.cursor;
    }


    public long getMinutesKeptAlive() {
        return minutesKeptAlive;
    }

    public String getSearchAfter() {
        return searchAfter;
    }

    public void setSearchAfter(String searchAfter) {
        this.searchAfter = searchAfter;
    }

    public int getSize() {
        return size;
    }

    @Override
    public ResultSubset copy() {
        return new Cursor(this.searchAfter, this.minutesKeptAlive, this.size);
    }

    @Override
    public String toString(){
        final String scoreString = "{" +
                "\"cursor\":%s," +
                "\"windowSize\":%s," +
                "\"minutsKeptAlive\":%s" +
                "}";
        return String.format(scoreString,this.searchAfter, this.size, this.minutesKeptAlive);
    }
}
