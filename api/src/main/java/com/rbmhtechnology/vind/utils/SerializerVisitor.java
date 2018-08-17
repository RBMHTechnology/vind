package com.rbmhtechnology.vind.utils;

import com.rbmhtechnology.vind.api.query.filter.Filter;
import static com.rbmhtechnology.vind.api.query.filter.Filter.*;

/**
 * Created by fonso on 21.07.17.
 */
public interface SerializerVisitor {

    String visit(Filter filter) ;
    String visit(AndFilter filter) ;
    String visit(OrFilter filter) ;
    String visit(NotFilter filter) ;
    String visit(TermFilter filter) ;
    String visit(TermsQueryFilter filter) ;
    String visit(PrefixFilter filter) ;
    String visit(DescriptorFilter filter) ;
    String visit(BeforeFilter filter) ;
    String visit(AfterFilter filter) ;
    String visit(GreaterThanFilter filter) ;
    String visit(LowerThanFilter filter) ;
    String visit(BetweenDatesFilter filter) ;
    String visit(BetweenNumericFilter filter) ;
    String visit(WithinBBoxFilter filter) ;
    String visit(WithinCircleFilter filter) ;
    String visit(NotEmptyTextFilter filter) ;
    String visit(NotEmptyFilter filter) ;
    String visit(NotEmptyLocationFilter filter) ;
    String visit(ChildrenDocumentFilter filter) ;
}
