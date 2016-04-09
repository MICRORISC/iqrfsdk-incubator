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
import com.microrisc.simply.iqrf.dpa.v22x.devices.UART;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import java.io.File;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Running first tests with DPA <-> MQTT.
 *
 * @author Rostislav Spinar
 */
public class OpenGatewayTcpCloudProtronix {

    // references for DPA
    public static DPA_Simply DPASimply = null;
    public static Network DPANetwork = null;
    public static Map<String, Node> DPANodes = null;
    public static Map<String, OsInfo> DPAOSInfo = new LinkedHashMap<>();
    public static Map<String, UART> DPAUARTs = new LinkedHashMap<>();
    public static Map<String, UUID> DPAUARTUUIDs = new LinkedHashMap<>();
    public static Map<String, short[]> DPADataOut = new LinkedHashMap<>();
    public static Map<String, List<String>> DPAParsedDataOut = new LinkedHashMap<>();
    public static final int READING = 5; 
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
    public static int pidProtronix = 0;

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
        
        // REF TO UART ON NODES
        DPAUARTs = getUARTOnNodes();
        
        int checkResponse = 0;
        
        // SENDING AND RECEIVING
        while( true ) {
            
            DPAUARTUUIDs = sendDPARequests();
            
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
                
            // node 1-20
            if(0 < key && 20 >= key) {
                
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
    
    // reference to uart peripheral on all nodes
    public static Map<String, UART> getUARTOnNodes() {
        
        Map<String, UART> DPAUARTs = new LinkedHashMap<>();
        
        for (Map.Entry<String, Node> entry : DPANodes.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-20
            if(0 < key && 20 >= key) {
                
                System.out.println("Getting UART on the node: " + entry.getKey());
                
                // UART peripheral
                UART uart = entry.getValue().getDeviceObject(UART.class);

                if (uart == null) {
                    printMessageAndExit("UART doesn't exist on node", true);
                } 
                else {
                    int protronixHWPID = 0x0132;
                    uart.setRequestHwProfile(protronixHWPID);
                    
                    // worst case: 60 * 50 * 2 + 2000 (uart timeout) + 2000 (reserve)
                    uart.setDefaultWaitingTimeout(10000);

                    DPAUARTs.put(entry.getKey(), uart);
                }
            }
        }
        
        return DPAUARTs;
    }
    
    // sends all requests and do not wait for response
    public static Map<String, UUID> sendDPARequests() {

        // delay before getting uart response on the node
        short uartTimeout = 0x70;
        
        // 1B address, 1B function, 2B register, 2B number of registers, 2B crc
        short[] modbusIn = { 0x01, 0x04, 0x75, 0x31, 0x00, 0x03, 0x00, 0x00 };
        modbusIn = calculateModbudCrc(modbusIn, 0, 6);
        
        Map<String, UUID> DPAUARTUUIDs = new LinkedHashMap<>();
        UUID uuidUART = null;

        // SEND DPA REQUESTS
        for (Map.Entry<String, UART> entry : DPAUARTs.entrySet()) {

            int key = Integer.parseInt(entry.getKey());

            // node 1-20
            if(0 < key && 20 >= key) {

                System.out.println("Issuing req for node: " + entry.getKey());
                
                if (null != entry.getValue()) {

                    // ASYNC DPA CALL - NO BLOCKING
                    uuidUART = entry.getValue().async_writeAndRead(uartTimeout, modbusIn);
                    DPAUARTUUIDs.put(entry.getKey(), uuidUART);
                }
            }
        }
        
        return DPAUARTUUIDs;
    }
    
    // collect dpa responses 
    public static Map<String, short[]> collectDPAResponses() {

        // PROTRONIX PARAMS OUT
        Map<String, short[]> DPADataOut = new LinkedHashMap<>();
        CallRequestProcessingState procState = null;
        short[] dataOut = null;
        
        // CHECK STATE AND COLLECT 
        for (Map.Entry<String, UART> entry : DPAUARTs.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-20
            if(0 < key && 20 >= key) {

                System.out.println("Collecting resp for node: " + entry.getKey());

                if (null != entry.getValue()) {
                    procState = entry.getValue().getCallRequestProcessingState(DPAUARTUUIDs.get(entry.getKey()));

                    if (null != procState) {
                        // if any error occured
                        if (procState == CallRequestProcessingState.ERROR) {

                            // general call error
                            CallRequestProcessingError error = entry.getValue().getCallRequestProcessingError(DPAUARTUUIDs.get(entry.getKey()));
                            printMessageAndExit("Getting UART data failed on the node: " + error.getErrorType(), false);

                            DPADataOut.put(entry.getKey(), null);

                        } else {

                            // have result already - protronix
                            if (procState == CallRequestProcessingState.RESULT_ARRIVED) {

                                dataOut = entry.getValue().getCallResultImmediately(DPAUARTUUIDs.get(entry.getKey()), short[].class);

                                if (null != dataOut) {
                                    DPADataOut.put(entry.getKey(), dataOut);
                                }                            
                            } else if (procState == CallRequestProcessingState.ERROR) {

                                // general call error
                                CallRequestProcessingError error = entry.getValue().getCallRequestProcessingErrorOfLastCall();

                                if (error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL) {

                                    // specific call error
                                    DPA_AdditionalInfo dpaAddInfo = entry.getValue().getDPA_AdditionalInfoOfLastCall();
                                    DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                    printMessageAndExit("Getting UART data failed on the node, DPA error: " + dpaResponseCode, false);

                                    DPADataOut.put(entry.getKey(), null);
                                }
                            } else {
                                System.out.println("Getting UART data on node hasn't been processed yet: " + procState);
                                DPADataOut.put(entry.getKey(), null);
                            }
                        }
                    } else {
                        System.out.println("No call request processing info from connector");
                    }
                }
            }
        }
        
        return DPADataOut;
    }
    
    // data parsing
    public static Map<String, List<String>> parseDPAResponses() {
        
        Map<String, List<String>> DPAParsedDataOut = new LinkedHashMap<>();
        short[] data = null;
        
        // PARSE DATA 
        for (Map.Entry<String, short[]> entry : DPADataOut.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-20
            if(0 < key && 20 >= key) {

                System.out.println("Parsing resp for node: " + entry.getKey());

                if (null != entry.getValue()) {

                    if (0 == entry.getValue().length) {
                        System.out.print("No received data from UART on the node ");

                        DPAParsedDataOut.put(entry.getKey(), null);
                    }
                    else {
                        pidProtronix++;

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = DPAUARTs.get(entry.getKey()).getDPA_AdditionalInfoOfLastCall();

                        int co2 = (entry.getValue()[3] << 8) + entry.getValue()[4];
                        int humidity = (entry.getValue()[5] << 8) + entry.getValue()[6];
                        float temperature = (entry.getValue()[7] << 8) + entry.getValue()[8];

                        DecimalFormat df = new DecimalFormat("###.#");

                        if (DPAOSInfo.get(entry.getKey()) != null) {
                            moduleId = DPAOSInfo.get(entry.getKey()).getPrettyFormatedModuleId();
                        } else {
                            moduleId = "not-known";
                        }

                        List<String> mqttData = new LinkedList<>();
                        
                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        String mqttDataTemperature
                                = "{\"e\":["
                                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + df.format(temperature) + "}"
                                + "],"
                                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                + "}";
                        
                        mqttData.add(mqttDataTemperature);
                        
                        String mqttDataHumidity
                                = "{\"e\":["
                                + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "}"
                                + "],"
                                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                + "}";
                        
                        mqttData.add(mqttDataHumidity);
                        
                        String mqttDataCO2
                                = "{\"e\":["
                                + "{\"n\":\"co2\"," + "\"u\":\"PPM\"," + "\"v\":" + co2 + "}"
                                + "],"
                                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                + "}";
                        
                        mqttData.add(mqttDataCO2);

                        DPAParsedDataOut.put(entry.getKey(), mqttData);
                    }
                } else {
                    System.out.println("Protronix result has not arrived.");
                }
            }
        }
        
        return DPAParsedDataOut;
    }
    
    // sends parsed data 
    public static void MQTTSendData() {
                
        // SEND DATA 
        for (Map.Entry<String, List<String>> entry : DPAParsedDataOut.entrySet()) {
            
            int key = Integer.parseInt(entry.getKey());

            // node 1-20
            if(0 < key && 20 >= key) {

                System.out.println("Sending parsed data for node: " + entry.getKey());

                if( null != entry.getValue() ) {
                    for (String mqttData : entry.getValue()) {
                        try {
                            mqttCommunicator.publish(MQTTTopics.STD_SENSORS_PROTRONIX + entry.getKey(), 2, mqttData.getBytes());
                        } catch (MqttException ex) {
                            System.err.println("Error while publishing sync dpa message.");
                        }
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
    
    public static short[] calculateModbudCrc( short[] dataInOut, int start, int end ) {
        
        int lstart = start;
        int i = 0;
        
        if ( start < end && end < dataInOut.length ) {
            int crc = 0xFFFF;
            
            while ( lstart < end ) {
                crc ^= ( dataInOut[i] & 0xFF );

                for (int j=0; j<8; j++) {
                    boolean bitOne = ((crc & 0x01) == 0x01);
                    crc >>>= 1;
                    if(bitOne) {
                        crc ^= 0x0000A001;
                    }
                }

                lstart++; i++;
            }
            
            dataInOut[end] = (short)(crc & 0x00FF);
            dataInOut[end + 1] = (short)((crc & 0xFF00) >> 8);
            
            return (dataInOut);
            
        } else {
            System.out.println("Invalid start (" + start + ") and end (" + end + ")");
            return null;
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