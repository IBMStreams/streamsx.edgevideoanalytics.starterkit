/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device;

import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;

import com.ibm.streamsx.edgevideo.device.wipRecognition.FaceRecognizer.Prediction;

/**
 * A class for holding face detection processing info.
 */
public class FacesData {
  public FacesData(List<Mat> faces) {
    this.faces = faces;
  }
  public long timestamp = System.currentTimeMillis();
  public Mat rgbFrame;         // frame to detect faces in
  public MatOfRect faceRects;  // rectangles in rgbFrame of detected faces
  public List<Mat> faces;      // individual cropped faces
  public List<Prediction> predictions; // WIP info parallel to faces
}