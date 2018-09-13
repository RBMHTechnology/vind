mas---
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
