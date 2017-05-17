#!/bin/bash
#
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
#

# Build he EdgeVideoAnalytics Smart Camera IoT device code
#
# ./build.sh

if [ "$EDGENT_HOME" = "" ]; then
  echo "EDGENT_HOME is not set. e.g. /edgent-1.1.0/java8"
  exit 1
fi

if [ "$OPENCV_HOME" = "" ]; then
  echo "OPENCV_HOME is not set. e.g. /usr/local/opt/opencv3/share/OpenCV  where \$OPENCV_HOME/java/opencv-320.jar can be found"
  exit 1
fi
# Where the opencv dyn lib resides
JAVA_LIB_PATH_VMOPT=-Djava.library.path=${OPENCV_HOME}/java

export CLASSPATH=`./classpath.sh`

BIN=../bin
mkdir -p $BIN

javac -source 1.8 -d ${BIN}  `find ../device ../clients -name '*.java'`

