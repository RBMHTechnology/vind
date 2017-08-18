package com.rbmhtechnology.vind;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 22.07.16.
 */
public class SearchServerException extends RuntimeException {

    public SearchServerException(String m) {
        super(m);
    }

    public SearchServerException(String m, Throwable t) {
        super(m,t);
    }

}
