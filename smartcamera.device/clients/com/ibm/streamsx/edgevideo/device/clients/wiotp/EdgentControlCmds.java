/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2017
 */
package com.ibm.streamsx.edgevideo.device.clients.wiotp;

import java.util.concurrent.TimeUnit;

import org.apache.edgent.execution.mbeans.JobMXBean;
import org.apache.edgent.runtime.jsoncontrol.JsonControlService;
import org.apache.edgent.topology.mbeans.ApplicationServiceMXBean;
import org.apache.edgent.topology.services.ApplicationService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A utility class for composing JsonObject "Edgent Control" commands. 
 */
public class EdgentControlCmds {
  
  //private static String EDGENT_CONTROL_CMD_ID = Commands.CONTROL_SERVICE; 
  private static String EDGENT_CONTROL_TYPE_KEY = JsonControlService.TYPE_KEY; 
  private static String EDGENT_CONTROL_ALIAS_KEY = JsonControlService.ALIAS_KEY; 
  private static String EDGENT_CONTROL_OP_KEY = JsonControlService.OP_KEY; 
  private static String EDGENT_CONTROL_ARGS_KEY = JsonControlService.ARGS_KEY; 
  
  public static JsonObject mkControlCmd(String type, String alias, String op, String... args) {
    return mkControlCmd(type, alias, op, toJsonArray(args));
  }
  
  public static JsonObject mkControlCmd(String type, String alias, String op, JsonArray args) {
    JsonObject cmd = new JsonObject();
    cmd.addProperty(EDGENT_CONTROL_TYPE_KEY, type);
    cmd.addProperty(EDGENT_CONTROL_ALIAS_KEY, alias);
    cmd.addProperty(EDGENT_CONTROL_OP_KEY, op);
    cmd.add(EDGENT_CONTROL_ARGS_KEY, args);
    
    return cmd;
  }
  
  /** 
   * Make a command for JobMXBean.stateChange(Job.Action.CLOSE)
   * @param appName 
   * @return 
   */
  public static JsonObject mkStopAppCmd(String appName) {
    return mkControlCmd(JobMXBean.TYPE, appName, "stateChange", "CLOSE");
  }

  /** 
   * Make a command for ApplicationServiceMXBean.submit(applicationName, jsonObjectConfig)
   * @param appName 
   * @return 
   */
  public static JsonObject mkStartAppCmd(String appName) {
    JsonArray args = toJsonArray(appName);
    args.add(new JsonObject());  // empty jsonConfig object
    JsonObject cmd = mkControlCmd(ApplicationServiceMXBean.TYPE, ApplicationService.ALIAS, 
        "submit", args);
    return cmd;
  }

  /**
   * Make a command for PeriodMXBean.setPeriod(long period, TimeUnit unit)
   * @param beanEntityType - e.g., TStream.TYPE
   * @param beanEntityAlias - e.g., "myStreamAlias"
   * @param period
   * @param unit
   * @return
   */
  public static JsonObject mkPeriodMXBeanSetPeriodCmd(String beanEntityType, String beanEntityAlias, long period, TimeUnit unit) {
    return mkControlCmd(beanEntityType, beanEntityAlias, 
        "setPeriod", Long.valueOf(period).toString(), unit.name());
  }
  
  private static JsonArray toJsonArray(String... strings) {
    JsonArray ja = new JsonArray();
    for (String s : strings)
      ja.add(new JsonPrimitive(s));
    return ja;
  }

}
