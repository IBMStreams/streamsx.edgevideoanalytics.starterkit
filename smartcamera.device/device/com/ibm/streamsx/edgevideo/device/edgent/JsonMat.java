/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.edgent;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.google.gson.JsonObject;

/**
 * OpenCV Mat to/from JsonObject
 * 
 * TODO semi-lame, presumably base64 of jpg or png would be better.
 */
public class JsonMat {

  public static JsonObject toJsonObject(Mat mat) {
    JsonObject jo = new JsonObject();
    jo.addProperty("width", mat.width());
    jo.addProperty("height", mat.height());
    jo.addProperty("type", mat.type());
    jo.addProperty("channels", mat.channels());
    jo.addProperty("depth", mat.depth());
    jo.addProperty("mat", base64MimeEncodeMat(mat));
    return jo;
  }

  public static Mat fromJsonObject(JsonObject joMat) {
    int width = joMat.getAsJsonPrimitive("width").getAsInt();
    int height = joMat.getAsJsonPrimitive("height").getAsInt();
    int type = joMat.getAsJsonPrimitive("type").getAsInt();
    Mat face = JsonMat.base64MimeDecodeMat(width, height, type, joMat.get("mat").getAsString());
    return face;
  }

  private static String base64MimeEncodeMat(Mat mat) {
    int width = mat.width(), height = mat.height(), channels = mat.channels();
    
    // resize if needed
    // With initial resize factor of 4 and being within 2' of the MBP camera,
    // a face image seems to be on the order of 15Kb.
    if (width*height*channels > 50*1024) {
      Mat smallerFace = new Mat();
      int resizeFactor = 2;
      Imgproc.resize(mat, smallerFace, 
          new Size(mat.width()/resizeFactor, mat.height()/resizeFactor));
      mat = smallerFace;
      width = mat.width(); height = mat.height(); channels = mat.channels();
    }
    
    byte[] sourcePixels = new byte[width * height * channels];  
    mat.get(0, 0, sourcePixels);
  
    // Base64 encode the image to be able to package in JsonObject
    // java.utils.Base64 since 1.8, otherwise use Apache Commons
    Encoder encoder = Base64.getMimeEncoder();
    String base64 = encoder.encodeToString(sourcePixels);
  
    //System.out.println("pub face bytes size: " + sourcePixels.length + " base64 size:" + base64.length());
    
    return base64;
  }

  private static Mat base64MimeDecodeMat(int width, int height, int type, String base64MimeMatStr) {
    // java.utils.Base64 since 1.8, otherwise use Apache Commons
    Decoder decoder = Base64.getMimeDecoder();
    byte[] sourcePixels = decoder.decode(base64MimeMatStr);
  
    //System.out.println(String.format("base64DecodeMat: width=%d height=%d type=%d", width, height, type));
    
    Mat mat = new Mat(height, width, type);
    mat.put(0,  0, sourcePixels);
    
    return mat;
  }

}
