# Collection Management Tool

This tool is build for creating and updating searchlib collections in *solr cloud* of or to a specific version.
This includes also the loading of runtime libs.

## Usage

* build a runnable jar with `mvn package` 
* run `java -jar target/collection-managment-tool-{version}.jar` in order to get more information

## For beginners

* download solr (version 5.5.4)
* start it in cloud mode, the easiest ways is to use the `./bin/solr start -c` (one node)
* create a collection, e.g. `java -jar build/libs/collection-managment-tool-2.0.15.jar -cc test -from com.rbmhtechnology.vind:backend-solr:1.0.2 -in localhost:9983`
* update a collection, e.g. `java -jar build/libs/collection-managment-tool-2.0.15.jar -uc test -from com.rbmhtechnology.vind:backend-solr:1.0.2 -in localhost:9983`
