package com.rbmhtechnology.vind;

import com.rbmhtechnology.vind.api.SearchServer;

/**
 */
public class SearchServerProviderLoaderException extends RuntimeException {

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
