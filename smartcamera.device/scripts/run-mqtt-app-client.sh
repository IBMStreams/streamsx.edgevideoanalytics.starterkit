#!/bin/bash
#
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
#

# Runs the MqttFaceDetectAppClient app.
#
# ./run-mqtt-app-client.sh mqtt-device.cfg
#

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

VM_OPTS="${JAVA_LIB_PATH_VMOPT}"

java ${VM_OPTS} com.ibm.streamsx.edgevideo.device.clients.mqtt.MqttFaceDetectAppClient $* 
