/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.microrisc.opengateway;

import com.microrisc.opengateway.iqrf.dpa.DPAController;
import com.microrisc.opengateway.mqtt.MQTTController;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author spinarr
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

        private final String password = null;
        private final String userName = null;

        private void runOpenGatewayLogic() throws MqttException {

            // IQRF DPA
            DPAController dpaController = new DPAController(NODES_COUNT);
            dpaController.initDPASimplyAndGetNodes();
            dpaController.runTasks();

            // IQRF MQTT
            String url = protocol + broker + ":" + port;
            MQTTController mqttController = new MQTTController(url, clientId, cleanSession, quietMode, userName, password);
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
