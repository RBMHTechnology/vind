package com.rbmhtechnology.vind.test;

import java.util.Optional;

public enum Backend {
    Solr,
    Elastic;

    public boolean isActive() {
        return Optional.ofNullable(System.getenv("VIND.TEST.BACKEND"))
                .map((String s) -> s.contains(this.name().toLowerCase()))
                .orElse(this.equals(Solr));
    }
}
