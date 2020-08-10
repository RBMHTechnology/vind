package com.rbmhtechnology.vind.api.query.filter;

import com.rbmhtechnology.vind.model.DocumentFactory;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 27.06.16.
 */
public interface FilterQuerySerializer {

    String serialize(Filter filter, DocumentFactory factory);
    Filter deserialize(String luceneQuery, DocumentFactory factory);

}
