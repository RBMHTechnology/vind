#!/usr/bin/env bash

if [ "$1" == "start" ]; then
	echo "check if there is something running on port 8983"

	RSP=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8983/solr/)

	if [  "$RSP" == 200 ]; then
		echo "8983 is already take, stop these process first"
		exit 1
	fi

	if [ "$2" == "cloud" ]; then
		RSP=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:7574/solr/)
		if [  "$RSP" == 200 ]; then
		    echo "7574 is already take, stop these process first"
		    exit 1
	    fi
	fi

	echo "installing and starting solr 5.5.1"
	mkdir -p .remote
	rm -rf .remote/solr-5.5.1

	cd .remote

	if [ ! -f ./solr-5.5.1.zip ]; then
   		echo "solr-5.5.1 does currently not exist. Trying to download."
   		wget "http://archive.apache.org/dist/lucene/solr/5.5.1/solr-5.5.1.zip"
	fi

	echo "unpackage solr"
	unzip solr-5.5.1.zip

	rm solr-5.5.1.zip

	echo "copy core configuration"
	if [ "$2" == "cloud" ]; then
	    echo "start solr cloud"
	    ./solr-5.5.1/bin/solr -e cloud -noprompt
	    ./solr-5.5.1/bin/solr create -c searchindex -d ../backend/solr/src/main/resources/solrhome/core/
	else
		cp -R ../backend/solr/src/main/resources/solrhome/core/ solr-5.5.1/server/solr/core
		echo "start solr"
	    ./solr-5.5.1/bin/solr start
	fi

elif [ "$1" == "stop" ]; then
	echo "try to stop solr instance"
	if [ ! -f .remote/solr-5.5.1 ]; then
		./.remote/solr-5.5.1/bin/solr stop -all
	fi
else
	echo "only 'start', 'start cloud' and 'stop' are supported"
fi


