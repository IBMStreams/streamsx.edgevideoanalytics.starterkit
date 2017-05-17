/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.edgent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.edgent.execution.services.ControlService;
import org.apache.edgent.execution.services.RuntimeServices;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.topology.Topology;

/**
 * Workaround EDGENT-400
 * 
 * An edgentControl stopApp device cmd followed by a startApp cmd yields an IllegalStateException
 * when using top.poll(...).alias("sensorPollStream") because start tries to register a 
 * PeriodMXBean and stop didn't unregister it.
 *  
 * <pre> 
 * SEVERE: Control id: stream:sensorPollStream already exists
Mar 22, 2017 3:03:24 PM org.apache.edgent.runtime.etiao.Executable invokeAction
SEVERE: Exception caught while invoking action: {}
java.util.concurrent.ExecutionException: java.lang.IllegalStateException
  ...
Caused by: java.lang.IllegalStateException
  at org.apache.edgent.runtime.jsoncontrol.JsonControlService.registerControl(JsonControlService.java:123)
  at org.apache.edgent.oplet.core.PeriodicSource.start(PeriodicSource.java:57)
  at org.apache.edgent.runtime.etiao.Invocation.start(Invocation.java:189)
  ...
  </pre>
 * 
 * <p>This class attempts to work around the problem by enabling clients to
 * {@link #addControlId(Topology, String, String, Class) addControlId} 
 * to keep track of and remove a control upon topology shutdown.  
 * 
 * <p>To achieve that this class creates a Supplier<Iteratable<String>> and adds it
 * as Topology.source().
 * 
 * <p>Create one instance of this class for the entire application.
 * 
 * Sample use:
 * <pre>{@code
 * class MyApp {
 *   WorkaroundEdgent400 hack = new WorkaroundEdgent400();
 *   
 *   ...  // an IotProvider based app
 *   
 *   void buildTopology(IotDevice iotDevice, JsonObject config) {
 *     Topology top = iotDevice.topology(); 
 *     top.poll(...).alias("sensorPollStream").print();
 *      
 *     hack.addControlId(top, TStream.TYPE, "sensorPollStream", PeriodMXBean.class);
 *   }
 * }</pre>
 */
public class WorkaroundEdgent400 {
  private Map<String,MySupplier> topSuppliers = Collections.synchronizedMap(new HashMap<>());

  /**
   * Register a control to unregister upon topology shutdown.
   * 
   * See {@link ControlService#getControlId(String, String, Class) getControlId()} for the args
   */
  @SuppressWarnings("javadoc")
  public <T> void addControlId(Topology top, String type, String alias, Class<T> controlInterface) {
    if (!topSuppliers.containsKey(top.getName())) {
      topSuppliers.put(top.getName(), new MySupplier(top));
      
      top.source(topSuppliers.get(top.getName()));  // so supplier gets called upon top shutdown
    }
    MySupplier mySupplier = topSuppliers.get(top.getName());
    mySupplier.addControlId(type, alias, controlInterface);
  }
  
  private static class MySupplier implements Supplier<Iterable<String>>, AutoCloseable {
    private static final long serialVersionUID = 1L;
    private Supplier<RuntimeServices> services;
    private Map<String,Supplier<String>> controlIds = new HashMap<>();  // uniq-key, Supplier<controlId>
    
    MySupplier(Topology top) {
      services = top.getRuntimeServiceSupplier();
    }

    <T> void addControlId(String type, String alias, Class<T> controlInterface) {
      String key = type+"::"+alias;
      controlIds.put(key, newControlIdFn(type, alias, controlInterface));
    }
    
    private <T> Supplier<String> newControlIdFn(String type, String alias, Class<T> controlInterface) {
      return () -> {
        ControlService cs = getControlService();
        return cs.getControlId(type, alias, controlInterface);
      };
    }
    
    @Override
    public void close() throws Exception {
      System.out.println("WorkaroundEdgent400 close() called");
      
      ControlService cs = getControlService();
      for (Entry<String,Supplier<String>> e : controlIds.entrySet()) {
        String controlId = e.getValue().get();
        System.out.println("WorkaroundEdgent400.close() - unregister " + controlId);
        if (controlId != null)
          cs.unregister(controlId);
      }
    }

    @Override
    public Iterable<String> get() {
      return Collections.emptyList();
    }
    
    private ControlService getControlService() {
      RuntimeServices rts = services.get();
      return rts.getService(ControlService.class);
    }
    
  }
  
}
