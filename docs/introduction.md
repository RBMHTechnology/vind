## 1. Introduction

Vind (faɪnd) is a modular Java library which aims to lower the hurdle of integrating information discovery facilities in Java projects.
It should help programmers to come to a good solution in an assessable amount of time, improve the 
maintainability of software projects, and simplify a centralized information discovery service management including monitoring and reporting.

In Vind we try to design an API which follows this 3 design principles:

**1. Versatility:** Vind will be used in many different projects, so it was an aim to keeping the dependency footprint small, 
which avoids version-clashes in the downstream projects.

**2. Backend Agnostic:** Wherever possible and feasible, the library has to abstracted from the basic search framework. This enabled us to change the
backend without migrating application software.

**3. Flat learning curve:** It was an aim to keep the learning curve rather flat, so we tried to use Java built-in constructs whenever possible. Additionally
we tried to follow the concept: easy things should be easy, complex things can (but does not have to) be complex.

The search lib is modular and currently implements the following layers:

![Vind Architecture](./images/layer_cake.png)

We built a short tutorial to give you a smooth entry to all the functions of the lib.
The runnable code for each step can be found under `demo/demo-step{number}`.

For a deeper dive in the API of Vind [have a look at the Javadoc](https://www.javadoc.io/doc/com.rbmhtechnology.vind/vind).


