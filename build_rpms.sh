#!/bin/bash

rpmdir=$PWD/rpms
sources=SOURCES
ingest_yaml=ingest-application.yml
ingest_finaljar=${sources}/ingest-server.jar
replication_yaml=replication-application.yml
replication_finaljar=${sources}/replication-shell.jar
retval=0

if [ "$1" = "clean" ]; then
    echo "Cleaning"
    rm -rf ${rpmdir}/BUILD/
    rm -rf ${rpmdir}/BUILDROOT/
    rm -rf ${rpmdir}/RPMS/
    rm -rf ${rpmdir}/SRPMS/
    rm -rf ${rpmdir}/SOURCES/
    rm -rf ${rpmdir}/tmp/
    exit 0
fi

if [ ! -d ${sources} ]; then
    mkdir ${sources}
fi

# Get the version of the build and trim off the -SNAPSHOT
echo "Getting version from maven..."
full_version=`./mvnw -q -Dexec.executable="echo" -Dexec.args='${project.version}' --non-recursive exec:exec`
if [ $? -ne 0 ]; then
    echo "Error getting version from maven exec plugin"
    exit -1
fi

version=`echo ${full_version} | sed 's/-.*//'`
release_type=`echo ${full_version} | sed 's/.*-//'`

ingest_jarfile=ingest-rest/target/ingest-rest-${version}-${release_type}.jar
replication_jarfile=replication-shell/target/replication-shell-${version}-${release_type}.jar

./mvnw -q -Dmaven.test.redirectTestOutputToFile=true -Dspring.profiles.active=gitlab -pl ingest-rest clean package # > /dev/null
if [ $? -ne 0 ]; then
  echo "Error building ingest-server"
  exit 99
fi

./mvnw -q -Dmaven.test.redirectTestOutputToFile=true -Dspring.profiles.active=gitlab -pl replication-shell clean package # > /dev/null
if [ $? -ne 0 ]; then
  echo "Error building replication-shell"
  exit 99
fi

# Copy ingest artifacts
mkdir ${rpmdir}/SOURCES
cp ${ingest_jarfile} ${rpmdir}/${ingest_finaljar}
cp ingest-rest/src/main/sh/ingest-server.sh ${rpmdir}/${sources}
cp ingest-rest/src/main/sh/ingest-prepare.sh ${rpmdir}/${sources}
cp ingest-rest/src/main/sh/ingest-server.service ${rpmdir}/${sources}
cp ingest-rest/src/main/resources/application.yml ${rpmdir}/${sources}/${ingest_yaml}

# Copy replication artifacts
cp ${replication_jarfile} ${rpmdir}/${replication_finaljar}
cp replication-shell/src/main/sh/replication.sh ${rpmdir}/${sources}
cp replication-shell/src/main/sh/replicationd-prepare.sh ${rpmdir}/${sources}
cp replication-shell/src/main/sh/replicationd.service ${rpmdir}/${sources}
cp replication-shell/src/main/resources/application.yml ${rpmdir}/${sources}/${replication_yaml}

# build the rpms
cd ${rpmdir}
rpmbuild -ba --define="_topdir ${rpmdir}" --define="_tmppath ${rpmdir}/tmp" --define="ver $version" SPECS/ingest-server-el6.spec
rpmbuild -ba --define="_topdir ${rpmdir}" --define="_tmppath ${rpmdir}/tmp" --define="ver $version" SPECS/ingest-server-el7.spec
rpmbuild -ba --define="_topdir ${rpmdir}" --define="_tmppath ${rpmdir}/tmp" --define="ver $version" SPECS/replicationd-el6.spec
rpmbuild -ba --define="_topdir ${rpmdir}" --define="_tmppath ${rpmdir}/tmp" --define="ver $version" SPECS/replicationd-el7.spec
