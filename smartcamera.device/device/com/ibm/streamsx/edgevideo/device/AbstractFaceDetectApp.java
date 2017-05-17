/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * An abstract class providing general (non-Edgent) infrastructure upon which to build
 * specific OpenCV based face detection applications.
 * 
 * <p>An implementation must supply a {@link #runFaceDetection()} implementation
 * and call {@link #run(String[])}.
 * 
 * <p>Of particular interest to implementations will be the 
 * {@link #camera} and {@link #faceDetector} members created by this class,
 * as well as {@link #renderImages(Mat, MatOfRect, List)}.
 * 
 * <p>With appropriate setup, runs on OSX, Raspberry Pi, ...
 * 
 * <p>Type this command before starting the program to ensure
 * that OpenCV will be able to use the Raspberry Pi camera
 * <pre>
 * sudo modprobe bcm2835-v4l2
 * </pre>
 * 
 * 20170502 - change default resize factor to 3
 * 20170502 - move edgent device app code into com.ibm.streamsx.edgevideo.device.edgent, remove Pipeline variant
 * 20170502 - add setFps=<fps> cmd to cmd clients
 * 20170502 - tweak rendering panel sizes
 * 20170502 - migrate TStream alias & edgent400 workaround into base app to better highlight IotProvider
 * 20170502 - make base app's fluent topology builder better visually (spacing)
 * 20170427 - Add camera FPS setting on Pi to avoid frame delay at tgt-FPS < 10.  See Camera.java
 * 
 * TODO - README.md in anticipation of addition to a github repo
 * TODO - Add optional controllable periodic publish of full image to a separate eventId
 * TODO - ability to specify initial poll rate as CLI arg, maybe also as startApp control cmd 
 * TODO - think about ApplicationClient being able to tell the stopped/started status of a topology
 */
public abstract class AbstractFaceDetectApp {

  protected static String usage = "[-h] [--fps=N | --pollMsec=N] [--resize=N]";

  protected static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  private static String OPENCV_HOME_DEFAULT = "/usr/local/opt/opencv3/share/OpenCV";
  private static String HAARCASCADE_DIR_NAME = "haarcascades";
  private static String FACE_CLASSIFIER_NAME = "haarcascade_frontalface_alt.xml";
	//private static String FACE_CLASSIFIER_NAME = "haarcascade_frontalface_default.xml";

  protected String faceClassifierPath;

  protected MyPanel faceDetectPanel;     // panel for rendering the (augmented) rgbFrame
  protected MyPanel detectedFacesPanel;  // panel for rendering the detected faces
  public static int DETECTED_FACES_PANEL_WIDTH = 250;
  public static int DETECTED_FACES_PANEL_HEIGHT = 200;
  protected boolean renderDetections = false;  // include detectedFacesPanel?
  protected int resizeFactor = -1;

  protected long sensorPollValue = 1000/3;   // 3/sec
  protected TimeUnit sensorPollUnit = TimeUnit.MILLISECONDS;

  protected Stats stats;
  protected Camera camera;
  protected FaceDetector faceDetector;
	
	protected AbstractFaceDetectApp() {
	  
    // Init OpenCV - Load the native library.
    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    System.out.println("Loaded OpenCV native lib: " + Core.NATIVE_LIBRARY_NAME.toString());

    String opencvHome = System.getenv("OPENCV_HOME");
    if (opencvHome == null) {
      opencvHome = OPENCV_HOME_DEFAULT;
    }
    faceClassifierPath = opencvHome + "/" + HAARCASCADE_DIR_NAME + "/" + FACE_CLASSIFIER_NAME;
	}
	
	protected void run(String args[]) throws Exception {
	  processArgs(args);

	  initPanels();
        
    camera = new Camera(getTgtFps());
    faceDetector = new FaceDetector(new File(faceClassifierPath));
    if (resizeFactor >= 0) {
      faceDetector.setResizeFactor(resizeFactor);
    }
    stats = new Stats();
    
    stats.start();
    
    System.out.println(String.format("SensorPoll: %d %s (%.1f fps target)",
        sensorPollValue, sensorPollUnit.toString(), getTgtFps()));
    
    runFaceDetection();
	}
	
	/**
	 * Do the continuous processing.
	 * 
	 * <p>Applications must provide an implementation of this.
	 * It is called by {@link #run(String[])}.
	 * @throws Exception
	 */
	protected abstract void runFaceDetection() throws Exception;
  
  protected double getTgtFps() {
    return (double)1000 / sensorPollUnit.toMillis(sensorPollValue);
  }
	
	protected String now() {
	  synchronized(sdf) {
	    return sdf.format(new Date(System.currentTimeMillis()));
	  }
	}
  
  protected void processArgs(String[] args) throws Exception {
    List<String> argList = Arrays.asList(args);
    if (argList.contains("-h") || argList.contains("--help"))
      throw new Exception("Usage: " + usage);
    
    for (String arg : argList) {
      if (arg.startsWith("--fps=")) {
        int fps = Integer.valueOf(arg.split("=")[1]);
        sensorPollValue = 1000/fps;
        sensorPollUnit = TimeUnit.MILLISECONDS;
      }
    }
    for (String s : argList) {
      if (s.startsWith("--pollMsec=")) {
        long setPollMsec = Integer.valueOf(s.split("=")[1]);
        sensorPollValue = setPollMsec;
        sensorPollUnit = TimeUnit.MILLISECONDS;
      }
    }
    for (String s : argList) {
      if (s.startsWith("--resize=")) {
        resizeFactor = Integer.valueOf(s.split("=")[1]);
      }
    }
  }
  
  protected void initPanels() {
    faceDetectPanel = MyPanel.newFramedPanel("Device - "+this.getClass().getSimpleName());
    if (renderDetections) {
      detectedFacesPanel = MyPanel.newFramedPanel("DetectedFaces",
          25, 50, DETECTED_FACES_PANEL_WIDTH, DETECTED_FACES_PANEL_HEIGHT);
    }
  }

  protected void renderImages(FacesData facesData) {
    renderImages(facesData.rgbFrame, facesData.faceRects, facesData.faces);
  }
  
  protected void renderImages(Mat rgbFrame, MatOfRect faceRects, List<Mat> faces) {
    // draw rectangles around the detected faces and render
    Rect[] rectArray = faceRects.toArray();
    for (Rect faceRect : rectArray) {
      Imgproc.rectangle(rgbFrame, new Point(faceRect.x, faceRect.y), new Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height), new Scalar(0, 255, 0));
    }
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
