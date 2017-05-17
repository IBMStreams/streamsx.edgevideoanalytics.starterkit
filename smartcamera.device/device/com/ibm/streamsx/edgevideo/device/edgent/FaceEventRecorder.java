/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.edgent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.edgent.function.Consumer;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import com.google.gson.JsonObject;

/**
 * Record a face detection events to files.
 * 
 * <p>Just something to create a dataset for development / testing.
 * 
 * <p>For each event generates:
 * <pre>{@code
 * /tmp/faceDetectEventRecordings/event-<n>-json.txt // JSON of JsonObject from JsonFaceEvent.toJsonObject()
 * /tmp/faceDetectEventRecordings/event-<n>-mat.png  // png of Mat from JsonFaceEvent.getFace(jsonFaceEvent)
 * }</pre>
 */
public class FaceEventRecorder {
  private long maxRecordingCnt = 25;
  private long imgCount = 0;
  private File dir = new File("/tmp/faceDetectEventRecordings/");
  
  private FaceEventRecorder() {
    if (!dir.exists()) {
      dir.mkdirs();
    }
  }
  
  public static Consumer<JsonObject> newConsumer() {
    FaceEventRecorder recorder = new FaceEventRecorder();
    return data -> recorder.recordEvent(data);
  }
  
  public void recordEvent(JsonObject faceDetectionEvent) {
    if (++imgCount > maxRecordingCnt)
      return;
    System.out.println("Recording event number " + imgCount);
    Mat face = JsonFaceEvent.getFace(faceDetectionEvent);
    write(imgCount, face);
    write(imgCount, faceDetectionEvent);
  }
  
  private void write(long cnt, Mat frame) {
    File file = new File(dir, String.format("event-%03d-mat.png", cnt));
    Imgcodecs.imwrite(file.toString(), frame);
  }
  
  private void write(long cnt, JsonObject jo) {
    File file = new File(dir, String.format("event-%03d-json.txt", cnt));
    try(FileWriter writer = new FileWriter(file)) {
      writer.write(jo.toString());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
