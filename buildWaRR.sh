#!/bin/bash

echo "Building Chrome"
NR_CORES=`cat /proc/cpuinfo | grep ^processor | wc -l`
echo "Using $NR_CORES cores"
cd src
make -j $NR_CORES BUILDTYPE=Release chrome
cd ..
echo "Building selenium server"
cd selenium
./go selenium-server-standalone
cd ..
echo "Building the WaRR Replayer"
cd WaRRReplayer
ant
cd ..
echo "Done"
