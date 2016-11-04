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

package com.microrisc.opengateway.mqtt;

import com.microrisc.opengateway.apps.automation.OpenGatewayAppStd;
import com.microrisc.opengateway.dpa.DPA_Request;
import com.microrisc.opengateway.dpa.DPA_Result;
import com.microrisc.opengateway.dpa.ResponseData;
import com.microrisc.opengateway.web.WebRequestParser;
import com.microrisc.opengateway.web.WebRequestParserException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rostislav Spinar
 * @author Michal Konopa
 */
public class MqttCommunicator implements MqttCallback {
    
    private MqttClient client;
    private String brokerUrl;
    private boolean quietMode;
    private MqttConnectOptions conOpt;
    private boolean clean;
    private String password;
    private String userName;
    private String certFile;
    
    // time  between consecutive attempts to reconnection [in ms]
    private static final int DEFAULT_RECONNECTION_SLEEP_TIME = 3000;
    
    private Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            
            while ( (client != null) && !(client.isConnected()) ) {
                // Connect to the MQTT server
                log("Reconnecting to " + brokerUrl + " with client ID " + client.getClientId());
                
                try {
                    client.connect(conOpt);
                } catch ( MqttException ex ) {
                    log(
                        "Reconnecting to " + brokerUrl + " with client "
                        + "ID " + client.getClientId() + " failed: " + ex.getMessage()
                    );
                }
                
                if ( !client.isConnected() ) {
                   try {
                    Thread.sleep(DEFAULT_RECONNECTION_SLEEP_TIME);
                    } catch ( InterruptedException ex ) {
                        log.warn(ex.toString());                    
                    }
                }
            }
            log("Connected");
        }
    };
    
    private Thread reconnectionThread;
    
    private Runnable responseMessagesProcessorRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("Response worker thread - message processing");
            
            if(!responseDataToPublish.isEmpty()) {
                Map<String, ResponseData> responseDataMap = responseDataToPublish.poll();
                processResponseData(responseDataMap);
            } else {
                System.out.println("Response worker thread - message buffer is empty");
            }
        }
    };
    
    private Thread responseMessagesThread;
    
    // response messages to publish
    private Queue<Map<String, ResponseData>> responseDataToPublish = new ConcurrentLinkedQueue<>();

    private void processResponseData(Map<String, ResponseData> responseDataMap) {

        Set<String> topics = responseDataMap.keySet();
        String topic = (String) topics.toArray()[0];

        // converting DPA result into web response form
        String webResponse = MqttFormatter.formatResponseData(responseDataMap.get(topic));

        System.out.println("Web topic: " + topic);
        System.out.println("Web response: " + webResponse);

        try {
            publish(topic, 0, webResponse.getBytes());
        } catch (MqttException ex) {
            System.err.println("Error while publishing web response message: " + ex);
        }
    }
    
    private static final Logger log = LoggerFactory.getLogger(MqttCommunicator.class);
    
    // sets connection options
    private void setConnectionOptions(
            boolean isCleanSession, String password, String userName, String certFile
    ) throws CertificateException, IOException, KeyStoreException, 
            NoSuchAlgorithmException, KeyManagementException 
    {
        conOpt.setCleanSession(isCleanSession);
            
        if ( !password.isEmpty() ) {
            conOpt.setPassword(password.toCharArray());
        }
        if ( !userName.isEmpty() ) {
            conOpt.setUserName(userName);
        }

        if ( !certFile.isEmpty() ) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            InputStream certFileInputStream = fullStream(certFile);
            Certificate ca = cf.generateCertificate(certFileInputStream);

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("ca", ca);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLSv1");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            conOpt.setSocketFactory(sslContext.getSocketFactory());
        }
    }
    
    /**
     * Constructs an instance of the sample client wrapper
     *
     * @param MQTTConfig the configuration params of the server to connect to
     * @throws MqttException
     */
    public MqttCommunicator(MqttConfiguration mqttConfig) throws MqttException {
        
        String brokerUrl = mqttConfig.getProtocol() + mqttConfig.getBroker() + ":" + mqttConfig.getPort();
        
        this.brokerUrl = brokerUrl;
        this.quietMode = mqttConfig.isQuiteMode();
        this.clean = mqttConfig.isCleanSession();
        this.certFile = mqttConfig.getCertFilePath();
        this.userName = mqttConfig.getUsername();
        this.password = mqttConfig.getPassword();
        
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
            setConnectionOptions(clean, password, userName, certFile);

            // Construct an MQTT blocking mode client
            client = new MqttClient(this.brokerUrl, mqttConfig.getClientId(), dataStore);

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
        } catch (CertificateException e) {
            e.printStackTrace();
            log("Unable to set up client - certificate exception: " + e.toString());
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            log("Unable to set up client - certificate exception in input stream: " + e.toString());
            System.exit(1);
        } catch (KeyStoreException e) {
            e.printStackTrace();
            log("Unable to set up client - certificate exception in key store: " + e.toString());
            System.exit(1);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log("Unable to set up client - certificate exception in loading key store: " + e.toString());
            System.exit(1);
        } catch (KeyManagementException e) {
            e.printStackTrace();
            log("Unable to set up client - certificate exception in ssl context: " + e.toString());
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
    public synchronized void publish(String topicName, int qos, byte[] payload) throws MqttException {

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
     * Subscribe to a topic on an MQTT server. Once subscribed this method waits
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
        log.debug("connectionLost - start: cause=" + cause.getMessage());
        
        // Called when the connection to the server has been lost.
        // An application may choose to implement reconnection
        // logic at this point. This sample simply exits.
        log("Connection to " + brokerUrl + " lost! " + cause);
        
        reconnectionThread = new Thread(reconnectionRunnable);
        reconnectionThread.start();
        
        log.debug("connectionLost - end");
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
        
        // message data
        final String messageData = new String(message.getPayload());
        
        DPA_Request dpaRequest = null;
        DPA_Result result = null;
        try {
            dpaRequest = WebRequestParser.parse( messageData );
            
            if(dpaRequest.getDpa().equalsIgnoreCase("req")) {
                result = OpenGatewayAppStd.sendWebRequestToDpaNetwork(dpaRequest, topic);
            }
        } catch ( WebRequestParserException ex ) {
            System.err.println("Error while parsing web request: " + ex);
            return;
        } 
        
        // no result - usually in the case of error
        if ( result == null ) {
            System.err.println("Null result from DPA.");
            return;
        }
        
        // holding topic and response data
        Map<String, ResponseData> responseDataMap = new LinkedHashMap<>();
        
        // creating data of response to publish
        ResponseData responseData = createResponseData(dpaRequest, result);
        
        // adding topic and response data
        responseDataMap.put(topic, responseData);
        
        // buffer and notify
        responseDataToPublish.add(responseDataMap);
        
        // process it
        responseMessagesThread = new Thread(responseMessagesProcessorRunnable);
        responseMessagesThread.start();
    }
    
    // creates response data for publishing
    private ResponseData createResponseData(DPA_Request request, DPA_Result result) {
        return new ResponseData(
                request.getN(), 
                request.getSv(),
                String.valueOf(request.getPid()), 
                result.getRequest().getNadr(), 
                String.valueOf(result.getRequest().getPnum()),
                result.getRequest().getPcmd(),
                String.valueOf(result.getDpaAddInfo().getHwProfile()),
                String.valueOf(result.getDpaAddInfo().getResponseCode()),
                String.valueOf(result.getDpaAddInfo().getDPA_Value()),
                result.getRequest().getModuleId()
        );
    }
    
    /**
     * <p>Creates an InputStream from a file, and fills it with the complete
     * file. Thus, available() on the returned InputStream will return the
     * full number of bytes the file contains</p>
     * @param fname The filename
     * @return The filled InputStream
     * @exception IOException, if the Streams couldn't be created.
     **/
    private InputStream fullStream ( String fname ) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(fname);
        //FileInputStream fis = new FileInputStream(fname);
        
        DataInputStream dis = new DataInputStream(is);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }
}
