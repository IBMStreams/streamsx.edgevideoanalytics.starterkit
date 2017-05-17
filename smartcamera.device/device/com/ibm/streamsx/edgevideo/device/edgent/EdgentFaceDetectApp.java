/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.edgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.connectors.iot.QoS;
import org.apache.edgent.connectors.iotp.IotpDevice;
import org.apache.edgent.connectors.mqtt.iot.MqttDevice;
import org.apache.edgent.execution.mbeans.PeriodMXBean;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.opencv.core.Mat;

import com.google.gson.JsonObject;
import com.ibm.streamsx.edgevideo.device.AbstractFaceDetectApp;
import com.ibm.streamsx.edgevideo.device.FacesData;

/**
 * An Edgent version of FaceDetectDemo that uses a DirectProvider and IotpDevice.
 * 
 * <p>This extends FaceDetectDemo to leverage common non-Edgent
 * code and highlights the Edgent-specfic code.
 */
public class EdgentFaceDetectApp extends AbstractFaceDetectApp {
  
  protected static String usage = "[-h] [--fps=N | --pollMsec=N] [--resize=N] {--quickstart | [--mqtt] iot-device-cfg-file}  # see scripts/wiotp-device.cfg";
  protected static String quickstartDeviceId = "EdgentFaceDetectDemo-device1";

  protected boolean useMqttDevice;
  protected boolean useIotpQuickstart;
  protected String iotDeviceCfgPath;
  
  protected int publishQoS = QoS.FIRE_AND_FORGET;

  protected WorkaroundEdgent400 edgent400Workaround = new WorkaroundEdgent400();
  
  public static void main (String args[]) throws Exception {
		new EdgentFaceDetectApp().run(args);
	}
	
	@Override
	protected void runFaceDetection() throws Exception {
	  buildAndStartTopologies();
	}
	
	/**
	 * Build and start the Edgent processing topologies using
	 * a DirectProvider and an IoT hub connector.
	 * 
	 * <p>Override to use a different approach - e.g., IotProvider
	 * @throws Exception
	 */
	protected void buildAndStartTopologies() throws Exception {
	  
    // The standard Edgent IotDevice pattern of: 
    //    create provider, create topology, create IoT Connector, build topology, submit it

    DirectProvider provider = new DirectProvider();
    Topology top = provider.newTopology();

    IotDevice iotDevice = newIotHubConnector(top);
    
    buildTopology(iotDevice, null);
    
    provider.submit(top);   
	}
  
	/**
	 * Build a topology that performs face detection in a single map() transformation.
	 * 
	 * @param iotDevice IoT hub connector
	 * @param config
	 */
	protected void buildTopology(IotDevice iotDevice, JsonObject config) {
    
    // polled rawRgbFrame -> filter empty frames -> detect faces -> display -> publish
    
    Topology top = iotDevice.topology();
    
    // create a stream with one FacesData for each processed frame
    TStream<FacesData> frameData = 
        
      top.poll(() -> camera.grabFrame(), sensorPollValue, sensorPollUnit)
      
         .alias("sensorPollStream")  // ### enable poll control via PeriodMXBean
         
         .filter(rawRgbFrame -> !rawRgbFrame.empty())
         
         .map(rawRgbFrame -> faceDetector.detectFaces(rawRgbFrame))
         
         .peek(facesData -> renderImages(facesData));
    
    publishEvents(frameData, iotDevice);

    edgent400Workaround.addControlId(top, TStream.TYPE, "sensorPollStream", PeriodMXBean.class);
  }


  protected void publishEvents(TStream<FacesData> frameData, IotDevice iotDevice) {
    
    // create a stream with one JsonObject for each detected face
    
    TStream<JsonObject> faceEvents = frameData.flatMap(facesData -> {
          List<JsonObject> results = new ArrayList<>();
          for (Mat face : facesData.faces) {
            results.add(JsonFaceEvent.toJsonObject(facesData.timestamp, face));
          }
          return results;
        });
    
    iotDevice.events(faceEvents, "faces", publishQoS);
    
    //faceEvents.sink(FaceEventRecorder.newConsumer());
  }
  
  protected IotDevice newIotHubConnector(Topology top) {
    if (useMqttDevice) {
      return newMqttDevice(top, iotDeviceCfgPath);
    }
    else if (useIotpQuickstart) {
      return newQuickstartIotDevice(top);
    }
    else {
      return new IotpDevice(top, new File(iotDeviceCfgPath));
    }
  }
  
  protected IotDevice newQuickstartIotDevice(Topology top) {
    IotDevice iotDevice = IotpDevice.quickstart(top, quickstartDeviceId);
    System.out.println(
        "#############################################\n"
      + "Open your browser on https://quickstart.internetofthings.ibmcloud.com/#/device/"
          + quickstartDeviceId + "\n"
      + "#############################################");
    return iotDevice;
  }

  protected IotDevice newMqttDevice(Topology top, String iotDeviceCfgPath) {
    try {
      Properties props = new Properties();
      props.load(new FileInputStream(new File(iotDeviceCfgPath)));
      
      return new MqttDevice(top, props);
      
    } catch (IOException e) {
      throw new RuntimeException("Unable to initialize load MqttDevice config", e);
    }
  }
  
  protected void processArgs(String[] args) throws Exception {
    List<String> argList = Arrays.asList(args);
    if (argList.size() == 0 || argList.contains("-h") || argList.contains("--help"))
      throw new Exception("Usage: " + usage);

    super.processArgs(args);
    
    useMqttDevice = argList.contains("--mqtt");
    useIotpQuickstart = argList.contains("--quickstart");
    iotDeviceCfgPath = argList.get(argList.size() - 1);
    
    if (useIotpQuickstart) {
      sensorPollValue = 1;
      sensorPollUnit = TimeUnit.SECONDS;
    }
    
    if (!useIotpQuickstart && !new File(iotDeviceCfgPath).exists()) {
      throw new Exception("No such iotDeviceCfgPath file: " + iotDeviceCfgPath);
    }
  }
	
}
