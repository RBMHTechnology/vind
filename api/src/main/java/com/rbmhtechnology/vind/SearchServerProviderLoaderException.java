package com.rbmhtechnology.vind;


/**
 */
public class SearchServerProviderLoaderException extends SearchServerException {

    private  Class serverClass;

    public SearchServerProviderLoaderException(String m, Class serverClass) {
        super(m);
        this.serverClass = serverClass;
    }

    public SearchServerProviderLoaderException(String m, Class serverClass, Throwable t) {
        super(m,t);
        this.serverClass = serverClass;
    }

    public Class getServerClass() {
        return serverClass;
    }
}
