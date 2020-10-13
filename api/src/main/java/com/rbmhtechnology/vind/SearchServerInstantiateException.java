package com.rbmhtechnology.vind;

/**
 */
public class SearchServerInstantiateException extends SearchServerException {

    private  Class serverClass;

    public SearchServerInstantiateException(String m, Class serverClass) {
        super(m);
        this.serverClass = serverClass;
    }

    public SearchServerInstantiateException(String m, Class serverClass, Throwable t) {
        super(m,t);
        this.serverClass = serverClass;
    }

    public Class getServerClass() {
        return serverClass;
    }
}
