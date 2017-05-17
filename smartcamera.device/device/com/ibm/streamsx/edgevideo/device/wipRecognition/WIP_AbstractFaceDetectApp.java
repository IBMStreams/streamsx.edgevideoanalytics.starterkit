/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.wipRecognition;

import java.io.File;

import com.ibm.streamsx.edgevideo.device.AbstractFaceDetectApp;
import com.ibm.streamsx.edgevideo.device.Camera;
import com.ibm.streamsx.edgevideo.device.FaceDetector;

/**
 * Temp hack to start fleshing out incorporating face recognition
 */
public abstract class WIP_AbstractFaceDetectApp extends AbstractFaceDetectApp {

  protected String recognizerTrainingDataPath = "nyi-recognizerTrainingDataPath";
  protected FaceRecognizer faceRecognizer;
	
	protected void run(String args[]) throws Exception {
	  processArgs(args);

	  initPanels();
        
    camera = new Camera();
    faceDetector = new FaceDetector(new File(faceClassifierPath));
    
    faceRecognizer = new FaceRecognizer();
    faceRecognizer.loadAndTrain(new File(recognizerTrainingDataPath));
    
    runFaceDetection();
	}
	
}
