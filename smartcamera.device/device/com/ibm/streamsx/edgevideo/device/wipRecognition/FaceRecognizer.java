/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.wipRecognition;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opencv.core.Mat;

/**
 * A simple OpenCV based face recognizer.
 * 
 * <p>The recognizer is trained with a number of subject images.  
 * Once trained it can be called to score an image
 * with respect to the subjects it's been trained on.
 */
public class FaceRecognizer {
  // BasicFaceRecognizer model;   TODO face recog is part of opencv "contrib"
  
  public FaceRecognizer() {
    // create model
    @SuppressWarnings("unused")
    int numPCs = 14;  // number of principal components to keep
//    model = FaceRecog.createEigenFaceRecognizer(numPCs);
  }
  
  public void loadTrainedModel(File modelPath) {
    // TODO
  }
  
  public void loadAndTrain(File csvPath) {
    List<Mat> faces = new ArrayList<>();
    List<Integer> subjectIds = new ArrayList<>();
    Map<Integer,String> subjectIdLabels = new HashMap<>();
    
    readCsv(csvPath, faces, subjectIds, subjectIdLabels);
    train(faces, subjectIds, subjectIdLabels);
  }
  
  public void train(List<Mat> faces, List<Integer> subjectIds, Map<Integer,String> subjectIdLabels) {
//    model.train(faces, subjectIds)
    for (@SuppressWarnings("unused") int subjectId : subjectIds) {
//      model.setLabelInfo(subjectId, subjectIdLabels.get(subjectId));
    }
  }
  
  public static class Prediction {
    public Prediction(int subjectId, double confidence, String subjectIdLabel) {
      this.subjectId = subjectId;
      this.confidence = confidence;
      this.subjectIdLabel = subjectIdLabel;
    }
    public final int subjectId;
    public final double confidence;
    public final String subjectIdLabel;
  }
  
  public Prediction predict(Mat face) {
    int[] subjectId = {-1};
    double[] confidence = {0.0};
//    model.predict(face, subjectId, confidence);
    String subjectLabel = "";
    if (subjectId[0] != -1) {
//      subjectLabel = model.getLabelInfo(subjectId);
    }
    return new Prediction(subjectId[0], confidence[0], subjectLabel);
  }
  
  public void readCsv(File csvPath, List<Mat> faces, List<Integer> subjectIds, Map<Integer,String> subjectIdLabels) {
    // csv:   <imagePath, subjectId, subjectIdLabel>
    
    // TODO 
  }

}
