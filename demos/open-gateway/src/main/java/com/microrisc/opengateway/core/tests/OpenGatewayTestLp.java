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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.asynchrony.AsynchronousMessagesListener;
import com.microrisc.simply.asynchrony.AsynchronousMessagingManager;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessageProperties;
import com.microrisc.simply.iqrf.dpa.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Custom;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import java.io.File;
import java.text.DecimalFormat;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttException;


/**
 * Running first tests with DPA <-> MQTT.
 * 
 * @author Rostislav Spinar
 */
public class OpenGatewayTestLp implements AsynchronousMessagesListener<DPA_AsynchronousMessage> 
{
    // references for DPA
    public static DPA_Simply simply = null;
    public static Network network1 = null;
    
    public static Node node1 = null;
    public static Node node2 = null;
    public static Node node3 = null;
    
    public static OsInfo osInfoNode1 = null;
    public static OsInfo osInfoNode2 = null;
    public static OsInfo osInfoNode3 = null;
    
    public static boolean asyncRequestReceived = false;
    public static String asyncNodeId = null;
    public static int asyncPeripheralNumber = 0;
    public static short[] asyncMainData = null;
    public static DPA_AdditionalInfo asyncAdditionalData = null;
    
    // references for MQTT
    public static String protocol = "tcp://";
    public static String broker = "localhost";
    public static int port = 1883;
    
    public static String clientId = "b827eb26c73d-lp";
    public static boolean cleanSession = true;
    public static boolean quietMode = false;
    public static boolean ssl = false;
    
    public static String password = null;
    public static String userName = null;
    
    public static MQTTCommunicator mqttCommunicator = null;
    public static String webResponseToBeSent = null;
    public static boolean webRequestReceived = false;
    
    public static int pid = 0;
    public static int pidAsyncCitiq = 0;
    
    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message, boolean exit) {
        System.out.println(message);
        
        if(exit) {
            if ( simply != null ) {
                simply.destroy();
            }
            System.exit(1);
        }
    } 
    
    public static void main(String[] args) throws InterruptedException, MqttException {
        
        // DPA INIT
        
        try {
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "cdc" + File.separator + "Simply.properties");
        } catch ( SimplyException ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage(), true);
        }
        
        // MQTT INIT
        
        String url = protocol + broker + ":" + port;
        mqttCommunicator = new MQTTCommunicator(url, clientId, cleanSession, quietMode, userName, password);
        
        // ASYNC REQUESTS FROM DPA
        
        OpenGatewayTestLp msgListener = new OpenGatewayTestLp();
        
        // getting access to asynchronous messaging manager
        AsynchronousMessagingManager<DPA_AsynchronousMessage, DPA_AsynchronousMessageProperties> asyncManager 
                = simply.getAsynchronousMessagingManager();
        
        // register the listener of asynchronous messages
        asyncManager.registerAsyncMsgListener(msgListener);
        
        // SYNC REQUESTS TO DPA
        
        // getting network 1
        network1 = simply.getNetwork("1", Network.class);
        if ( network1 == null ) {
            printMessageAndExit("Network 1 doesn't exist", true);
        }
        
        // getting node 1
        node1 = network1.getNode("1");
        if ( node1 == null ) {
            printMessageAndExit("Node 1 doesn't exist", true);
        }
        
        // getting node 2
        node2 = network1.getNode("2");
        if (node2 == null) {
            printMessageAndExit("Node 2 doesn't exist", true);
        }

        // getting node 3
        node3 = network1.getNode("3");
        if (node3 == null) {
            printMessageAndExit("Node 3 doesn't exist", true);
        }
        
        // getting OS interface
        OS osn1 = node1.getDeviceObject(OS.class);
        if ( osn1 == null ) {
            printMessageAndExit("OS doesn't exist on node 1", false);
        }
         
        // get info about module
        osInfoNode1 = osn1.read();
        if (osInfoNode1 == null) {
            CallRequestProcessingState procState = osn1.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = osn1.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 1: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 1: " + procState, false);
            }
        }
        
        // getting OS interface
        OS osn2 = node2.getDeviceObject(OS.class);
        if ( osn2 == null ) {
            printMessageAndExit("OS doesn't exist on node 2", false);
        }
         
        // get info about module
        osInfoNode2 = osn2.read();
        if (osInfoNode2 == null) {
            CallRequestProcessingState procState = osn2.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = osn2.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 2: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 2: " + procState, false);
            }
        }
        
        // getting OS interface
        OS osn3 = node3.getDeviceObject(OS.class);
        if ( osn3 == null ) {
            printMessageAndExit("OS doesn't exist on node 3", false);
        }
         
        // get info about module
        osInfoNode3 = osn3.read();
        if (osInfoNode3 == null) {
            CallRequestProcessingState procState = osn3.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = osn3.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 3: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 3: " + procState, false);
            }
        }
        
        // getting custom interface
        Custom custom = node1.getDeviceObject(Custom.class);
        if ( custom == null ) {
            printMessageAndExit("Custom doesn't exist on node 1", true);
        }
        
        // getting results
        // set up maximal number of cycles according to your needs - only testing
        final int MAX_CYCLES = 5000;        
        for ( int cycle = 0; cycle < MAX_CYCLES; cycle++ ) {
        
            // getting actual temperature
            short peripheralIQHome = 0x20;
            short cmdIdTemp = 0x10;
            short cmdIdHum = 0x11;
            short[] data = new short[]{};
            
            Short[] receivedDataTemp = null;
            Short[] receivedDataHum = null;
            
            UUID tempRequestUid = custom.async_send(peripheralIQHome, cmdIdTemp, data);
            Thread.sleep(500);
            UUID humRequestUid = custom.async_send(peripheralIQHome, cmdIdHum, data);
            Thread.sleep(500);

            // maximal number of attempts of getting a result
            final int RETRIES = 3;
            int attempt = 0;
            int checkResponse = 0;
            
            while (attempt++ < RETRIES) {
                
                // main job is here for now - quick hack to test
                while (true) { 
                    
                    Thread.sleep(10);
                    checkResponse++;
                    
                    // dpa async task
                    if(asyncRequestReceived) {
                        asyncRequestReceived = false;
                        sendDPAAsyncRequest();
                    }
                    
                    // mqtt web confirmation task
                    if(webRequestReceived) {
                        webRequestReceived = false;
                        /*
                            try {
                                mqttCommunicator.publish(MQTTTopics.ACTUATORS_LEDS_LP, 2, webResponseToBeSent.getBytes());
                            } catch (MqttException ex) {
                                System.err.println("Error while publishing sync dpa message.");
                            }
                        */
                    }
                    
                    // periodic task ever 60s
                    if(checkResponse == 6000) {
                        checkResponse = 0;
                        break;
                    }
                }
                
                 // get request call state
                CallRequestProcessingState procStateTemp = custom.getCallRequestProcessingState(tempRequestUid);
                CallRequestProcessingState procStateHum = custom.getCallRequestProcessingState(humRequestUid);

                // if any error occured
                if (procStateTemp == CallRequestProcessingState.ERROR  ) {

                    // general call error
                    CallRequestProcessingError error = custom.getCallRequestProcessingError(tempRequestUid);
                    printMessageAndExit("Getting custom temperature failed on node 1: " + error.getErrorType(), false);
                    
                } else {
                    // have result iqhome
                    if (procStateTemp == CallRequestProcessingState.RESULT_ARRIVED) {
                        receivedDataTemp = custom.getCallResultImmediately(tempRequestUid, Short[].class);
                        
                        if (receivedDataTemp != null && receivedDataTemp.length == 0) {

                            // specific call error
                            DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                            DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                            printMessageAndExit("Getting custom temperature failed on node 1, DPA error: " + dpaResponseCode, false);
                        } 
                        else {
                            System.out.println("Getting custom temperature hasn't been processed yet on node 1: " + procStateTemp);
                        }
                    }
                }
                
                // if any error occured
                if (procStateHum == CallRequestProcessingState.ERROR  ) {

                    // general call error
                    CallRequestProcessingError error = custom.getCallRequestProcessingError(humRequestUid);
                    printMessageAndExit("Getting custom humidity failed on node 1: " + error.getErrorType(), false);

                } else {
                    
                    if (procStateHum == CallRequestProcessingState.RESULT_ARRIVED ) {
                        receivedDataHum = custom.getCallResultImmediately(humRequestUid, Short[].class);
                        
                        if (receivedDataTemp != null && receivedDataTemp.length == 0) {

                            // specific call error
                            DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                            DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                            printMessageAndExit("Getting custom humidity failed on node 1, DPA error: " + dpaResponseCode, false);    
                        } 
                        else {
                            System.out.println("Getting custom humidity hasn't been processed yet on node 1: " + procStateHum);
                        }
                    }
                }
                break;
            }

            if (receivedDataTemp != null && receivedDataHum != null) {
                
                if (receivedDataTemp.length == 0 && receivedDataHum.length == 0) {
                    System.out.print("No received data from custom on the node 1 " + node1.getId());
                } else {
                    
                    pid++;

                    System.out.print("Received temperature from custom on the node " + node1.getId() + ": ");
                    for (Short readResultLoop : receivedDataTemp) {
                        System.out.print(Integer.toHexString(readResultLoop).toUpperCase() + " ");
                    }
                    System.out.println();

                    float temperature = (receivedDataTemp[1] << 8) + receivedDataTemp[0];
                    temperature = temperature / 16;

                    System.out.print("Received humidity from custom on the node " + node1.getId() + ": ");
                    for (Short readResultLoop : receivedDataHum) {
                        System.out.print(Integer.toHexString(readResultLoop).toUpperCase() + " ");
                    }
                    System.out.println();

                    float humidity = (receivedDataHum[1] << 8) + receivedDataHum[0];
                    humidity = Math.round(humidity / 16);

                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                    //DPA_AdditionalInfo dpaAddInfoTemp = (DPA_AdditionalInfo)custom.getCallResultAdditionalInfo(tempRequestUid);
                    //DPA_AdditionalInfo dpaAddInfoHum = (DPA_AdditionalInfo)custom.getCallResultAdditionalInfo(humRequestUid);

                    DecimalFormat df = new DecimalFormat("###.##");

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String iqhomeValuesToBeSent =
                              "{\"e\":["
                            + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + df.format(temperature) + "},"
                            + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "}"
                            + "],"
                            + "\"iqrf\":["
                            + "{\"pid\":" + pid + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node1.getId() + "," 
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\"," 
                            + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\"," 
                            + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                            + "],"
                            + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode1.getPrettyFormatedModuleId() + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.LP_SENSORS_IQHOME, 2, iqhomeValuesToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }

                    receivedDataTemp = null;
                    receivedDataHum = null;
                }
            } else {
                System.out.println("IQHome Result has not arrived.");
            }
        }
        
        // after end of work with asynchronous messages, unrergister the listener
        asyncManager.unregisterAsyncMsgListener(msgListener);
        
        // end working with Simply
        simply.destroy();
    }
    
    public static String sendDPAWebRequest(String topic, String msgSenML) {
        
        String valueN = null;
        String valueSV = null;
        
        int valuePID = 0;
        String valueDPA = null;
        int valueNADR = 0;
        
        // parse senml json msg request       
        JsonArray elements = Json.parse(msgSenML).asObject().get("e").asArray();
        
        for (JsonValue element : elements) {
            valueN  = element.asObject().getString("n", "");
            valueSV  = element.asObject().getString("sv", "");
            //System.out.println("Published action on e element: " + "n:" + valueN + " - " + "sv:" + valueSV);
        }
        
        // parse senml json msg request       
        elements = Json.parse(msgSenML).asObject().get("iqrf").asArray();

        for (JsonValue element : elements) {
            valuePID  = element.asObject().getInt("pid", 0);
            valueDPA = element.asObject().getString("dpa", "");
            valueNADR  = element.asObject().getInt("nadr", 0);
            //System.out.println("Published action on iqrf element: " + "pid:" + valuePID + " - " + "dpa:" + valueDPA + " - " + "nadr:" + valueNADR);
        }
        
        // there is a need to select topic
        /*
        if (MQTTTopics.ACTUATORS_LEDS_LP.equals(topic)) {
        
            // TODO: check nodeID and add selection
            if(valueDPA.equalsIgnoreCase("REQ")) {

                if(valueN.equalsIgnoreCase("LEDR")) {

                    LEDR ledr = node1.getDeviceObject(LEDR.class);
                    if (ledr == null) {
                        printMessageAndExit("LEDR doesn't exist or is not enabled", false);
                        return null;
                    }

                    if(valueSV.equalsIgnoreCase("PULSE")) {

                        VoidType setResult = ledr.pulse();
                        if (setResult == null) {
                            processNullResult(ledr, "Pulsing LEDR failed",
                                    "Pulsing LEDR hasn't been processed yet"
                            );
                            return null;
                        }

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = ledr.getDPA_AdditionalInfoOfLastCall();

                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        webResponseToBeSent
                                = "{\"e\":[{\"n\":\"ledr\"," + "\"sv\":" + "\"" + LEDR.MethodID.PULSE.name().toLowerCase() + "\"}],"
                                + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node1.getId() + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.LEDR + "," + "\"pcmd\":" + "\"" + LEDR.MethodID.PULSE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\"," 
                                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + osInfo.getPrettyFormatedModuleId() + "\""
                                + "}";

                        webRequestReceived = true;
                        return webResponseToBeSent;
                    }
                }
            }
        }
        */        
        
        return null;
    }
    
    public static boolean sendDPAAsyncRequest() throws InterruptedException {
        
        if(asyncNodeId != null && asyncMainData != null && asyncAdditionalData != null) {

            if(asyncMainData.length == 0) {
                System.out.print("No received data from Asynchronny on the node " + asyncNodeId);
            }
            else {
                if (asyncNodeId.equals("2")) {

                    pidAsyncCitiq++;
                    String STATE = ""; 

                    if(asyncMainData[0] == 0)
                        STATE = "free";
                    else if (asyncMainData[0] == 1)
                        STATE = "occupied";

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String asyncRequestToBeSent 
                            = "{\"e\":[{\"n\":\"carplace\"," + "\"sv\":" + "\"" + STATE + "\"}],"
                            + "\"iqrf\":[{\"pid\":" + pidAsyncCitiq + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + asyncAdditionalData.getHwProfile() + "," + "\"rcode\":" + "\"" + asyncAdditionalData.getResponseCode().name().toLowerCase() + "\"," 
                            + "\"dpavalue\":" + asyncAdditionalData.getDPA_Value() + "}],"
                            + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode2.getPrettyFormatedModuleId() + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.LP_STATUS_CITIQ, 2, asyncRequestToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message from node 2.");
                    }
                }
            
                if(asyncNodeId.equals("3")) {

                    pidAsyncCitiq++;
                    String STATE = "";

                    if(asyncMainData[0] == 0)
                        STATE = "free";
                    else if (asyncMainData[0] == 1)
                        STATE = "occupied";

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String asyncRequestToBeSent 
                            = "{\"e\":[{\"n\":\"carplace\"," + "\"sv\":" + "\"" + STATE + "\"}],"
                            + "\"iqrf\":[{\"pid\":" + pidAsyncCitiq + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + asyncAdditionalData.getHwProfile() + "," + "\"rcode\":" + "\"" + asyncAdditionalData.getResponseCode().name().toLowerCase() + "\"," 
                            + "\"dpavalue\":" + asyncAdditionalData.getDPA_Value() + "}],"
                            + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode3.getPrettyFormatedModuleId() + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.LP_STATUS_CITIQ, 2, asyncRequestToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message from node 3.");
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void onAsynchronousMessage(DPA_AsynchronousMessage message) {
        
        System.out.println("New asynchronous message.");
        
        asyncNodeId = message.getMessageSource().getNodeId();
        asyncPeripheralNumber = message.getMessageSource().getPeripheralNumber();
        asyncMainData = (short[])message.getMainData();        
        asyncAdditionalData = (DPA_AdditionalInfo)message.getAdditionalData();
        
        // sending control message back to network based on received message
        asyncRequestReceived = true;
    }
}
