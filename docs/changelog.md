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
