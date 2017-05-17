/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.edgent;

import java.util.ArrayList;
import java.util.List;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.opencv.core.Mat;

import com.google.gson.JsonObject;
import com.ibm.streamsx.edgevideo.device.FacesData;

/**
 * Version of EdgentFaceDetectApp instrumented for stats
 */
public class EdgentFaceDetectAppStats extends EdgentFaceDetectApp {
  
  public static void main (String args[]) throws Exception {
		new EdgentFaceDetectAppStats().run(args);
	}
  
	/**
	 * Build a topology that performs face detection in a single map() transformation.
	 * 
	 * <p>Override to use alternate approaches.
	 * 
	 * @param iotDevice IoT hub connector
	 * @param config
	 */
	protected void buildTopology(IotDevice iotDevice, JsonObject config) {

    // polled rawRgbFrame -> filter empty frames -> detect faces > display -> publish
	  
	  Topology top = iotDevice.topology();
    
	  // create a stream with one FacesData for each processed frame
    @SuppressWarnings("unused")
    TStream<FacesData> frameData = 
      top.poll(() -> { stats.getFrame.markStart(); 
                       Mat frame = camera.grabFrame();
                       stats.getFrame.markEnd();
                       return frame;
               },
               sensorPollValue, sensorPollUnit)
           //.peek(t -> stats.getFrame.markEnd())
      
         .filter(rawRgbFrame -> !rawRgbFrame.empty())
           
           .peek(t -> stats.imgProcess.markStart())
         .map(rawRgbFrame -> faceDetector.detectFaces(rawRgbFrame))
           .peek(t -> stats.imgProcess.markEnd())
           
           //.peek(t -> stats.render.markStart())
         .peek(facesData -> renderImages(facesData.rgbFrame, facesData.faceRects, facesData.faces))
           //.peek(t -> stats.render.markEnd())
         
           .peek(t -> stats.reportFrameProcessed())
           ;
    
    //publish(frameData, iotDevice);
  }

  protected void publishEvents(TStream<FacesData> frameData, IotDevice iotDevice) {
    
    frameData = frameData.peek(t -> stats.publish.markStart());
    
    // create a stream with one JsonObject for each detected face    
    TStream<JsonObject> faceEvents = 
        frameData.flatMap(data -> {
                  List<JsonObject> faces = new ArrayList<>();
                  for (Mat face : data.faces) {
                    faces.add(JsonFaceEvent.toJsonObject(data.timestamp, face));
                  }
                  return faces;
                });
    
    iotDevice.events(faceEvents, "faces", publishQoS)
      .getFeed()
        .peek(t -> stats.publish.markEnd()) // is this called after tuple is handled by events?
      ;
    
    //faceEvents.sink(FaceEventRecorder.newConsumer());
  }
	
}
