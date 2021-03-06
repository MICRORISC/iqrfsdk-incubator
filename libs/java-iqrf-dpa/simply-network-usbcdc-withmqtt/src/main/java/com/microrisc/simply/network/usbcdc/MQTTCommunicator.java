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

package com.microrisc.simply.network.usbcdc;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import java.sql.Timestamp;

/**
 *
 * @author Rostislav Spinar
 */
public class MQTTCommunicator implements MqttCallback {

    // Private instance variables
    private MqttClient client;
    private String brokerUrl;
    private boolean quietMode;
    private MqttConnectOptions conOpt;
    private boolean clean;
    private String password;
    private String userName;
    private String netType;

    /**
     * Constructs an instance of the sample client wrapper
     *
     * @param brokerUrl the url of the server to connect to
     * @param clientId the client id to connect with
     * @param cleanSession clear state at end of connection or not (durable or
     * non-durable subscriptions)
     * @param quietMode whether debug should be printed to standard out
     * @param userName the username to connect with
     * @param password the password for the user
     * @throws MqttException
     */
    public MQTTCommunicator(String brokerUrl, String clientId, boolean cleanSession, boolean quietMode, String userName, String password, String netType) throws MqttException {
        
        this.brokerUrl = brokerUrl;
        this.quietMode = quietMode;
        this.clean = cleanSession;
        this.password = password;
        this.userName = userName;
        this.netType = netType;
        
    	//This sample stores in a temporary directory... where messages temporarily
        // stored until the message has been delivered to the server.
        //..a real application ought to store them somewhere
        // where they are not likely to get deleted or tampered with
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);

        try {
            // Construct the connection options object that contains connection parameters
            // such as cleanSession and LWT
            conOpt = new MqttConnectOptions();
            conOpt.setCleanSession(clean);
            
            if (password != null) {
                conOpt.setPassword(this.password.toCharArray());
            }
            if (userName != null) {
                conOpt.setUserName(this.userName);
            }

            // Construct an MQTT blocking mode client
            client = new MqttClient(this.brokerUrl, clientId, dataStore);

            // Set this wrapper as the callback handler
            client.setCallback(this);
            
            // Connect to the MQTT server
            log("Connecting to " + brokerUrl + " with client ID " + client.getClientId());
            client.connect(conOpt);
            log("Connected");

        } catch (MqttException e) {
            e.printStackTrace();
            log("Unable to set up client: " + e.toString());
            System.exit(1);
        }
    }

    /**
     * Publish / send a message to an MQTT server
     *
     * @param topicName the name of the topic to publish to
     * @param qos the quality of service to delivery the message at (0,1,2)
     * @param payload the set of bytes to send to the MQTT server
     * @throws MqttException
     */
    public void publish(String topicName, int qos, byte[] payload) throws MqttException {

        // Connect to the MQTT server
        //log("Connecting to " + brokerUrl + " with client ID " + client.getClientId());
        //client.connect(conOpt);
        //log("Connected");

        String time = new Timestamp(System.currentTimeMillis()).toString();
        log("Publishing at: " + time + " to topic \"" + topicName + "\" qos " + qos);

        // Create and configure a message
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);

    	// Send the message to the server, control is not returned until
        // it has been delivered to the server meeting the specified
        // quality of service.
        client.publish(topicName, message);

        // Disconnect the client
        //client.disconnect();
        //log("Disconnected");
    }

    /**
     * Subscribe to a topic on an MQTT server Once subscribed this method waits
     * for the messages to arrive from the server that match the subscription.
     * It continues listening for messages until the enter key is pressed.
     *
     * @param topicName to subscribe to (can be wild carded)
     * @param qos the maximum quality of service to receive messages at for this
     * subscription
     * @throws MqttException
     */
    public void subscribe(String topicName, int qos) throws MqttException {
        
        // Connect to the MQTT server
        //client.connect(conOpt);
        //log("Connected to " + brokerUrl + " with client ID " + client.getClientId());

    	// Subscribe to the requested topic
        // The QoS specified is the maximum level that messages will be sent to the client at.
        // For instance if QoS 1 is specified, any messages originally published at QoS 2 will
        // be downgraded to 1 when delivering to the client but messages published at 1 and 0
        // will be received at the same level they were published at.
        log("Subscribing to topic \"" + topicName + "\" qos " + qos);
        client.subscribe(topicName, qos);

        // Disconnect the client from the server
        //client.disconnect();
        //log("Disconnected");
    }

    /**
     * Utility method to handle logging. If 'quietMode' is set, this method does
     * nothing
     *
     * @param message the message to log
     */
    private void log(String message) {
        if (!quietMode) {
            System.out.println(message);
        }
    }

    /**
     * @see MqttCallback#connectionLost(Throwable)
     */
    public void connectionLost(Throwable cause) {
	
        // Called when the connection to the server has been lost.
        // An application may choose to implement reconnection
        // logic at this point. This sample simply exits.
        log("Connection to " + brokerUrl + " lost!" + cause);

        // Connect to the MQTT server
        log("Reconnecting to " + brokerUrl + " with client ID " + client.getClientId());
        
        conOpt = new MqttConnectOptions();
        conOpt.setCleanSession(false);
        
        try {
            client.connect(conOpt);
            
            if(netType.equals("STD")) {
                subscribe(MQTTTopics.STD_DPA_PACKETS, 2);
            } else if(netType.equals("LP")) {
                subscribe(MQTTTopics.LP_DPA_PACKETS, 2);
            }
        } catch (MqttException ex) {
            log("Reconnecting to " + brokerUrl + " with client ID " + client.getClientId() + "failed!" + ex.getMessage());
        }
        
        log("Connected");
    }

    /**
     * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
     */
    public void deliveryComplete(IMqttDeliveryToken token) {
        
        // Called when a message has been delivered to the
        // server. The token passed in here is the same one
        // that was passed to or returned from the original call to publish.
        // This allows applications to perform asynchronous
        // delivery without blocking until delivery completes.
        //
        // This sample demonstrates asynchronous deliver and
        // uses the token.waitForCompletion() call in the main thread which
        // blocks until the delivery has completed.
        // Additionally the deliveryComplete method will be called if
        // the callback is set on the client
        //
        // If the connection to the server breaks before delivery has completed
        // delivery of a message will complete after the client has re-connected.
        // The getPendingTokens method will provide tokens for any messages
        // that are still to be delivered.
    }

    /**
     * @see MqttCallback#messageArrived(String, MqttMessage)
     */
    public void messageArrived(String topic, MqttMessage message) throws MqttException {
	
        // Called when a message arrives from the server that matches any
        // subscription made by the client
        
        String time = new Timestamp(System.currentTimeMillis()).toString();
        System.out.println("Time:\t" + time
                           + "  Topic:\t" + topic
                           + "  Message:\t" + new String(message.getPayload())
                           + "  QoS:\t" + message.getQos());
        
    }
}
