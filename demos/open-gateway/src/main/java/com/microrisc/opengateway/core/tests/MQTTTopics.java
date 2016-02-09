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
    
    public static final String ACTUATORS_LEDS_STD = CLIENT_ID + "/std/actuators/leds";
    public static final String ACTUATORS_LEDS_LP = CLIENT_ID + "/lp/actuators/leds";
    
    public static final String SENSORS_THERMOMETERS_STD = CLIENT_ID + "/std/sensors/thermometers";
    public static final String SENSORS_THERMOMETERS_LP = CLIENT_ID + "/lp/sensors/thermometers";
    
    public static final String ASYNCHRONOUS_RESPONSES_STD = CLIENT_ID + "/std/asynchronous/responses";
    public static final String ASYNCHRONOUS_RESPONSES_LP = CLIENT_ID + "/lp/asynchronous/responses";
}