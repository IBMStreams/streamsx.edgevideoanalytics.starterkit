/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.clients.mqtt;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.iot.Commands;
import org.apache.edgent.topology.TStream;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.JsonObject;
import com.ibm.streamsx.edgevideo.device.clients.wiotp.EdgentControlCmds;
import com.ibm.streamsx.edgevideo.device.edgent.EdgentFaceDetectIotProviderApp;

/**
 * An MQTT ApplicationClient that 
 * publishes device comands for IotProvider based EdgentFaceDetect device applications.
 * 
 * <p>This connects to your MQTT server
 * as the Application defined in a application config file.
 * 
 * <p>Note, the config file also contains some additional information for this application.
 *
 * <p>See {@code scripts/README} for information about a
 * prototype application configuration file and running the application.
 */
public class MqttFaceDetectCmdAppClient {
  
  private static final String usage = "[--deviceId=<id>] {stopApp | startApp | setFps=fps | setPollMsec=msec | demoControlEcho=str | demoCmdEcho=str} <mqtt-app-cfg-file-path> # see scripts/mqtt-device.cfg";

  private static MqttClient client;
  private static String deviceId;
  private static String faceDetectAppName = "face-detect";
  private static String faceDetectSensorPollStreamAlias = "sensorPollStream";
  
  private static String EDGENT_CONTROL_CMD_ID = Commands.CONTROL_SERVICE; 

  private static String cmdTopicPattern;
  
  public static void main(String[] args) throws Exception {
    if (args.length == 0)
      throw new Exception("Usage: " + usage);
    List<String> argList = Arrays.asList(args);
    boolean stopApp = argList.contains("stopApp");
    boolean startApp = argList.contains("startApp");
    Integer setPollMsec = null;
    for (String s : argList) {
      if (s.startsWith("setFps")) {
        int fps = Integer.valueOf(s.split("=")[1]);
        setPollMsec = 1000/fps;
        break;
      }
    }
    for (String s : argList) {
      if (s.startsWith("setPollMsec")) {
        setPollMsec = Integer.valueOf(s.split("=")[1]);
        break;
      }
    }
    String demoControlEchoStr = null;
    for (String s : argList) {
      if (s.startsWith("demoControlEcho")) {
        demoControlEchoStr = s.split("=")[1];
        break;
      }
    }
    String demoCmdEchoStr = null;
    for (String s : argList) {
      if (s.startsWith("demoCmdEcho")) {
        demoCmdEchoStr = s.split("=")[1];
        break;
      }
    }
    String deviceIdArg = null;
    for (String arg : argList) {
      if (arg.startsWith("--deviceId")) {
        deviceIdArg = arg.split("=")[1];
      }
    }
    String appCfgPath = argList.get(argList.size() - 1);

    Properties cfgProps = new Properties();
    cfgProps.load(new FileReader(new File(appCfgPath)));
    
    deviceId = cfgProps.getProperty("mqttDevice.id");
    if (deviceIdArg != null) {
      deviceId = deviceIdArg;
    }
    
    String url = cfgProps.getProperty("mqtt.serverURLs");
    MqttClientPersistence persistence = new MemoryPersistence();
    String clientId = cfgProps.getProperty("mqttDevice.id") + "-app-cmd";  // not the device id
    
    System.out.println("MQTT URL: " + url);
    System.out.println("clientId: " + clientId);
    System.out.println("deviceId: " + deviceId);
    
    client = new MqttClient(url, clientId, persistence);
    
    client.connect();
    
    cmdTopicPattern = MqttFaceDetectAppClient.getCommandTopicPattern(cfgProps, deviceId, "{COMMAND}");

    if (stopApp) {
      stopApp(faceDetectAppName);
    }
    else if (startApp) {
      startApp(faceDetectAppName);
    }
    else if (setPollMsec != null) {
      setStreamPollMsec(faceDetectSensorPollStreamAlias, setPollMsec);
    }
    else if (demoControlEchoStr != null) {
      demoControlEchoStr(demoControlEchoStr);
    }
    else if (demoCmdEchoStr != null) {
      demoCmdEchoStr(demoCmdEchoStr);
    }
    else {
      System.err.println("No command specified.");
      throw new Exception("Usage: " + usage);
      
    }
    
    client.disconnect();
  }
  
  private static void stopApp(String appName) {
    JsonObject cmd = EdgentControlCmds.mkStopAppCmd(appName);
    publishCmd(EDGENT_CONTROL_CMD_ID, cmd);
  }
  
  private static void startApp(String appName) {
    JsonObject cmd = EdgentControlCmds.mkStartAppCmd(appName);
    publishCmd(EDGENT_CONTROL_CMD_ID, cmd);
  }
  
  private static void setStreamPollMsec(String streamAlias, Integer msec) {
    JsonObject cmd = EdgentControlCmds.mkPeriodMXBeanSetPeriodCmd(TStream.TYPE, streamAlias,
        msec, TimeUnit.MILLISECONDS);
    publishCmd(EDGENT_CONTROL_CMD_ID, cmd);
  }
  
  private static void demoControlEchoStr(String str) {
    JsonObject cmd = EdgentControlCmds.mkControlCmd(
        EdgentFaceDetectIotProviderApp.MyDemoControl.CONTROL_TYPE, "demoControl-1", "echo", str);
    publishCmd(EDGENT_CONTROL_CMD_ID, cmd);
  }
  
  private static void demoCmdEchoStr(String str) {
    JsonObject cmd = new JsonObject();
    cmd.addProperty("echoString", str);
    publishCmd("demoEchoCmdId", cmd);
  }
  
  private static void publishCmd(String commandId, JsonObject data) {
    System.out.println(String.format("Publish cmd: deviceId=%s commandId=%s data=%s",
        deviceId, commandId, data));
    
    String topic = cmdTopicPattern.replace("{COMMAND}", commandId);
    System.out.println("topic: " + topic);
    byte[] payload = data.toString().getBytes(StandardCharsets.UTF_8);
    
    try {
      client.publish(topic, payload, 0/*qos*/, false/*retained*/);
    } catch (Exception e) {
      throw new RuntimeException("PublishCommand failed", e);
    }
  }

}
