package com.rbmhtechnology.vind.utils;

public class SpecialCharacterEscaping {

    public static final String[] RESERVED_CHARACTERS = new String[] {
        "\\", // having this at the beginning is important to not escape \ which are used for escaping the following characters
        "+",
        "-",
        "=",
        "&",
        "|",
        "!",
        "(",
        ")",
        "{",
        "}",
        "[",
        "]",
        "^",
        "\"",
        "~",
        "*",
        "?",
        ":",
        "/",
        "<",
        ">",
    };

    private SpecialCharacterEscaping() {
    }

    public static String escapeSpecialCharacters(final String input) {
        String intermediateEscaping = input;
        for (String reservedCharacter : RESERVED_CHARACTERS) {
            intermediateEscaping = intermediateEscaping.replace(reservedCharacter, "\\"+reservedCharacter);
        }
        return intermediateEscaping;
    }
}
