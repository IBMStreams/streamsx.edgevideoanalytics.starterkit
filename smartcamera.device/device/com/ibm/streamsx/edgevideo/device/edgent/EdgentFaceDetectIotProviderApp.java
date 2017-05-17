/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.edgent;
import java.util.Arrays;
import java.util.List;

import org.apache.edgent.connectors.iot.IotDevice;
import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.providers.iot.IotProvider;
import org.apache.edgent.topology.TStream;

import com.google.gson.JsonObject;

/**
 * A version of the Edgent code that uses an IotProvider in order to 
 * benefit from the features it provides.
 */
public class EdgentFaceDetectIotProviderApp extends EdgentFaceDetectApp {
  protected static String usage = "[-h] [--fps=N | --pollMsec=N] [--resize=N] [--noautostart] {--quickstart | iot-device-cfg-file}  # see scripts/wiotp-device.cfg";

  protected boolean autoSubmit = true;

  public static void main (String args[]) throws Exception {
    new EdgentFaceDetectIotProviderApp().run(args);
  }
	
  @Override
	protected void buildAndStartTopologies() throws Exception {
    camera.setAdjustFps(false);

    // The standard Edgent IotProvider pattern of:
    //    create provider, register topology builder(s), start provider
    
    IotProvider provider = new IotProvider(topology -> newIotHubConnector(topology));
    
    // uses edgent-1.1.0 auto-submit feature
    provider.registerTopology("face-detect", 
        (iotDevice, config) -> buildTopology(iotDevice, config), autoSubmit, null);
    
    // come back to talk about this if their's time
    registerExtraFeatureDemoStuff(provider);
    
    provider.start();
	}

  
  protected void registerExtraFeatureDemoStuff(IotProvider provider) {
    
    // demo an application custom device command handler
    provider.registerTopology("demoCmdHandler", 
        (iotDevice, config) -> buildDemoCmdHandler(iotDevice, config), true, null);

    // demo an application custom control bean
    registerDemoControl(provider.getServices().getService(ControlService.class));
    
  }

  protected void buildDemoCmdHandler(IotDevice iotDevice, JsonObject config) {
    
    // it's easy to define and service your own device commands
    
    TStream<JsonObject> demoEchoCmds = iotDevice.commands("demoEchoCmdId");
        
    demoEchoCmds.sink(echoCmd -> System.out.println("Received device cmd demoEchoCmdId payload: " + echoCmd));
    
  }
  
  protected void registerDemoControl(ControlService controlSvc) {
    
    // It's easy to define and register your own Controls and they're
    // automatcally supported by the provider's "edgentControl" device cmds
    // handler
    
    String controlInstanceAlias = "demoControl-1";
    
    controlSvc.registerControl(MyDemoControl.CONTROL_TYPE, 
        "MyDemoControl-"+System.currentTimeMillis(), controlInstanceAlias, 
        MyDemoControlMXBean.class, new MyDemoControl(controlInstanceAlias));
  }
  
  public interface MyDemoControlMXBean {
    public void echo(String s);
  }
  
  public static class MyDemoControl implements MyDemoControlMXBean {
    public static String CONTROL_TYPE = "MyDemoControl";
    private String controlInstanceAlias;
    
    public MyDemoControl(String controlInstanceAlias) {
      this.controlInstanceAlias = controlInstanceAlias;
    }
    
    @Override
    public void echo(String s) {
      System.out.println("Hello from MyDemoControl-"+controlInstanceAlias+ " echo(): " + s);
    }
  }
  
  protected void processArgs(String[] args) throws Exception {
    List<String> argList = Arrays.asList(args);
    if (argList.size() == 0 || argList.contains("-h") || argList.contains("--help"))
      throw new Exception("Usage: " + usage);

    super.processArgs(args);
    
    autoSubmit = ! argList.contains("--noautostart");
  }
	
}
