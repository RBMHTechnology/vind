package com.rbmhtechnology.vind.api;

import com.rbmhtechnology.vind.annotations.Facet;
import com.rbmhtechnology.vind.annotations.FullText;
import com.rbmhtechnology.vind.annotations.Type;
import com.rbmhtechnology.vind.annotations.language.Language;

import java.util.List;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 17.06.16.
 */
@Type(name="asset")
public class Asset {

    @FullText(language = Language.German, boost = 2)
    private String title;

    @FullText(language = Language.German)
    private String text;

    @FullText(language = Language.German)
    @Facet
    private List<String> category;

}
