#!/bin/bash
#
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
#

# Runs EdgentFaceDetectIotProviderApp.
#
# ./run-edgent-face-detect-iotp-provider.sh [-h] [--fps=N | --pollMsec=N] [--resize=N] [--noautostart] {--quickstart | iot-device-cfg-file}  # see scripts/wiotp-device.cfg
#
# Note, the config file also contains some additional information for this application.
# A sample wiotp-device.cfg is in the scripts/connectors/iotp directory.

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

export CLASSPATH=`./classpath.sh`:../bin
#echo CLASSPATH=$CLASSPATH

# https://github.com/ibm-watson-iot/iot-java/tree/master#migration-from-release-015-to-021
# Uncomment the following to use the pre-0.2.1 WIoTP client behavior.
#
#USE_OLD_EVENT_FORMAT=-Dcom.ibm.iotf.enableCustomFormat=false

VM_OPTS="${USE_OLD_EVENT_FORMAT} ${JAVA_LIB_PATH_VMOPT}"

java ${VM_OPTS} com.ibm.streamsx.edgevideo.device.edgent.EdgentFaceDetectIotProviderApp "$@" 
