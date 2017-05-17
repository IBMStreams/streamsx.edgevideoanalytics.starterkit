/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device;

import static org.opencv.videoio.Videoio.CAP_PROP_FPS;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

/**
 * An simple OpenCV based interface to a camera.
 */
public class Camera {
  private int cameraDeviceIndex = 0;
  private VideoCapture camera;
  private volatile boolean initialized;
  private double tgtFps = 0.0;
  private boolean adjustFpsEnabled = true;
  
  public Camera() {
  }
  
  public Camera(double targetFps) {
    this.tgtFps = targetFps;
  }

  /**
   * Grab a frame from the camera. Calls {@link #open()} if needed.
   * @return the frame
   */
  public Mat grabFrame() {
    if (!initialized) {
      open();
    }
    Mat frame = new Mat();
    camera.read(frame);
    return frame;
  }
  
  public void open() {
    if (initialized)
      return;
    camera = new VideoCapture();
    
    // Properties seem to be a sore spot in OpenCV.
    // Some properties aren't supported on certain cameras and it
    // can be hard to tell which are/arent.
    // In some (all?) cases, setting a property before the device
    // is opened has no effect.
    
    camera.open(cameraDeviceIndex);
    initialized = true;
    checkOpened();
    
    adjustForFps();
  }
  
  private void checkOpened() {
    if (!camera.isOpened()) {
      delay(2000);
      if (!camera.isOpened()) {
        throw new RuntimeException("Camera initialization error.\n"
            + "If a Raspberry Pi, ensure that the following command was executed : sudo modprobe bcm2835-v4l2"
            );
      }
    }
  }
  
  private void delay(int ms) {
    try {
      System.out.println("Waiting "+ms+" msec for camera open");
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  public void close() {
    if (camera != null)
      camera.release();
  }
  
  public void adjustForFps(double tgtFps) {
    this.tgtFps = tgtFps;
    if (initialized) {
      adjustForFps();
    }
  }
  
  private void adjustForFps() {
    if (isRaspberryPi())
      adjustFpsPi();
  }
  
  private static boolean isRaspberryPi() {
    return "linux".equalsIgnoreCase(System.getProperty("os.name"))
            && "arm".equalsIgnoreCase(System.getProperty("os.arch"));
  }
  
  public boolean setAdjustFps(boolean enabled) {
    boolean prev = this.adjustFpsEnabled;
    this.adjustFpsEnabled = enabled;
    return prev;
  }
  
  private void adjustFpsPi() {
    
    // N.B. Can causes us problems when dynamically changing the poll rate.
    // e.g., if we initialize for 3fps and we later start polling
    // at 10fps w/o doing another adjustment, the camera will limit
    // us to that 3fps rate.
    
    if (!adjustFpsEnabled || tgtFps >= 10.0) {
      setProp(CAP_PROP_FPS, 0); // use default
      System.out.println("Camera FPS reset to " + Math.round(getProp(CAP_PROP_FPS)));
    }
    else {
      // set to tgtFps - 1
      long fps = Math.round(tgtFps);
      setProp(CAP_PROP_FPS, Math.max(fps - 1, 1));
      System.out.println("Camera FPS is being limited to " + Math.round(getProp(CAP_PROP_FPS)));
    }

    // What's that all about?
    // The goal is to work around an observed
    // "frame buffering delay" at lower target FPS rates.
    // E.g., for a target of fps=3, the grabbed frame
    // is 2-3s in the past.  OpenCV-3.1, jessie, PiCamera.
    //
    // ***On my Pi/PiCamera***
    // By default the CAP_PROP_FPS == 30.
    // It is changeable post-open() and then that value seems
    // to **** be retained even across user logout/login. ****
    // It is reset across reboots - maybe due to some system config file?
    // Setting FPS=0 resets to its default of 30.
    // 
    // On the Pi, the CAP_PROP_BUFFERSIZE isn't supported.
    // It appears that it's >1 as this presumably accounts for the
    // "buffering delay" seen at slow app fps grab rates (e.g., <=3).
    // Setting CAP_PROP_FPS to 2 really does constrain the camera.read()
    // to that fps limit - trying to read faster blocks.
    // Changing FPS to a low number does seem to eliminate the "buffering delay"
    // presumably because there isn't a bunch of images in the buffer.
    //
    // Here's what I'm seeing by default with the "non-edgent" app (Camera FPS=30):
    //    FPS tgt-poll-FPS effective-poll-FPS   delay
    //    30   1            1                   5-6s
    //    30   3            3                   2-3s
    //    30   5            4                   1-1.5s
    //    30   10           7                   <1s
    //    1    1            1                   4s
    //    1    2            1 *                 <1s     
    //    2    1            1                   5-6s
    //    2    2            2                   3-4s
    //    2    3            2 *                 <1
    //    2    5            2 *                 <1
    //    3    1            1                   5-6s
    //    3    2            2                   2-3s
    //    3    3            3                   2-3s
    //    3    4            3 *                 <1s
    //    3    5            3 *                 <1s
    //    4    5            4 *                 <1s
    //    5    5            4                   1-1.5s
    //    5    6            5                   1-1.5s
    //
    // Conclusion / heuristic:
    // For tgt FPS >= 10 use the default (30-FPS)
    // Otherwise, set FPS to (tgt-FPS - 1)
  }
  
  public double setProp(int propId, double value) {
    double was = camera.get(propId);
    boolean b = camera.set(propId, value);
    System.out.println(String.format("Camera.setProp(%d, %.2f) (was %.2f) returned %b",
                       propId, value, was, b));
    return was;
  }
  
  public double getProp(int propId) {
    return camera.get(propId);
  }
}
