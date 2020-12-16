---
layout: rbmht-layout
---

## Changelog

This document shows the changes that are introduced into Vind with the releases.

## Pre 1.0.0
For information about older releases check the [history on GitHub](https://github.com/RBMHTechnology/vind/commits/master).

## 1.0.0
* Full package renaming from its original Searchlib structure.
* Included _suggestion-handler_ module and configured **Vind** Solr backend to use it by default.

## 1.0.1
* Bugfix: Suggestion handler and Solr backend runtimelib configuration issue in tests fixed.

## 1.0.2
* Collection manager improvements: Improved logging.
* Reduced logging level to avoid not helpful noise in the logs.
* Reduced solr calls in Vind updates of documents with nested documents.
* Added the possibility of faceting on nested documents fields.
* BugFix: Error in solr _schema.xml_.**multi stored filter** field was not defined as stored.
* BugFix: solved issue with long queries by using POST method in Solr requests instead of GET.

## 1.0.3
* BugFix: nested document facet queries updated to use `edismax` and added missing parenthesis to nested document search query.
* BugFix: non escaped characters in nested document Json facet query.
* BugFix: changed return type of `searchServer` getById method for annotated java pojos to `BeanGetResult`.
* BugFix: avoid document duplication in index (Solr issue with coexisting documents and documents with nested docs).

## 1.1.0
* Full renaming from reporting to monitoring module.
* First stable version of monitoring model.
* Added monitoring log writer.
* Added monitoring ElasticSearch writer.
* Added first monitoring analyzer: report, able to write Json or HTML reports.
* Added to `SearchServer` a getRawQuery method to get the backend specific query.
* Added the possibility ti configure by config properties the backend connection timeout and read timeout.
* BugFix: Added real deep copy of fulltext search object.
* BugFix: Added missing search facet limit to term facet Json implementation.

## 1.1.1
* BugFix: added mixIn to properly serialize DateMathExpression objects and its sub objects.
* __API change__: deprecated method `getName()` from Facet API. Use `getFacetName()` instead when
looking for the Facet name or `getFieldName()`.
* Simplified InteractionEntry data model.
* Added monitoring to Index, Real time Get, Update and Delete actions from monitoringServer.
* Added _silent_ option to _MonitoringServer_ allowing to ignore unnecessary exceptions thrown during monitoring, which do not affect
search execution. By default the _MonitoringServer_ silent is set to **false**, and this can be changed with the method _MonitoringServer.setSilent(boolean)_.

## 1.1.2
* BugFix: Reduced logging level from error to debug of _Filter.getScope(FieldDescriptor descriptor)_ when falling back to
default scope due to a null FieldDescriptor.
* Added helpful debug logging in _MonitoringServer_.

## 1.1.3
* BugFix: CollectionManagementService now closes the SolrCloudClient connection
* BugFix: Solved issue with javadoc version
* BugFix: Homogenize monitoring property types to return always a number(some interval facets where giving a start date or a number depending on the original
   field descriptor type).

## 1.1.4
* SuggestionHandler code cleaning and improvements in logging and Exception handling.
* Bugfix: Fixed issue when adding and filters.

## 1.2.0
* General improvements on reporting analyzer module and monitoring.
* Updated suggestion handler result to NamedList in order to be more homogeneous with Solr result types.
* Added support to Vind configuration via environmental variables.
* Bugfix: Added to NOT filter Solr serialization a base '\*:\*' to ensure the subtraction has a positive base operator.
* Bugfix: solved issue with wrong parsing of children doc filters for suggestions on fields members of both, parent and nested doc.
* Bugfix: Removed hard coded logical operator on suggestion handler, allowing to override it by parameter or configuration.

## 1.2.1
* Changed configuration properties preference order (from most relevant to less): Environment properties > provided config file > default config file.
* BugFix: suggestion handler maps every different type of single quote to the same (the same goes for double quotes and other special characters).
* BugFix: Solved issue with connection timeout when creating reports.

## 1.2.2
* Bugfix #46: fixed issue in children filter serialization normalizing the filters to logical DNF and grouping them afterwards by hierarchical scope.
* Improvement #51: removed ElasticSearch Jest client dependency from _monitoring-api_ module by extracting it to a new _monitoring-utils_ module.
* Improvement #50: added helper methods to create _before_, _after_ and _between_ filters from java util Date.
* Feature #45: added a new filter _TermsQueryFilter_ which enables the possibility to do huge comma separated term filters by the means of the _TermsQuery_ parser in Solr.
* Bugfix #43: fixed preprocessing from analysis report to add _final_ flag to fulltext queries which the last entry of a session and have no following closing action (like selection of a document or restarting of the search).
* Feature #53: Added field analysis handler.

## 1.2.3
* Updated Solr dependencies to 5.5.5.
* Improvement #60: Added health check functionality.
* Feature #57: Added the possibility to define more than one _and_ or _or_ children searches.
* Bugfix #62: Fixed issue which enforced collections created with Vind release <= 1.2.0 to be completely deleted and re-indexed in order to don't throw an exception on suggestions.

## 1.2.4
* Fix #64: removed suggestion string field copy rule to suggestion analyzed field from schema.

## 1.2.5
* Fix #66: Fixed normalization of AND filters containing just OR children filters.

## 1.3.0
* Improvement #68: Documented and applied new release policy.
* Changed suggestion handler internal operator from AND to OR as default.

## 2.0.0
* Upgrade to Solr7.5.
* Ensure that number of the page is 1 or bigger.
* Feature #73: Support index Within.
* Improvement #67: Provide Docker-Image for Solr Backend.

# 2.1.0
* Feature #87:Added the possibility to set autoAddReplicas to true for the collection manager tool
* Bugfix: avoid null pointer exception whe updating a non existing doc.
* Bugfix: #86: moved missing suggestion handler fix from Vind 1.x.x to 2.x.x

# 2.1.1
* Hotfix: fixed issue in suggestion handler when q.op was set to `OR`.

# 2.1.2
* Hotfix: Fixed issue introduced by solr inconsistent response types.

# 2.1.3
* Hotfix: Releated to previous fix, remove cast to integer by NumberUtils toInt method.

# 2.1.4
* Hotfix: Reduced log level from info to debug on document update to avoid spaming the logs.

# 2.1.5
* Hotfix: Ping fails against non-cloud solr-backend (#90)

# 2.2.0
* Improvement: Simplified suggestion regex query.
* Improvement: Added word boundary as prefix for words in suggestion handler instead of blank space.
* Improvement: Instead of reading local solr schema as file to do schema check, now reading it as input stream.
* Improvement: Removed invalid characters from file name and path on collection download

# 2.2.1
* Fix: alignment of language specific fields tokenizer with generic text definition

# 2.3.0
* Improvement: Delete by query no longer queries for the ids of the documents as it was not performing for long sets of 
documents matching the delete filters.

# 2.3.1
* Fix: Issue on nested doc deletion when deleting by query.

# 2.4.0
* Improvement: Builders for StatsFacets now have for each `foo()`-method also a `foo(boolean fooEnabled)` method.

# 2.5.0
* Fix: Possible IllegalArgumentException in DocumentFactory (#106)
* Fix: Added base Match all clause to NOT filter serialization (#110)

# 3.0.0
* Feature: Implemented Elasticsearch backend (7.6.1)
* Feature: Smart parse of fulltext search input
* Feature: Reverse search (Only Elasticsearch)
* Feature: Facet sorting
* Improvement: Support basic and ApiKey backend authorization
* Feature: MasterSlaveBackend for parallel indexing 

# 3.0.1
* Improvement: Add support to '.' on field names for elasticsearch backend.
* Improvement: Extended smart parser to support range filters on numeric and date fields.(#158)

# 3.0.2
* Improvement: Added strict parse mode.(#158)
* Bugfix: Fixed Smart parser issues regarding multi clauses and external parentheses.

# 3.0.3
* Bugfix: Refactor smart parser grammar to fix issues on boolean operations within filter values.

# 3.0.4
* Bugfix: Fix quoted literal issues.
* Bugfix: Fixed priority on unary operator grammar.

# 3.0.5
* Improvement: change default search string on elastic backend from * to \*:* in oder to avoid empty results when no fulltext values are indexed.(#163)
* BugFix: Added track_total_hits default to true and configurable in elastic searches to get real number of results over 10000.(#162)
* BugFix: Escape single quotes on painles script literal to avoid errors updating.(#165)

# 3.0.6
* Improvement: produce significant error messages on search server instantiation.(#168)
* BugFix: Term facets results are properly build now.

# 3.0.7
* BugFix: Fixed nested filer scopes on boolean operation filter (AND, OR, NOT).

# 3.0.8
* BugFix: Facet limit <0 is translated to aggregation size Integer.MAX_VALUE on elastic backend.

# 3.0.9
* BugFix: Fix sorting when sort fields do not (yet) exist (#174)
* BugFix: Do not throw Exception when deleting a non existing document (#176)
* BugFix: Allow brackets in fulltext search for Smart Parser (#177)

# 3.0.10
* BugFix: Fix indexing of non-stored complex fields (#175)

# 3.0.11
* BugFix: Fix atomic updates for complex fields (#183)
* Improvement: Add special sorting by score (#184)
* BugFix: Support open parentheses within fulltext smart parser section (#186)

# 3.0.12
* BugFix: Minimum should match elastic unexpected behaviour. (#190)
* Improvement: Ignore _()[]{}'"_ characters in smart parser fulltext search section. (#191)
* BugFix: Atomic update and complex fields name issue. (#189)
* Improvement: Improved update scripts to use parameters for values and fields.

# 3.0.13
* BugFix: Update script issue with dates.

# 3.0.14
* BugFix: Complex field field name creation issue.

# 3.0.15
* BugFix: Avoid null pointer exception on populaltion of complex fields.

# 3.0.16
* BugFix: problem on foorprint creation,missing complex field  

# 3.0.17
* BugFix: Fixed issue on complex field footprint creation.
* Improvement: some refactoring

# 3.0.18
* Improvement: Increase version conflict retries for elastic backend and add a config parameter.
* BugFix: add a null check to avoid null pointer except on complex fields footprint

# 3.0.19
* BugFix: fixed boolean condition in foot print creation

# 3.0.20
* BugFix: Avoid null pointer exception when not storage type defined for complex fields

# 3.0.21
* BugFix: Avoid type check validation for multi field when value is null.

# 3.0.22
* BugFix: Avoid null pointer exception when there is no suggestion results on spellchecked queries.

# 3.0.23
* BugFix: Added check for non collection values when building the default sorting values.

# 3.0.24
* BugFix: fix binary field footprint.

# 3.0.25
* Improvement: Applied max retries config value to delete by query.

# 3.0.26
* Improvement: Added search configuration to delete on version conflict or not.

# 3.0.27
* Improvement: Implemented support fot [* TO *] range.
* bugfix: Made field descriptor aware of context to avoid unknown field error in elastic.

# 3.0.28
* Bugfix: Escaped elastic special characters from search when the syntax is not right.

# 3.0.29
* Bugfix: fulltext search elastic input treated to avoid errors with whitespaces.
* Bugfix: Avoid NPE when doing contextualize search on fields which do not have the 
context. 

# 3.0.30
* BugFix: roll back fulltext search text treatement.
* BugFix: between filter uses datemath to elastic string.
* BugFix: get FieldName now returns Optional to avoid NPE and elastic errors with null fields. 

# 3.0.31
* BugFix: Fix context field issues.
* Improvement: Added better elasticsearch footprint handling based on mapping fields.
* BugFix: fixed issue with spellcheck results. 

# 3.0.32
* Feature: Implemented cursor search for elasticsearch backend.

# 3.1.00
* Improvement: Added cursor reference to individual document for cursored based search(#199).
