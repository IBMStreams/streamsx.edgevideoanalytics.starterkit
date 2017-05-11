/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.wipRecognition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import com.ibm.streamsx.edgevideo.device.FacesData;
import com.ibm.streamsx.edgevideo.device.wipRecognition.FaceRecognizer.Prediction;

/**
 * Temp hack to start fleshing out incorporating face recognition
 */
public class WIP_NonEdgentFaceDetectApp extends WIP_AbstractFaceDetectApp {

	public static void main (String args[]) throws Exception {
		new WIP_NonEdgentFaceDetectApp().run(args);
	}
	
	/**
	 * Do the continuous face detection processing and render images.
	 * @throws Exception
	 */
	@Override
	protected void runFaceDetection() throws Exception {
    
    while (true) {
      
      // Grab a frame
      Mat rawRgbFrame = camera.grabFrame();
      
      // Process it
      if (!rawRgbFrame.empty()) {
        
        FacesData data = faceDetector.detectFaces(rawRgbFrame);
    
        //System.out.println(now()+" - Detected faces : "+data.faces.size());
        
        doFaceRecognition(data);
       
        // render images
        renderImages(data.rgbFrame, data.faceRects, data.faces);
        
        // Note: lacks publish data to Enterprise IoT hub
      }
      
      // Note: lacks ability to dynamically control the poll rate
      Thread.sleep(TimeUnit.MILLISECONDS.convert(sensorPollValue, sensorPollUnit));
    }
    
  }
  
  protected void doFaceRecognition(FacesData faces) {
    List<Prediction> predictions = new ArrayList<>();
    for (Mat face : faces.faces) {
      predictions.add(faceRecognizer.predict(face));
    }
    faces.predictions = predictions;
  }
  
  protected void renderImages(Mat rgbFrame, MatOfRect faceRects, List<Mat> faces) {
    renderImages(rgbFrame, faceRects, faces, Collections.emptyList());
  }
  
  protected void renderImages(Mat rgbFrame, MatOfRect faceRects, List<Mat> faces, List<Prediction> predictions) {
    // draw rectangles around the detected faces and render
    Rect[] rectArray = faceRects.toArray();
    for (Rect faceRect : rectArray) {
      Imgproc.rectangle(rgbFrame, new Point(faceRect.x, faceRect.y), new Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height), new Scalar(0, 255, 0));
    }
    
    // TODO add recognition prediction info label to image
    
    faceDetectPanel.matToBufferedImage(rgbFrame);
    faceDetectPanel.repaint();
    
    // render the detected faces
    if (renderDetections) {
      detectedFacesPanel.clear();
      for (Mat face: faces) {
        // TODO handle rendering multiple detections / images in the panel 
        detectedFacesPanel.matToBufferedImage(face);
      }
      detectedFacesPanel.repaint();
    }
  }
	
}
