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

package com.microrisc.opengateway.core.utils.mqtt;

import com.microrisc.opengateway.core.communication.CommunicationHandlerException;
import com.microrisc.opengateway.core.communication.mqtt.MQTTCommunicationHandler;
import com.microrisc.opengateway.core.internal.OpenGatewayManager;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rostislav Spinar
 */
public class MQTTCommunicationHandlerImpl extends MQTTCommunicationHandler {
    
    /** Logger.*/
    private static final Logger logger = LoggerFactory.getLogger(MQTTCommunicationHandlerImpl.class);
    
    private ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> dataPushServiceHandler;
    
    public MQTTCommunicationHandlerImpl(String clientId,
            String mqttBrokerEndPoint, List<String> subscribeTopics) {
        super(clientId, mqttBrokerEndPoint, subscribeTopics);
    }
    
    public ScheduledFuture<?> getDataPushServiceHandler() {
        return dataPushServiceHandler;
    }
    
    @Override
    public void connect() {
        Runnable connector = new Runnable() {
            public void run() {
                // start up if not up and running already
                while (!isConnected()) {
                    try {
                        connectToBroker();
                        subscribeToBroker();
                        checkAndPublishDeviceData(dataCheckAndPushInterval);

                    } catch (CommunicationHandlerException e) {
                        logger.warn("Connection/Subscription to MQTT Broker at: "
                                + mqttBrokerEndPoint + " failed");

                        try {
                            Thread.sleep(timeoutInterval);
                        } catch (InterruptedException ex) {
                            logger.error("MQTT-Subscriber: Thread Sleep Interrupt Exception");
                        }
                    }
                }
            }
        };

        Thread connectorThread = new Thread(connector);
        connectorThread.setDaemon(true);
        connectorThread.start();
    }
    
    @Override
    public void checkAndPublishDeviceData(int checkInterval) {
        Runnable checkAndPushDataRunnable = new Runnable() {
            @Override
            public void run() {
                
                synchronized(OpenGatewayManager.publishQueueMQTT) {
                    
                    if(!OpenGatewayManager.publishQueueMQTT.isEmpty()) {
                        
                        Iterator it = OpenGatewayManager.publishQueueMQTT.entrySet().iterator();
                        
                        while (it.hasNext()) {
                            // map item
                            Map.Entry pair = (Map.Entry)it.next(); 
                            
                            // publish data
                            publishDeviceData((String)pair.getKey(), (String)pair.getValue());
                            
                            // avoids a ConcurrentModificationException
                            it.remove();
                        }                        
                    }
                }
            }
        };

        dataPushServiceHandler = service.scheduleAtFixedRate(checkAndPushDataRunnable, checkInterval,
                checkInterval, TimeUnit.MILLISECONDS);
    }
 
    private void publishDeviceData(String topic, String message) {        
        
        MqttMessage pushMessage = new MqttMessage();
        pushMessage.setPayload(message.getBytes(StandardCharsets.UTF_8));
        pushMessage.setQos(DEFAULT_MQTT_QUALITY_OF_SERVICE);
        pushMessage.setRetained(true);

        try {
            publishToBroker(topic, pushMessage);
            logger.info("Message: '" + pushMessage
                    + "' published to MQTT Queue at ["
                    + mqttBrokerEndPoint
                    + "] under topic [" + topic + "]");

        } catch (CommunicationHandlerException e) {
            logger.warn("Data publish attempt to topic - ["
                    + topic + "] failed for payload ["
                    + message + "]");
        }
    }
    
    @Override
    public void processIncomingMessage(String topic, MqttMessage message) {
        
        String payload = new String(message.getPayload());
        
        // TODO: parsing shall be handled here and queue shall be designed for DPA jSimply
        
        synchronized (OpenGatewayManager.controlQueueDPA) {
            OpenGatewayManager.controlQueueDPA.put(topic, payload);
            
            logger.info("Message: '" + payload
                    + "' published to internal DPA Queue with topic [" 
                    + topic + "]");
        }
    }
    
    @Override
    public void disconnect() {
        Runnable stopConnection = new Runnable() {
            public void run() {
                while (isConnected()) {
                    try {
                        dataPushServiceHandler.cancel(true);
                        closeConnection();

                    } catch (MqttException e) {
                        if (logger.isDebugEnabled()) {
                            logger.warn("Unable to 'STOP' MQTT connection at broker at: "
                                    + mqttBrokerEndPoint);
                        }

                        try {
                            Thread.sleep(timeoutInterval);
                        } catch (InterruptedException e1) {
                            logger.error("MQTT-Terminator: Thread Sleep Interrupt Exception");
                        }
                    }
                }
            }
        };

        Thread terminatorThread = new Thread(stopConnection);
        terminatorThread.setDaemon(true);
        terminatorThread.start();
    }
}
