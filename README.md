# VIND

VIND (faɪnd) is a modular Java library that bundles functionality in the area of search. The lib was build to enable 
the integration of search facilities in several projects without getting to deep into the 
search topic. It should help programmers to come to a good solution in an assessable amount of time, improve the 
maintainability of software projects, and simplify a centralized search service management including monitoring and reporting.
 
## Design principles

The work tried to consider the following design issues:

**1. Versatility:** Vind will be used in many different projects, so it was an aim to keeping the dependency footprint small, 
which avoids version-clashes in the downstream projects.

**2. Backend Agnostic:** Wherever possible and feasible, the library has to abstracted from the basic search framework. This enabled us to change the
backend without migrating application software.

**3. Flat learning curve:** It was an aim to keep the learning curve rather flat, so we tried to use Java built-in constructs whenever possible. Additionally
we tried to follow the concept: easy things should be easy, complex things can (but does not have to) be complex.

The search lib is modular and implements the following layers:

![Search Lib Architecture](./docs/images/layer_cake.png)

## How to use

The modules of the VIND lib are provided as Maven artifacts and thus can be seamlessly integrated in new and existing Java Software
projects. VIND decouples API and the *real* indexing components. The first backend which is also the reference implementation is build
on top of [Apache Solr](http://lucene.apache.org/solr/). The lib integrates an in-memory indexer on top of an Embedded Solr Server 
which enables developers to start without setting up a complex infrastructure. Furthermore VIND includes a backend maintainance component
which makes it easy to setup VIND index collections and keep them in sync with the VIND version.

[Get a detailed documentation of all functions and features](https://rbmhtechnology.github.io/vind/) 
or [dive deeper in the API of the VIND with Javadoc](https://rbmhtechnology.github.io/vind/javadoc/). 

## How to contribution

VIND is an Open Source project so everyone is encouraged to improve it. Don't hesitate to report bugs, provide fixes or
share new ideas with us. We have various ways for contribution:

* use the issue tracker - report bugs, suggest features or give hints how we can improve the documentation.
* discuss issues with the community - two brains are better than one.
* write code - no patch is too small. So even fixing typos helps to improve VIND.

## License
Free use of this software is granted under the terms of the Apache License Version 2.0.
See the [License](LICENSE.txt) for more details.

## Authors
VIND is lead by [Red Bull Media House Technology](https://github.com/RBMHTechnology) and was initiated in 2017.

## Changelog
The [Changelog](./docs/changelog.adoc) provides a complete list of changes in older releases.
