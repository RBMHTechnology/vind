package com.rbmhtechnology.vind.annotations.language;

/**
 * Supported Languages
 */
public enum Language {

    German("de"),
    English("en"),
    Spanish("es"),
    None(null);

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getLangCode() {
        return code;
    }
}
