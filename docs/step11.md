## 12. Complex Fields

There are special situations in which having the same value for every scope (storing, fulltext search, filtering,
faceting, suggesting or sorting) may not be enough for the project requirements. Think of the scenario of a taxonomy 
term, with a unique identifier for filtering, a label for storing, sorting or faceting, plus a a set of synonyms 
for full text search and suggestions. Such a situation cannot be covered by the basic field descriptors, and to 
fill in that gap complex field descriptors where created.

A complex field descriptor is a field storing a simplified view of a java class, and which it is declared by providing 
the methods to calculate the values for each of the specific scopes desired.

```java
SingleValuedComplexField.NumericComplexField<Taxonomy,Integer,String> numericComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,Integer,String>()
        .setFacet(true, tx -> Arrays.asList(tx.getId()))
        .setFullText(true, tx -> Arrays.asList(tx.getTerm()))
        .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
        .buildNumericComplexField("numberFacetTaxonomy", Taxonomy.class, Integer.class, String.class);

MultiValuedComplexField.TextComplexField<Taxonomy,String,String> multiComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,String,String>()
        .setFacet(true, tx -> Arrays.asList(tx.getLabel()))
        .setSuggest(true, tx -> Arrays.asList(tx.getLabel()))
        .setStored(true, tx -> tx.getTerm())
        .buildMultivaluedTextComplexField("multiTextTaxonomy", Taxonomy.class, String.class, String.class);
```

The complex field definition has 3 types to be specified, the first one is the complex java class to be stored, in the previous 
example _Taxonomy_. The second one should be the returning type of the facet function, Integer in the example as the Id would be
the value used for the faceting. Finally a 3rd type for the sort scope. Suggestion and fulltext scope will be always expecting a 
String type return function.

*Note*: Facet, FullText and Suggest are design to be always multivalued so the functions providing their values should return an array of 
the expected type.   

### 12.1 Advance Filter

A new scope has been added to the complex filter, which only purpose is to do filtering. This field values should have the same type 
defined for faceting and it is always multivalued.

```java
SingleValuedComplexField.NumericComplexField<Taxonomy,Integer,String> numericComplexField = new ComplexFieldDescriptorBuilder<Taxonomy,Integer,String>()
        .setAdvanceFilter(true, tx -> Arrays.asList(tx.getTerm()))
        .buildNumericComplexField("numberFacetTaxonomy", Taxonomy.class, Integer.class, String.class);

```
  
### 12.2 Scoped filters

As with the complex fields it is possible to have different values for different scopes, the filters support the option to 
specify the scope in which they apply:
* Scope.Facet
* Scope.Filter
* Scope.Suggest

```java
server.execute(Search.fulltext().filter(textComplexField.equals("uno",Scope.Filter)), assets);
```

*Note*: The default scope is facet.
