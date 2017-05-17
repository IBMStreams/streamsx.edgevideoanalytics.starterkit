/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device;

public class Stats {
  private long startMillis;
  private long lastReportMillis;
  private long frameProcessedCnt;
  public Timer getFrame = new Timer();
  public Timer imgProcess = new Timer();;
  public Timer render = new Timer();;
  public Timer publish = new Timer();;
  
  public static class Timer {
    public long cnt;
    public long totMsec;
    public long startTime;
    
    public void reset() {
      cnt = 0;
      totMsec = 0;
      startTime = 0;
    }
    
    public void markStart() {
      startTime = System.currentTimeMillis();
    }
    
    public void markEnd() {
      cnt++;
      if (startTime != 0) {
        totMsec += System.currentTimeMillis() - startTime;
        startTime = 0;
      }
    }
    
    public long getAvgMsec() {
      if (cnt > 0)
        return totMsec / cnt;
      return -1;
    }
  }
  
  public Stats start() {
    startMillis = System.currentTimeMillis();
    lastReportMillis = startMillis;
    return this;
  }
  
  public long getFrameProcessedCnt() {
    return frameProcessedCnt;
  }
  
  public Stats reportFrameProcessed() {
    frameProcessedCnt++;
    long now = System.currentTimeMillis();
    if (now - lastReportMillis > 1000) {
      long elapsedSec = (now - startMillis) / 1000;
      double intervalFps = -1.0;
      if (getFrame.cnt != 0) {
        intervalFps = getFrame.cnt / ((double) (now - lastReportMillis) / 1000); 
      }
      double fps = (double) frameProcessedCnt / elapsedSec;
      lastReportMillis = now;
      System.out.println(String.format(
        "# elapsedSec: %2d frameCnt: %2d fps: %2.0f intervalFps: %2.0f  (avgMsec getFrame: %2d imgProc: %2d render: %d publish: %2d)", 
        elapsedSec, frameProcessedCnt, fps, intervalFps,
        getFrame.getAvgMsec(), imgProcess.getAvgMsec(), render.getAvgMsec(), publish.getAvgMsec()));
      
      getFrame.reset();
      imgProcess.reset();
      render.reset();
      publish.reset();
    }
    return this;

  }

}
