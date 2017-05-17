#!/bin/sh
#
# echo a CLASSPATH value that includes the sample's external dependencies

OPENCV_JAR_NAME=opencv-320.jar
if [ ! -f ${OPENCV_HOME}/java/${OPENCV_JAR_NAME} ]; then
    OPENCV_JAR_NAME=opencv-310.jar
fi

echo ${OPENCV_HOME}/java/${OPENCV_JAR_NAME}\
:${EDGENT_HOME}/lib/edgent.providers.direct.jar\
:${EDGENT_HOME}/lib/edgent.providers.iot.jar\
:${EDGENT_HOME}/connectors/iotp/lib/edgent.connectors.iotp.jar\
:${EDGENT_HOME}/connectors/mqtt/lib/edgent.connectors.mqtt.jar\
:${EDGENT_HOME}/ext/slf4j-jdk14-1.7.12.jar