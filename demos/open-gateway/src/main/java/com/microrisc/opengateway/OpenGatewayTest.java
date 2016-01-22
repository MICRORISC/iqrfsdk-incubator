/* 
 * Copyright 2014 MICRORISC s.r.o.
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

package com.microrisc.opengateway;

import com.microrisc.opengateway.mqtt.MQTTController;
import com.microrisc.simply.CallRequestProcessingState;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.asynchrony.AsynchronousMessagesListener;
import com.microrisc.simply.asynchrony.AsynchronousMessagingManager;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessageProperties;
import com.microrisc.simply.iqrf.dpa.v22x.devices.LEDR;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Thermometer;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.Thermometer_values;
import com.microrisc.simply.iqrf.types.VoidType;
import java.io.File;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONObject;

/**
 * Example of using asynchronous messaging.
 * Example has been tested with CustomDpaHandler-Coordinator-PullNodes.hex.
 * 
 * @author Michal Konopa, Rostislav Spinar
 */
public class OpenGatewayTest 
implements AsynchronousMessagesListener<DPA_AsynchronousMessage> 
{
    // references for DPA
    public static DPA_Simply simply = null;
    public static Network network1 = null;
    public static Node node1 = null;
    
    // references for MQTT
    public static String protocol = "tcp://";
    public static String broker = "localhost";
    public static int port = 1883;
    
    public static String clientId = "open-gateway";
    public static String subTopic = "data/in";
    public static String pubTopic = "data/out";
    
    public static boolean cleanSession = true;
    public static boolean quietMode = false;
    public static boolean ssl = false;
    
    public static String password = null;
    public static String userName = null;
    
    public static MQTTController mqttController = null;

    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        if ( simply != null ) {
            simply.destroy();
        }
        System.exit(1);
    } 
    
    public static void main(String[] args) throws InterruptedException, MqttException {
        
        // DPA INIT
        
        try {
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "Simply.properties");
        } catch ( SimplyException ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }
        
        // MQTT INIT
        
        String url = protocol + broker + ":" + port;        
        mqttController = new MQTTController(url, clientId, cleanSession, quietMode, userName, password);        
        mqttController.subscribe(subTopic, 2);
        
        // ASYNC REQUESTS FROM DPA
        
        OpenGatewayTest msgListener = new OpenGatewayTest();
        
        // getting access to asynchronous messaging manager
        AsynchronousMessagingManager<DPA_AsynchronousMessage, DPA_AsynchronousMessageProperties> asyncManager 
                = simply.getAsynchronousMessagingManager();
        
        // register the listener of asynchronous messages
        asyncManager.registerAsyncMsgListener(msgListener);
        
        // SYNC REQUESTS To DPA
        
        // getting network 1
        network1 = simply.getNetwork("1", Network.class);
        if ( network1 == null ) {
            printMessageAndExit("Network 1 doesn't exist");
        }
        
        // getting node 1
        node1 = network1.getNode("1");
        if ( node1 == null ) {
            printMessageAndExit("Node 1 doesn't exist");
        }
        
        // getting Thermometer interface
        Thermometer thermo = node1.getDeviceObject(Thermometer.class);
        if ( thermo == null ) {
            printMessageAndExit("Thermometer doesn't exist on node 1");
        }
        
        // getting results
        // set up maximal number of cycles according to your needs
        final int MAX_CYCLES = 5000;        
        for ( int cycle = 0; cycle < MAX_CYCLES; cycle++ ) {
        
            // getting actual temperature
            Thermometer_values thermoValues = null;
            UUID tempRequestUid = thermo.async_get();

            // maximal number of attempts of getting a result
            final int MAX_RESULT_GETTING = 10;
            int attempt = 0;
            while (attempt++ < MAX_RESULT_GETTING) {

                CallRequestProcessingState procState = thermo.getCallRequestProcessingState(
                        tempRequestUid
                );

                if (procState == CallRequestProcessingState.RESULT_ARRIVED) {
                    thermoValues = thermo.getCallResultImmediately(tempRequestUid, Thermometer_values.class);
                    //result = thermo.getCallResultInDefaultWaitingTimeout(getStateRequestUid, LED_State.class);
                    break;
                }

                if (procState == CallRequestProcessingState.ERROR) {

                    // general call error
                    CallRequestProcessingError error = thermo.getCallRequestProcessingErrorOfLastCall();

                    // specific call error
                    //DPA_AdditionalInfo dpaAddInfo = thermo.getDPA_AdditionalInfoOfLastCall();
                    //DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();

                    //printMessageAndExit("Getting temperature failed: " + error + ", DPA error: " + dpaResponseCode);
                    printMessageAndExit("Getting temperature failed: " + error);
                } else {
                    System.out.println("Getting temperature hasn't been processed yet: " + procState);
                    System.out.println();
                }

                Thread.sleep(30000);
            }

            if (thermoValues != null) {
                
                // printing results        
                System.out.println("Temperature on the node " + node1.getId() + ": "
                        + thermoValues.getValue() + "." + thermoValues.getFractialValue() + " *C"
                );
                
                String msqMqtt = thermoValues.toPrettyFormattedString();
                
                // send data to mqtt
                try {
                    mqttController.publish(pubTopic, 2, msqMqtt.getBytes());
                } catch (MqttException ex) {
                    System.err.println("Error while publishing sync dpa message.");
                }
                
            } else {
                System.out.println("Result has not arrived.");
            }
        }
        
        // after end of work with asynchronous messages, unrergister the listener
        asyncManager.unregisterAsyncMsgListener(msgListener);
        
        // end working with Simply
        simply.destroy();
    }
    
    public static boolean sendDPARequest(JSONObject JSONRequest) {
        
        // TODO: from JSONRequest to DPARequest       
        
        // getting LEDR interface
        LEDR ledr = node1.getDeviceObject(LEDR.class);
        if (ledr == null) {
            printMessageAndExit("LEDG doesn't exist or is not enabled");
        }
        
        // pulsing of LEDR
        VoidType setResult = ledr.pulse();
        if ( setResult == null ) {
            processNullResult(ledr, "Pulsing LEDR failed", 
                                    "Pulsing LEDR hasn't been processed yet"
            );
            return false;
        }
        
        return true;
    }
    
    // processes NULL result
    private static void processNullResult(LEDR ledr, String errorMsg, String notProcMsg) 
    {
        CallRequestProcessingState procState = ledr.getCallRequestProcessingStateOfLastCall();
        if ( procState == CallRequestProcessingState.ERROR ) {
            CallRequestProcessingError error = ledr.getCallRequestProcessingErrorOfLastCall();
            printMessageAndExit(errorMsg + ": " + error);
        } else {
            printMessageAndExit(notProcMsg + ": " + procState);
        }
    }

    @Override
    public void onAsynchronousMessage(DPA_AsynchronousMessage message) {
        System.out.println("New asynchronous message: ");
        
        System.out.println("Message source: "
            + "network ID= " + message.getMessageSource().getNetworkId()
            + ", node ID= " + message.getMessageSource().getNodeId()
            + ", peripheral number= " + message.getMessageSource().getPeripheralNumber()
        );
        
        System.out.println("Main data: " + message.getMainData());
        System.out.println("Additional data: " + message.getAdditionalData());
        System.out.println();
        
        // getting specific type once we know what message comes
        //OsInfo osi = (OsInfo)message.getMainData();
        //System.out.println("Pretty format: " + osi.toPrettyFormatedString());
        //System.out.println();
        
        //Gson gson = new GsonBuilder().create();
        
        StringBuilder strBuilder = new StringBuilder();
        String NEW_LINE = System.getProperty("line.separator");
        
        strBuilder.append("{" + NEW_LINE);
        strBuilder.append(" Network ID: " + message.getMessageSource().getNetworkId() + NEW_LINE);
        strBuilder.append(" Node ID: " + message.getMessageSource().getNodeId() + NEW_LINE);
        strBuilder.append(
                " Device Interface ID: " 
                + message.getMessageSource().getPeripheralNumber() + NEW_LINE
        );
        strBuilder.append(" Main DATA: " + message.getMainData() + NEW_LINE);
        strBuilder.append(" Additional DATA: " + message.getAdditionalData() + NEW_LINE);
        
        strBuilder.append("}");
        
        String msqMqtt = strBuilder.toString();
        
        try {
            mqttController.publish(pubTopic, 2, msqMqtt.getBytes());
        } catch (MqttException ex) {
            System.err.println("Error while publishing async dpa message.");
        }
    }
}
