package com.rbmhtechnology.vind.api.query.division;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API class for query paging.
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 24.06.16.
 */
public class Page extends ResultSubset {

    private Logger log = LoggerFactory.getLogger(getClass());

    private int page, pagesize;

    /**
     * Creates a new instance of {@link Page}.
     * @param page int indicating the actual page number.
     * @param pagesize int setting the nu,mber of documents to be shown by each page.
     */
    public Page(int page, int pagesize) {
        if(page < 1) {
            log.error("Page number can not be lower than 1: {}",page);
            throw new IllegalArgumentException("Page must be a positive, but is: " + page);
        }
        this.page = page;
        this.pagesize = pagesize;
        type = DivisionType.page;
    }

    /**
     * Gets the actual page number.
     * @return int page number.
     */
    public int getPage() {
        return page;
    }

    /**
     * Gets the configured number of documents per page.
     * @return int number of documents.
     */
    public int getPagesize() {
        return pagesize;
    }

    /**
     * Gets the number of documents previous to the actual page.
     * @return int number of previous documents.
     */
    public int getOffset() {
        return (page-1)*pagesize;
    }

    /**
     * Gets the next page configuration.
     * @return next {@link Page}.
     */
    public Page next() {
        return new Page(this.page+1, this.pagesize);
    }

    /**
     * Gets the previous page configuration.
     * @return previous {@link Page}.
     */
    public Page previous() {
        return new Page(this.page-1, this.pagesize);
    }

    @Override
    public ResultSubset copy() {
        return new Page(this.page,this.pagesize);
    }

    @Override
    public String toString(){
        final String scoreString = "{" +
                "\"pageSize\":%s," +
                "\"pageNumber\":%s" +
                "}";
        return String.format(scoreString,this.pagesize,this.page);
    }
}
