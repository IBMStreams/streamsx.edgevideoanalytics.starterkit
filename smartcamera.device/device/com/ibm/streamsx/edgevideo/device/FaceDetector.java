/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2016
 */
package com.ibm.streamsx.edgevideo.device;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

/**
 * A simple OpenCV based Face Detector
 */
public class FaceDetector {
  private CascadeClassifier faceClassifier;
//  public static int DEFAULT_RESIZE_FACTOR = 4; // my MBP sees ~7 images/sec with the /2 and 21 with the /4
  public static int DEFAULT_RESIZE_FACTOR = 3; // better with the 8MP Pi Camera V2
  int resizeFactor = DEFAULT_RESIZE_FACTOR;
  
  /**
   * Create a new instance using the specified {@link CascadeClassifier}.
   * @param faceClassifierPath path to the classifier - e.g. haarcascade_frontalface_alt.xml
   * @throws IOException 
   */
  public FaceDetector(File faceClassifierPath) throws IOException {
    CascadeClassifier faceClassifier = new CascadeClassifier();
    boolean loaded = faceClassifier.load(faceClassifierPath.toString());
    if (!loaded) {
        throw new IOException("Error loading face classifier path: "+faceClassifierPath);
    }
    this.faceClassifier = faceClassifier;
  }
  
  /**
   * Create a new instance using the specified {@link CascadeClassifier}.
   * @param faceClassifier the classifier - e.g. haarcascade_frontalface_alt.xml
   */
  public FaceDetector(CascadeClassifier faceClassifier) {
    this.faceClassifier = faceClassifier;
  }
  
  /**
   * Detect faces present in {@code rawRgbFrame}.
   * 
   * <p>The processing is:
   * <br>
   * rawRgbFrame -> {@link #resize(Mat) resize} -> {@link #toGrayscale(Mat) toGrayscale} 
   *  -> {@link #detectFaceRects(Mat) detectFaceRects} -> {@link #extractFaces(Mat, MatOfRect) extractFaces}
   * 
   * <p>The detection results are returned in a {@link FacesData}.
   * Specifically, {@code data.rgbFrame} contains the rgb frame from
   * which faces were detected, {@code data.faceRects} are rectangles
   * in {@code rbgFrame} bounding the detected faces, and {@code data.faces}
   * are the detected face images extracted from {@code rbgFrame}.
   * 
   * @param rawRgbFrame the frame to analyze
   * @return {@link FacesData}
   */
  public FacesData detectFaces(Mat rawRgbFrame) {
    // rawRgbFrame -> resize -> toGrayscale -> detectFaceRects -> extractFaces
    
    // resize
    Mat rgbFrame = resize(rawRgbFrame);

    // to grayscale
    Mat grayFrame = toGrayscale(rgbFrame);

    // detect faces
    MatOfRect faceRects = detectFaceRects(grayFrame);

    // extract faces
    List<Mat> faces = extractFaces(rgbFrame, faceRects);
    
    FacesData data = new FacesData(faces);
    data.rgbFrame = rgbFrame;
    data.faceRects = faceRects;
    
    return data;
  }
  
  public Mat resize(Mat frame) {
    Mat resized = new Mat();
    Imgproc.resize(frame, resized, 
        new Size(frame.width()/resizeFactor, frame.height()/resizeFactor));
    return resized;
  }

  public Mat toGrayscale(Mat rgbFrame) {
    Mat grayFrame = new Mat();
    Imgproc.cvtColor(rgbFrame, grayFrame, Imgproc.COLOR_BGRA2GRAY);
    Imgproc.equalizeHist(grayFrame, grayFrame);
    return grayFrame;
  }
  
  public MatOfRect detectFaceRects(Mat frame) {
    MatOfRect faces = new MatOfRect();
    faceClassifier.detectMultiScale(frame, faces);
    return faces;
  }
  
  public List<Mat> extractFaces(Mat rgbFrame, MatOfRect faceRects) {
    List<Mat> faces = new ArrayList<>();
    for (Rect faceRect : faceRects.toArray()) {
      Mat rgbFaceFrame = new Mat(rgbFrame, faceRect);
      rgbFaceFrame = rgbFaceFrame.clone(); // don't inherit subsequent changes to rgbFrame
      faces.add(rgbFaceFrame);
    }
    return faces;
  }
  
  /**
   * Change the resize factor.
   * @param resizeFactor
   * @return the prior resizeFactor
   */
  public int setResizeFactor(int resizeFactor) {
    int old = this.resizeFactor;
    this.resizeFactor = resizeFactor;
    return old;
  }

}
