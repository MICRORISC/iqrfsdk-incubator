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

import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.errors.CallRequestProcessingErrorType;
import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Thermometer;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.Thermometer_values;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Running first tests with DPA <-> MQTT.
 *
 * @author Rostislav Spinar
 */
public class OpenGatewayTcpCloudMicrorisc {

    // references for DPA
    public static DPA_Simply DPASimply = null;
    public static Network DPANetwork = null;
    public static Map<String, Node> DPANodes = null;
    public static Map<String, OsInfo> DPAOSInfo = new LinkedHashMap<>();
    public static Map<String, Thermometer> DPAThermometers = new LinkedHashMap<>();
    public static Map<String, UUID> DPAThermometerUUIDs = new LinkedHashMap<>();
    public static Map<String, Thermometer_values> DPADataOut = new LinkedHashMap<>();
    public static Map<String, String> DPAParsedDataOut = new LinkedHashMap<>();
    public static final int READING = 120; 
    public static String moduleId = null;

    // references for MQTT
    public static MQTTCommunicator mqttCommunicator = null;
    public static String protocol = "tcp://";
    public static String broker = "localhost";
    public static int port = 1883;
    public static String clientId = "macid-std";
    public static boolean cleanSession = true;
    public static boolean quietMode = false;
    public static boolean ssl = false;
    public static String certFile = null;
    public static String password = null;
    public static String userName = null;

    // references for APP
    public static int pidMicrorisc = 0;

    public static void main(String[] args) throws InterruptedException, MqttException {
        
        // DPA INIT
        String configFile = "Simply.properties";
        DPASimply = getDPASimply(configFile);
        
        // MQTT INIT
        String url = protocol + broker + ":" + port;
        mqttCommunicator = new MQTTCommunicator(url, clientId, cleanSession, quietMode, userName, password, certFile);
        
        // APP EXIT HOOK
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("End via shutdown hook.");

                // end working with Simply
                DPASimply.destroy();
            }

        }));

        // REF TO DPA NET
        String netId = "1";
        DPANetwork = getDPANetwork(netId);
        
        // REF TO ALL NODES
        DPANodes = getDPANodes();
        int numberOfBondedNodes = DPANetwork.getNodesMap().size() - 1;
        
        // REF TO NODES OS-INFO FOR GETTING MODULE IDs
        DPAOSInfo = getOsInfoFromNodes();
        
        // MIDs
        for (Map.Entry<String, OsInfo> entry : DPAOSInfo.entrySet()) {
            System.out.println("Node: " + entry.getKey() + " MID: " + entry.getValue().getPrettyFormatedModuleId() );
        }
        
        // REF TO THERMOMETER ON NODES
        DPAThermometers = getThermometerOnNodes();
        
        int checkResponse = 0;
        
        // SENDING AND RECEIVING
        while( true ) {
            
            DPAThermometerUUIDs = sendDPARequests();
            
            // RECEIVING AND ACTING ON ASYNC AND WEB REQUESTS
            while (true) {

                Thread.sleep(1);
                checkResponse++;

                // dpa async task - not used for protronix
                //if (asyncRequestReceived) {
                //    asyncRequestReceived = false;
                //}

                // mqtt web confirmation task - not used for protronix
                //if (webRequestReceived) {
                //    webRequestReceived = false;
                //}

                // periodic task to read protronix every 5s - main
                if (checkResponse == READING * 1000) {
                    checkResponse = 0;
                    break;
                }
            }
            
            // GET RESPONSE DATA 
            DPADataOut = collectDPAResponses();

            // PARSE RESPONSE DATA
            DPAParsedDataOut = parseDPAResponses();
            
            // SEND DATA
            MQTTSendData();
        }
    }
    
    // init dpa simply
    public static DPA_Simply getDPASimply(String configFile) {
        
        DPA_Simply DPASimply = null;
        
        try {
            DPASimply = DPA_SimplyFactory.getSimply("config" + File.separator + "cdc" + File.separator + configFile);
        } catch (SimplyException ex) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage(), true);
        }
        
        return DPASimply;
    }
    
    // reference to dpa network
    public static Network getDPANetwork(String netId) {
        
        Network DPANetwork = null;
        
        DPANetwork = DPASimply.getNetwork(netId, Network.class);
        if (DPANetwork == null) {
            printMessageAndExit("DPA Network doesn't exist", true);
        }
        
        return DPANetwork;
    }
    
    // reference to all nodes in the network
    public static Map<String, Node> getDPANodes() {
        
        Map<String, Node> DPANodes = null;
        
        DPANodes = DPANetwork.getNodesMap();
        
        return DPANodes;
    }
    
    // reference to os info of all nodes
    public static Map<String, OsInfo> getOsInfoFromNodes() {
        
        Map<String, OsInfo> DPAOSInfo = new LinkedHashMap<>();
        
        for (Map.Entry<String, Node> entry : DPANodes.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());
            
            // node 1-40
            if(0 < key && 40 >= key) {
                
                System.out.println("Getting OsInfo on the node: " + entry.getKey());

                // OS peripheral
                OS os = entry.getValue().getDeviceObject(OS.class);

                if (os == null) {
                    printMessageAndExit("OS doesn't exist on node", false);
                }
                else {
                    // get info about module
                    OsInfo osInfo = os.read();

                    if (osInfo == null) {
                        CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
                        if (procState == ERROR) {
                            CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Getting OS info failed on node: " + error, false);
                        } else {
                            printMessageAndExit("Getting OS info hasn't been processed yet on node: " + procState, false);
                        }
                    } 
                    else {
                        DPAOSInfo.put(entry.getKey(), osInfo);
                    }
                }
            }
        }
        
        return DPAOSInfo;
    }
    
    // reference to thermometer peripheral on all nodes
    public static Map<String, Thermometer> getThermometerOnNodes() {
        
        Map<String, Thermometer> DPAThermometers = new LinkedHashMap<>();
        
        for (Map.Entry<String, Node> entry : DPANodes.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-40
            if(0 < key && 40 >= key) {

                System.out.println("Getting Thermometer on the node: " + entry.getKey());
                
                // Thermometer peripheral
                Thermometer thermometer = entry.getValue().getDeviceObject(Thermometer.class);

                if (thermometer == null) {
                    printMessageAndExit("Thermometer doesn't exist on node", true);
                } 
                else {
                    int defaultHWPID = 0xFFFF;
                    thermometer.setRequestHwProfile(defaultHWPID);

                    DPAThermometers.put(entry.getKey(), thermometer);
                }
            }
        }
        
        return DPAThermometers;
    }
    
    // sends all requests and do not wait for response
    public static Map<String, UUID> sendDPARequests() {

        Map<String, UUID> DPAThermometerUUIDs = new LinkedHashMap<>();
        UUID uuidThermometer = null;

        // SEND DPA REQUESTS
        for (Map.Entry<String, Thermometer> entry : DPAThermometers.entrySet()) {

            int key = Integer.parseInt(entry.getKey());

            // node 1-40
            if(0 < key && 40 >= key) {

                System.out.println("Issuing req for node: " + entry.getKey());
                
                if (null != entry.getValue()) {

                    // ASYNC DPA CALL - NO BLOCKING
                    uuidThermometer = entry.getValue().async_get();
                    DPAThermometerUUIDs.put(entry.getKey(), uuidThermometer);
                }
            }
        }
        
        return DPAThermometerUUIDs;
    }
    
    // collect dpa responses 
    public static Map<String, Thermometer_values> collectDPAResponses() {

        Map<String, Thermometer_values> DPADataOut = new LinkedHashMap<>();
        CallRequestProcessingState procState = null;
        Thermometer_values dataOut = null;
        
        // CHECK STATE AND COLLECT 
        for (Map.Entry<String, Thermometer> entry : DPAThermometers.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-40
            if(0 < key && 40 >= key) {

                System.out.println("Collecting resp for node: " + entry.getKey());

                if (null != entry.getValue()) {
                    procState = entry.getValue().getCallRequestProcessingState(DPAThermometerUUIDs.get(entry.getKey()));

                    if (null != procState) {
                        // if any error occured
                        if (procState == CallRequestProcessingState.ERROR) {

                            // general call error
                            CallRequestProcessingError error = entry.getValue().getCallRequestProcessingError(DPAThermometerUUIDs.get(entry.getKey()));
                            printMessageAndExit("Getting UART data failed on the node: " + error.getErrorType(), false);

                            DPADataOut.put(entry.getKey(), null);

                        } else {

                            // have result already
                            if (procState == CallRequestProcessingState.RESULT_ARRIVED) {

                                dataOut = entry.getValue().getCallResultImmediately(DPAThermometerUUIDs.get(entry.getKey()), Thermometer_values.class);                                
                                
                                if (null != dataOut) {
                                    DPADataOut.put(entry.getKey(), dataOut);
                                }                            
                            } 
                            else if ( procState == CallRequestProcessingState.ERROR ) {
                
                                // general call error
                                CallRequestProcessingError error = entry.getValue().getCallRequestProcessingErrorOfLastCall();
                
                                    if(error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL){
                            
                                        // specific call error
                                        DPA_AdditionalInfo dpaAddInfo = entry.getValue().getDPA_AdditionalInfoOfLastCall();
                                        DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                        printMessageAndExit("Getting Thermometer data failed on the node, DPA error: " + dpaResponseCode, false);

                                        DPADataOut.put(entry.getKey(), null);
                                    }
                            } else {
                                System.out.println("Getting Thermometer data on node hasn't been processed yet: " + procState);
                                DPADataOut.put(entry.getKey(), null);
                            }
                        }
                    }
                    else {
                        System.out.println("Could not get call request processing info from connector");
                    }
                }
            }
        }
        
        return DPADataOut;
    }
    
    // data parsing
    public static Map<String, String> parseDPAResponses() {
        
        Map<String, String> DPAParsedDataOut = new LinkedHashMap<>();
        short[] data = null;
        
        // PARSE DATA 
        for (Map.Entry<String, Thermometer_values> entry : DPADataOut.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-40
            if(0 < key && 40 >= key) {

                System.out.println("Parsing resp for node: " + entry.getKey());

                if (null != entry.getValue()) {

                    pidMicrorisc++;

                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = DPAThermometers.get(entry.getKey()).getDPA_AdditionalInfoOfLastCall();

                    if (DPAOSInfo.get(entry.getKey()) != null) {
                        moduleId = DPAOSInfo.get(entry.getKey()).getPrettyFormatedModuleId();
                    } else {
                        moduleId = "not-known";
                    }
                    
                    String temperature = entry.getValue().getValue() + "." + entry.getValue().getFractialValue();

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String mqttData
                            = "{\"e\":["
                            + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + temperature + "}"
                            + "],"
                            + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                            + "}";

                    DPAParsedDataOut.put(entry.getKey(), mqttData);
                }
                else {
                    System.out.println("Microrisc result has not arrived.");
                }
            }
        }

        return DPAParsedDataOut;
    }
    
    // sends parsed data 
    public static void MQTTSendData() {
                
        // SEND DATA 
        for (Map.Entry<String, String> entry : DPAParsedDataOut.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-40
            if(0 < key && 40 >= key) {

                System.out.println("Sending parsed data for node: " + entry.getKey());

                if( null != entry.getValue() ) {
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_MICRORISC + entry.getKey() , 2, entry.getValue().getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                }
            }
        }
    }

    // prints out specified message, destroys the Simply and exits
    public static void printMessageAndExit(String message, boolean exit) {
        System.out.println(message);

        if (exit) {
            if (DPASimply != null) {
                DPASimply.destroy();
            }
            System.exit(1);
        }
    }
    
    // sender of mqtt requests to dpa 
    public static String sendDPAWebRequest(String topic, String msgSenML) {
        return null;
    }

    // sender of dpa async requests and responses to mqtt 
    public static boolean sendDPAAsyncRequest() {
        return true;
    }
}
