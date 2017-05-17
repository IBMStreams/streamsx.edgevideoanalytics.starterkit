/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device;

import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;

/**
 * A simple (non-Edgent) face detection demo using OpenCV.  
 * 
 * <p>With appropriate setup, runs on OSX, Raspberry Pi, ...
 * 
 * <p>It opens the camera and grabs and process frames.
 * Each detected face in the frame is "boxed" and the frame is displayed in window.
 * 
 * <p>The OpenCV processing is:
 * <pre>
 * grab a frame -> resize -> toGrayscale -> detect faces -> extract faces -> render
 * </pre>
 * 
 * <p>Type this command before starting the program to ensure
 * that OpenCV will be able to use the Raspberry Pi camera
 * <pre>
 * sudo modprobe bcm2835-v4l2
 * </pre>
 */
public class NonEdgentFaceDetectApp extends AbstractFaceDetectApp {
  long frameCnt;
  long lastReportMillis;
  long startMillis = System.currentTimeMillis();

	public static void main (String args[]) throws Exception {
		new NonEdgentFaceDetectApp().run(args);
	}
	
	/**
	 * Do the continuous face detection processing and render images.
	 * @throws Exception
	 */
	@Override
	protected void runFaceDetection() throws Exception {
	  
    while (true) {
      
      // Grab a frame
      stats.getFrame.markStart();
      Mat rawRgbFrame = camera.grabFrame();
      stats.getFrame.markEnd();
      
      // Process it
      if (!rawRgbFrame.empty()) {
        
        stats.imgProcess.markStart();
        FacesData facesData = faceDetector.detectFaces(rawRgbFrame);
        stats.imgProcess.markEnd();
    
        //System.out.println(now()+" - Detected faces : "+data.faces.size());
        
        // render images
        stats.render.markStart();
        renderImages(facesData);
        stats.render.markEnd();
        
        // Note: lacks publish data to Enterprise IoT hub
      }
      
      stats.reportFrameProcessed();
      
      // Note: lacks ability to dynamically control the poll rate
      // Note the following yields "with fixed delay" vs Topology.poll()'s "at fixed rate" 
      Thread.sleep(TimeUnit.MILLISECONDS.convert(sensorPollValue, sensorPollUnit));
    }
    
  }
	
}
