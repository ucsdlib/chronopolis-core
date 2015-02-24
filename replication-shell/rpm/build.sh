#!/bin/bash

# TODO: Fill the SOURCES folder with the artifacts after a maven build... maybe just execute the mvn install here

rpmbuild -ba --define="_topdir $PWD" --define="_tmppath $PWD/tmp" SPECS/replication-shell.spec
