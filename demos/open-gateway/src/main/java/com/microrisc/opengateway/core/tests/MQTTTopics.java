/* 
 * Copyright 2016 MICRORISC s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.microrisc.opengateway.core.tests;

/**
 *
 * @author Rostislav Spinar
 */
public class MQTTTopics {
    
    private static final String CLIENT_ID = "b827eb26c73d";
    
    public static final String STD_SENSORS_PROTRONIX = CLIENT_ID + "/std/sensors/protronix";
    public static final String STD_SENSORS_PROTRONIX_DPA_REQUESTS = CLIENT_ID + "/std/sensors/protronix/dpa/requests";
    public static final String STD_SENSORS_PROTRONIX_DPA_CONFIRMATIONS = CLIENT_ID + "/std/sensors/protronix/dpa/confirmations";
    public static final String STD_SENSORS_PROTRONIX_DPA_RESPONSES = CLIENT_ID + "/std/sensors/protronix/dpa/responses";
    
    public static final String STD_SENSORS_AUSTYN = CLIENT_ID + "/std/sensors/austyn";
    public static final String STD_SENSORS_AUSTYN_DPA_REQUESTS = CLIENT_ID + "/std/sensors/austyn/dpa/requests";
    public static final String STD_SENSORS_AUSTYN_DPA_CONFIRMATIONS = CLIENT_ID + "/std/sensors/austyn/dpa/confirmations";
    public static final String STD_SENSORS_AUSTYN_DPA_RESPONSES = CLIENT_ID + "/std/sensors/austyn/dpa/responses";
    
    public static final String LP_SENSORS_IQHOME = CLIENT_ID + "/lp/sensors/iqhome";
    
    public static final String STD_ACTUATORS_AUSTYN = CLIENT_ID + "/std/actuators/austyn";
    public static final String STD_ACTUATORS_DEVTECH = CLIENT_ID + "/std/actuators/devtech";
    public static final String STD_ACTUATORS_DATMOLUX = CLIENT_ID + "/std/actuators/datmolux";
    public static final String STD_ACTUATORS_TECO = CLIENT_ID + "/std/actuators/teco";
    public static final String LP_ACTUATORS_TECO = CLIENT_ID + "/lp/actuators/teco";
    
    public static final String STD_STATUS_DEVTECH = CLIENT_ID + "/std/status/devtech";
    public static final String STD_STATUS_DEVTECH_DPA_REQUESTS = CLIENT_ID + "/std/status/devtech/dpa/requests";
    public static final String STD_STATUS_DEVTECH_DPA_CONFIRMATIONS = CLIENT_ID + "/std/status/devtech/dpa/confirmations";
    public static final String STD_STATUS_DEVTECH_DPA_RESPONSES = CLIENT_ID + "/std/status/devtech/dpa/responses";
    
    public static final String STD_STATUS_DATMOLUX = CLIENT_ID + "/std/status/datmolux";
    public static final String STD_STATUS_DATMOLUX_DPA_REQUESTS = CLIENT_ID + "/std/status/datmolux/dpa/requests";
    public static final String STD_STATUS_DATMOLUX_DPA_CONFIRMATIONS = CLIENT_ID + "/std/status/datmolux/dpa/confirmations";
    public static final String STD_STATUS_DATMOLUX_DPA_RESPONSES = CLIENT_ID + "/std/status/datmolux/dpa/responses";
    
    public static final String LP_STATUS_CITIQ = CLIENT_ID + "/lp/status/citiq";
    public static final String LP_SETTING_CITIQ = CLIENT_ID + "/lp/setting/citiq";
    
    public static final String STD_DPA_REQUESTS = CLIENT_ID + "/std/dpa/requests";
    public static final String STD_DPA_CONFIRMATIONS = CLIENT_ID + "/std/dpa/confirmations";
    public static final String STD_DPA_RESPONSES = CLIENT_ID + "/std/dpa/responses";
    public static final String LP_DPA_REQUESTS = CLIENT_ID + "/lp/dpa/requests";
    public static final String LP_DPA_CONFIRMATIONS = CLIENT_ID + "/lp/dpa/confirmations";
    public static final String LP_DPA_RESPONSES = CLIENT_ID + "/lp/dpa/responses";
    public static final String STD_DPA_ASYNCHRONOUS_RESPONSES = CLIENT_ID + "/std/dpa/asynchronous/responses";
    public static final String LP_DPA_ASYNCHRONOUS_RESPONSES = CLIENT_ID + "/lp/dpa/asynchronous/responses";
}
