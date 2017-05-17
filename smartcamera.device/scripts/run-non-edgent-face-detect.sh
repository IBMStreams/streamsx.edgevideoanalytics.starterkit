#!/bin/bash
#
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
#

# Runs NonEdgentFaceDetectApp.
#
# ./run-non-edgent-face-detect.sh [--fps=N | --pollMsec=N] [--resize=N]
#
# Note, the config file also contains some additional information for this application.
# A sample wiotp-device.cfg is in the scripts/connectors/iotp directory.

if [ "$OPENCV_HOME" = "" ]; then
  echo "OPENCV_HOME is not set. e.g. /usr/local/opt/opencv3/share/OpenCV  where \$OPENCV_HOME/java/opencv-320.jar can be found"
  exit 1
fi
# Where the opencv dyn lib resides
JAVA_LIB_PATH_VMOPT=-Djava.library.path=${OPENCV_HOME}/java

export CLASSPATH=`./classpath.sh`:../bin
#echo CLASSPATH=$CLASSPATH

VM_OPTS="${JAVA_LIB_PATH_VMOPT}"

java ${VM_OPTS} com.ibm.streamsx.edgevideo.device.NonEdgentFaceDetectApp "$@" 
