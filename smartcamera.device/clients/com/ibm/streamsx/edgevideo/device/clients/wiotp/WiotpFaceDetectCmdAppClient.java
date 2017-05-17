/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.clients.wiotp;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.iot.Commands;
import org.apache.edgent.topology.TStream;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.app.ApplicationClient;
import com.ibm.streamsx.edgevideo.device.edgent.EdgentFaceDetectIotProviderApp;

/**
 * An IBM Watson IoT Platform ApplicationClient that 
 * publishes device comands for IotProvider based EdgentFaceDetect device applications.
 * 
 * <p>This connects to your IBM Watson IoT Platform service
 * as the Application defined in a application config file.
 * The file format is the standard one for IBM Watson IoT Platform.
 * 
 * <p>Note, the config file also contains some additional information for this application.
 *
 * <p>See {@code scripts/README} for information about a
 * prototype application configuration file and running the application.
 */
public class WiotpFaceDetectCmdAppClient {
  
  private static final String usage = "[--deviceType=<type>] [--deviceId=<id>] {stopApp | startApp | setFps=fps | setPollMsec=msec | demoControlEcho=str | demoCmdEcho=str} <app-cfg-file-path> # see scripts/wiotp-app-client.cfg";

  private static ApplicationClient client;
  private static String iotpDevType;
  private static String iotpDevId;
  private static String faceDetectAppName = "face-detect";
  private static String faceDetectSensorPollStreamAlias = "sensorPollStream";
  
  private static String EDGENT_CONTROL_CMD_ID = Commands.CONTROL_SERVICE; 

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
    String deviceTypeArg = null;
    for (String arg : argList) {
      if (arg.startsWith("--deviceType")) {
        deviceTypeArg = arg.split("=")[1];
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
    
    String iotpAppId = getProperty(cfgProps, "id");
    iotpAppId += "-cmd";  // unique vs the id used by the event handling client
    cfgProps.setProperty("id", iotpAppId);

    iotpDevType = cfgProps.getProperty("deviceType");
    iotpDevId = cfgProps.getProperty("deviceId");
    if (deviceTypeArg != null) {
      iotpDevType = deviceTypeArg;
    }
    if (deviceIdArg != null) {
      iotpDevId = deviceIdArg;
    }
    System.out.println("deviceType: " + iotpDevType);
    System.out.println("deviceId:   " + iotpDevId);
    if (iotpDevType==null || iotpDevId==null)
      throw new Exception("deviceType or deviceId not specified");

    client = new ApplicationClient(cfgProps);
    
    client.connect();

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
  
  private static void publishCmd(String commandId, Object data) {
    System.out.println(String.format("Publish cmd: deviceType=%s deviceId=%s commandId=%s data=%s",
        iotpDevType, iotpDevId, commandId, data));
    
    boolean ok = client.publishCommand(iotpDevType, iotpDevId, commandId, data);
    if (!ok) {
      // right now publishCommand prints a stacktrace when ok==false;
      throw new RuntimeException("PublishCommand failed");
    }
  }
  
  private static String getProperty(Properties props, String... keys) {
    for (String key : keys) {
      String val = props.getProperty(key);
      if (val != null)
        return val;
    }
    return null;
  }

}
