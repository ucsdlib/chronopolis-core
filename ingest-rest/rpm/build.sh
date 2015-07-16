#!/bin/bash

# TODO: Fill the SOURCES folder with the artifacts after a maven build... maybe just execute the mvn install here
rpmdir=$PWD
sources=SOURCES
finaljar=$sources/ingest-server.jar
retval=0

if [ "$1" = "clean" ]; then
    echo "Cleaning"
    rm -rf BUILD/
    rm -rf BUILDROOT/
    rm -rf RPMS/
    rm -rf SRPMS/
    rm -rf SOURCES/
    rm -rf tmp/
    exit 0
fi

if [ ! -d $sources ]; then
    mkdir $sources
fi

cd ../

# Get the version of the build and trim off the -SNAPSHOT
echo "Getting version from maven..."
full_version=`mvn -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec`
version=`echo $full_version | sed 's/-.*//'`
release_type=`echo $full_version | sed 's/.*-//'`

if [ $? -ne 0 ]; then
    echo "Error getting version from maven exec plugin"
    exit
fi

jarfile=target/ingest-rest-$version-$release_type.jar

if [ ! -e $jarfile ]; then
    echo "Building latest jar..."
    mvn -q clean install # > /dev/null
    if [ $? -ne 0 ]; then
        echo "Error building replication-shell"
        exit
    fi
else
    echo "Jar already built"
fi


# Copy the artifacts
cp $jarfile rpm/$finaljar
cp target/classes/application.properties rpm/$sources
cp src/main/sh/ingest-server.sh rpm/$sources

# cd back to where we started and build the rpm
cd $rpmdir
rpmbuild -ba --define="_topdir $PWD" --define="_tmppath $PWD/tmp" --define="ver $version" --define="rel $BUILD_NUMBER" SPECS/ingest-server.spec
