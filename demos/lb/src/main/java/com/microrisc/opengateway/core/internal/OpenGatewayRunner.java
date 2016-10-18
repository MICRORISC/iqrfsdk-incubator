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

import com.microrisc.opengateway.core.utils.dpa.DPACommunicatorHandlerImpl;
import com.microrisc.opengateway.core.tests.MQTTCommunicator;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rostislav Spinar
 */
public class OpenGatewayRunner{

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(OpenGatewayRunner.class);

    private OpenGatewayLogic ogwWorker;
    
    public void createAndStartThreads() {
        ogwWorker = new OpenGatewayLogic();
        ogwWorker.start();
    }

    public void terminateThread() {
        // sending interrupt singal into thread
        ogwWorker.interrupt();

        while (ogwWorker.isAlive()) {
            try {
                ogwWorker.join();
            } catch (InterruptedException ex) {
                logger.warn("Termination - GatewayWorker interrupted");
            }
        }
    }

    private class OpenGatewayLogic extends Thread {

        // references for DPA
        private final int NODES_COUNT = 1;

        // references for MQTT
        private final String protocol = "tcp://";
        private final String broker = "localhost";
        private final int port = 1883;

        private final String clientId = "open-gateway";
        private final String subTopic = "data/in";
        private final String pubTopic = "data/out";

        private final boolean cleanSession = true;
        private final boolean quietMode = false;
        private final boolean ssl = false;

        private final String certFile = null;
        private final String password = null;
        private final String userName = null;

        private void runOpenGatewayLogic() throws MqttException {

            // IQRF DPA
            DPACommunicatorHandlerImpl dpaController = new DPACommunicatorHandlerImpl(NODES_COUNT);
            dpaController.initDPASimplyAndGetNodes();
            dpaController.runTasks();

            // IQRF MQTT
            String url = protocol + broker + ":" + port;
            MQTTCommunicator mqttController = new MQTTCommunicator(url, clientId, cleanSession, quietMode, userName, password, certFile, "STD");
            mqttController.subscribe(subTopic, 2);
        }

        @Override
        public void run() {

            try {
                runOpenGatewayLogic();
            } catch (MqttException ex) {
                logger.error("Mqtt exception - " + ex);
            }

            while (!this.isInterrupted()) {
                // sending and receiving data based on data and time events
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    logger.error("Sleep exception - " + ex);
                }
            }
        }
    }
}
