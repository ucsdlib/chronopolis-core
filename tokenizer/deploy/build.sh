#!/bin/sh

working_dir=$PWD
sources_dir=tokenizer
build_dir=TARS
finaljar=$sources/tokenizer.jar
retval=0

if [ "$1" = "clean" ]; then
    echo "Cleaning"
    rm -rf tokenizer/
    rm -rf TARS/
    exit 0
fi

if [ ! -d $sources_dir ]; then
    mkdir $sources_dir
fi

if [ ! -d $build_dir ]; then
    mkdir $build_dir
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

jarfile=target/standalone/tokenizer-standalone-$version-$release_type.jar

if [ ! -e $jarfile ]; then
    echo "Building latest jar..."
    mvn -q -Dmaven.test.redirectTestOutputToFile=true clean install # > /dev/null
    if [ $? -ne 0 ]; then
        echo "Error building tokenizer"
        exit 99
    fi
else
    echo "Jar already built"
fi

echo "Copying jar build files to $working_dir/$sources_dir"
cp $jarfile $working_dir/$sources_dir/$finaljar
cp target/classes/application.yml $working_dir/$sources_dir/application.yml

echo "Building tar"
cd $working_dir
tar cvf $build_dir/tokenizer-$version-$release_type.tar $sources_dir
