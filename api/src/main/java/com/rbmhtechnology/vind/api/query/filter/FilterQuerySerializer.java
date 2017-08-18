package com.rbmhtechnology.vind.api.query.filter;

/**
 * @author Thomas Kurz (tkurz@apache.org)
 * @since 27.06.16.
 */
public interface FilterQuerySerializer {

    String serialize(Filter filter);

}
