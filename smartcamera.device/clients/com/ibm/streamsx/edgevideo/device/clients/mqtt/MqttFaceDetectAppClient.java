/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.clients.mqtt;

import static com.ibm.streamsx.edgevideo.device.AbstractFaceDetectApp.DETECTED_FACES_PANEL_HEIGHT;
import static com.ibm.streamsx.edgevideo.device.AbstractFaceDetectApp.DETECTED_FACES_PANEL_WIDTH;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.edgent.connectors.mqtt.iot.MqttDevice;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.json.JsonFunctions;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import com.google.gson.JsonObject;
import com.ibm.streamsx.edgevideo.device.MyPanel;
import com.ibm.streamsx.edgevideo.device.edgent.JsonFaceEvent;

/**
 * An MQTT ApplicationClient that 
 * subscribes to device events for the EdgentFaceDetect device applications.
 * 
 * <p>The client application receives face-detect events and renders the
 * detected faces.
 * 
 * <p>This connects to your MQTT server
 * as the Application defined in a application config file.
 * 
 * <p>Note, the config file also contains some additional information for this application.
 *
 * <p>See {@code scripts/README} for information about a
 * prototype application configuration file and running the application.
 */
public class MqttFaceDetectAppClient {
  
  private static final String usage = "[--deviceId=<id>] <mqtt-app-cfg-file-path> # see scripts/mqtt-device.cfg";
  
  protected static MyPanel detectedFacesPanel;  // panel for rendering the detected faces

  public static void main(String[] args) throws Exception {
    if (args.length == 0)
      throw new Exception("Usage: " + usage);
    List<String> argList = Arrays.asList(args);
    String deviceIdArg = null;
    for (String arg : argList) {
      if (arg.startsWith("--deviceId")) {
        deviceIdArg = arg.split("=")[1];
      }
    }
    String appCfgPath = argList.get(argList.size() - 1);

    Properties cfgProps = new Properties();
    cfgProps.load(new FileReader(new File(appCfgPath)));
    
    String deviceId = null;  // all devices
    if (deviceIdArg != null) {
      deviceId = deviceIdArg;
    }

    detectedFacesPanel = MyPanel.newFramedPanel("MQTT DetectedFaces",
        50, 100, DETECTED_FACES_PANEL_WIDTH, DETECTED_FACES_PANEL_HEIGHT);
    
    // Init OpenCV - Load the native library.
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    System.out.println("Loaded OpenCV native lib: " + Core.NATIVE_LIBRARY_NAME.toString());
    
    String url = cfgProps.getProperty("mqtt.serverURLs");
    MqttClientPersistence persistence = new MemoryPersistence();
    String clientId = cfgProps.getProperty("mqttDevice.id") + "-app";  // not the device id
    
    System.out.println("MQTT URL: " + url);
    System.out.println("clientId: " + clientId);
    System.out.println("deviceId: " + deviceId);
    
    MqttClient client = new MqttClient(url, clientId, persistence);
    
    client.connect();
    
    String evtTopicPattern = getEventTopicPattern(cfgProps, deviceId, null);
    System.out.println("evtTopicPattern: " + evtTopicPattern);
    
    boolean subscribeToEvents = true;
    if (subscribeToEvents) {
      System.out.println("Subscribing to device events...");
      client.subscribe(evtTopicPattern);
      client.setCallback(new MqttCallback() {

        @Override
        public void connectionLost(Throwable arg0) {
          // TODO Auto-generated method stub
          
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {
          // TODO Auto-generated method stub
          
        }

        @Override
        public void messageArrived(String topic, MqttMessage msg) throws Exception {
          if (topic.contains("/faces/")) {
            handleFaceDetectEvent(msg);
          }
          else {
            System.out.println(
                String.format("Received unhandled event: %s", topic));
          }
        }
        
      });

      Thread.sleep(Integer.MAX_VALUE);
    }
    
    client.disconnect();
  }
  
  /** the returned string contains wildcard for "{DEVICEID}" and "{EVENTID}" unless deviceId or evtId are non-null */
  static String getEventTopicPattern(Properties cfgProps, String deviceId, String evtId) {
    // Use Edgent MqttDevice to learn the device's Event and Command topic patterns (from its cfg).
    DirectProvider dp = new DirectProvider();
    Topology top = dp.newTopology();
    MqttDevice mqttDevice = new MqttDevice(top, cfgProps);
    String evtTopicPattern = mqttDevice.eventTopic(evtId == null ? "+" : evtId);
    evtTopicPattern = evtTopicPattern.replace("/"+mqttDevice.getDeviceId()+"/", deviceId == null ? "/+/" : "/"+deviceId+"/");
    return evtTopicPattern;
  }
  
  /** the returned string contains wildcard for "{DEVICEID}" and "{COMMAND}" unless deviceId or command are non-null */
  static String getCommandTopicPattern(Properties cfgProps, String deviceId, String command) {
    // Use Edgent MqttDevice to learn the device's Event and Command topic patterns (from its cfg).
    DirectProvider dp = new DirectProvider();
    Topology top = dp.newTopology();
    MqttDevice mqttDevice = new MqttDevice(top, cfgProps);
    String cmdTopicPattern = mqttDevice.commandTopic(command == null ? "+" : command);
    cmdTopicPattern = cmdTopicPattern.replace("/"+mqttDevice.getDeviceId()+"/", deviceId == null ? "/+/" : "/"+deviceId+"/");
    return cmdTopicPattern;
  }

  private static void handleFaceDetectEvent(MqttMessage msg) {
    JsonObject faceEvent = JsonFunctions.fromBytes().apply(msg.getPayload());
    
    //System.out.println("Received face detection event: timestamp=" + JsonFaceEvent.getTimestamp(faceEvent));
    
    renderImage(JsonFaceEvent.getFace(faceEvent));
  }
  
  private static void renderImage(Mat face) {
    // render the detected face
    detectedFacesPanel.clear();
    detectedFacesPanel.matToBufferedImage(face);
    detectedFacesPanel.repaint();
  }

}
