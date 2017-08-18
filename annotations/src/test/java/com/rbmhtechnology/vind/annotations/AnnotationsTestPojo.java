package com.rbmhtechnology.vind.annotations;

import com.rbmhtechnology.vind.annotations.language.Language;

import java.util.List;

import static com.rbmhtechnology.vind.annotations.util.FunctionHelpers.*;

/**
 * Created by fonso on 6/15/16.
 */

@Type(name="TypeTestAnnotation")
public class AnnotationsTestPojo {

    @Id(prefix = "pref")
    public String idAnnotated;

    @Field(name = "FieldAnnotated",indexed = false, stored = false)
    public String testFieldAnnotated;

    @Field
    public String testFieldDefaultAnnotated;

    @FullText(language = Language.English)
    public String testFullTextdAnnotated;

    @Ignore
    public String testihnoretAnnotated;

    @Facet
    public String testFacetDefaultAnnotated;

    @Facet(suggestion = false)
    public String testFacetAnnotated;

    @Field
    @Facet
    public String testMultipleAnnotated;

    @Score
    public float testScoreAnnotated;

    @ComplexField(store = @Operator(function = GetterFunction.class, fieldName = "title"))
    public Taxonomy taxonomy;




    public static class Taxonomy {

        private List<String> terms;
        private String title;
        private String id;

        public Taxonomy(List<String> terms, String title, String id) {
            this.terms = terms;
            this.title = title;
            this.id = id;
        }

        public List<String> getTerms() {
            return terms;
        }

        public String getTitle() {
            return title;
        }

        public String getId() {
            return id;
        }
    }
}
