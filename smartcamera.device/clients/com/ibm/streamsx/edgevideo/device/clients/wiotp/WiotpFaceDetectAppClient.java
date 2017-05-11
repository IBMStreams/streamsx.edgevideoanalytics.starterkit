/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.clients.wiotp;

import static com.ibm.streamsx.edgevideo.device.AbstractFaceDetectApp.DETECTED_FACES_PANEL_HEIGHT;
import static com.ibm.streamsx.edgevideo.device.AbstractFaceDetectApp.DETECTED_FACES_PANEL_WIDTH;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.edgent.topology.json.JsonFunctions;
import org.opencv.core.Core;
import org.opencv.core.Mat;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.app.ApplicationClient;
import com.ibm.iotf.client.app.Command;
import com.ibm.iotf.client.app.Event;
import com.ibm.iotf.client.app.EventCallback;
import com.ibm.streamsx.edgevideo.device.MyPanel;
import com.ibm.streamsx.edgevideo.device.edgent.JsonFaceEvent;

/**
 * An IBM Watson IoT Platform ApplicationClient that 
 * subscribes to device events for the EdgentFaceDetect device applications.
 * 
 * <p>The client application receives face-detect events and renders the
 * detected faces.
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
public class WiotpFaceDetectAppClient {
  
  private static final String usage = "[--deviceType=<type>] [--deviceId=<id>] <app-cfg-file-path> # see scripts/wiotp-app-client.cfg";
  
  protected static MyPanel detectedFacesPanel;  // panel for rendering the detected faces

  public static void main(String[] args) throws Exception {
    if (args.length == 0)
      throw new Exception("Usage: " + usage);
    List<String> argList = Arrays.asList(args);
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
    
    String iotpOrg = getProperty(cfgProps, "Organization-ID", "org");
    String iotpAppId = getProperty(cfgProps, "id");
    String iotpApiKey = getProperty(cfgProps, "API-Key", "auth-key");
    System.out.println("org:     " + iotpOrg);
    System.out.println("id:      " + iotpAppId);
    System.out.println("ApiKey:  " + iotpApiKey);

    String iotpDevType = cfgProps.getProperty("deviceType");
    String iotpDevId = cfgProps.getProperty("deviceId");
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

    detectedFacesPanel = MyPanel.newFramedPanel("WIoTP DetectedFaces",
        50, 100, DETECTED_FACES_PANEL_WIDTH, DETECTED_FACES_PANEL_HEIGHT);
    
    // Init OpenCV - Load the native library.
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    System.out.println("Loaded OpenCV native lib: " + Core.NATIVE_LIBRARY_NAME.toString());

    ApplicationClient client = new ApplicationClient(cfgProps);
    
    client.connect();
    
    boolean subscribeToEvents = true;
    if (subscribeToEvents) {
      System.out.println("Subscribing to device events...");
      client.subscribeToDeviceEvents();
      client.setEventCallback(new EventCallback() {

        @Override
        public void processCommand(Command cmd) {
          // TODO Auto-generated method stub
          
        }

        @Override
        public void processEvent(Event event) {
          if (event.getEvent().equals("faces")) {
            handleFaceDetectEvent(event);
          }
          else {
            System.out.println(
                String.format("Received unhandled event: %s %s:%s %s", event.getEvent(),
                    event.getDeviceType(), event.getDeviceId(),
                    event.getFormat()));
          }
        }
        
      });
      Thread.sleep(Integer.MAX_VALUE);
    }
    
    client.disconnect();
  }
  
  private static void handleFaceDetectEvent(Event event) {
    @SuppressWarnings("deprecation")
    JsonObject faceEvent = JsonFunctions.fromString().apply(event.getPayload());
    
    //System.out.println("Received face detection event: timestamp=" + JsonFaceEvent.getTimestamp(faceEvent));
    
    renderImage(JsonFaceEvent.getFace(faceEvent));
  }
  
  private static void renderImage(Mat face) {
    // render the detected face
    detectedFacesPanel.clear();
    detectedFacesPanel.matToBufferedImage(face);
    detectedFacesPanel.repaint();
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
