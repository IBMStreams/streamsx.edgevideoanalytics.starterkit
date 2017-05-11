/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.edgent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.opencv.core.Mat;

import com.google.gson.JsonObject;

/**
 * Face detection event info to/from a JsonObject
 */
@SuppressWarnings("javadoc")
public class JsonFaceEvent {
  private static DateFormat timestampFormatter = newIso8601Formatter();
  
  /** Create a JsonObject FaceEvent */
  public static JsonObject toJsonObject(long timestamp, Mat face) {
    
    JsonObject payload = new JsonObject();
    payload.addProperty("timestamp", encodeTimestamp(timestamp));
    payload.add("face", JsonMat.toJsonObject(face));
    
    return payload;
  }
  
  /** Get the timestamp from a FaceEvent */
  public static String getTimestamp(JsonObject faceEvent) {
    return faceEvent.get("timestamp").getAsString();
  }
  
  /** Get the face from a FaceEvent */
  public static Mat getFace(JsonObject faceEvent) {
    return JsonMat.fromJsonObject(faceEvent.getAsJsonObject("face"));
  }
  
  private static String encodeTimestamp(long timestamp) {
    synchronized(timestampFormatter) {
      return timestampFormatter.format(timestamp);
    }
  }
  
  private static DateFormat newIso8601Formatter() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    return df;
  }

}
