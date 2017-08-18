package com.rbmhtechnology.vind.api.query.division;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API class for query slice result (offset + slice size).
 * @author Alfonso Noriega Meneses
 * @since 31.03.17.
 */
public class Slice extends ResultSubset {

    private Logger log = LoggerFactory.getLogger(getClass());

    private int offset, sliceSize;

    public Slice(int offset, int size) {
        if(offset < 0 || size < 0) {
            log.error("Offset and size numbers can not be lower than 0: offset - {},  size - {}",offset, size);
            throw new IllegalArgumentException("Offset and size should not be a negative value: " + offset+ ", "+size);
        }
        this.offset = offset;
        this.sliceSize = size;
        type = DivisionType.slice;
    }

    /**
     * Gets the actual offset number.
     * @return int offset number.
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the actual slice size number.
     * @return int slice size number.
     */
    public int getSliceSize() {
        return sliceSize;
    }

    @Override
    public ResultSubset copy() {
        return new Slice(this.offset,this.sliceSize);
    }

    @Override
    public String toString(){
        final String scoreString = "{" +
                "\"sliceSize\":%s," +
                "\"offset\":%s" +
                "}";
        return String.format(scoreString,this.sliceSize,this.offset);
    }
}
