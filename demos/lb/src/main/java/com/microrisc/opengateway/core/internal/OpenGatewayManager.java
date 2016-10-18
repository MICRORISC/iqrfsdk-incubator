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

package com.microrisc.opengateway.core.internal;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rostislav Spinar
 */
public class OpenGatewayManager {

    /** Logger.*/
    private static final Logger logger = LoggerFactory.getLogger(OpenGatewayManager.class);
    
    // references for MQTT
    //private static final String protocol = "tcp://";
    //private static final String broker = "localhost";
    //private static final int port = 1883;
    
    private static OpenGatewayManager thisInstance = new OpenGatewayManager();
    
    public static final HashMap<String, String> publishQueueMQTT;
    public static final HashMap<String, String> controlQueueDPA;
    
    static {
        // topic + message
        publishQueueMQTT = new HashMap<String, String>();
        // topic + message
        controlQueueDPA = new HashMap<String, String>();
    }
    
    private OpenGatewayManager() {
    }
    
    public static OpenGatewayManager getInstance() {
        return thisInstance;
    }
    
    public HashMap<String, String> getMQTTQueue () {
        return publishQueueMQTT;
    }
    
    public HashMap<String, String> getDPAQueue () {
        return controlQueueDPA;
    }
    
    public void init() {

        // read config from file
        
        // create dpa communicator and connect
        
        //CommunicationHandler dpaCommunicator = new DPACommunicationHandlerImpl();
        //dpaCommunicator.connect();
        
        // create mqtt communicator, connect and subscribe to topics
        
        //String brokerEndPoint = protocol + broker + ":" + port;
        //CommunicationHandler mqttCommunicator = new MQTTCommunicationHandlerImpl();
        //mqttCommunicator.connect();
    }
}
