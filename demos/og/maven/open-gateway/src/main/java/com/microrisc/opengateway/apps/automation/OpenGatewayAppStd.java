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
package com.microrisc.opengateway.apps.automation;

import com.microrisc.opengateway.config.ApplicationConfiguration;
import com.microrisc.opengateway.config.DeviceInfo;
import com.microrisc.opengateway.dpa.DPA_Request;
import com.microrisc.opengateway.dpa.DPA_Result;
import com.microrisc.opengateway.mqtt.MqttCommunicator;
import com.microrisc.opengateway.mqtt.MqttConfiguration;
import com.microrisc.opengateway.mqtt.MqttTopics;
import com.microrisc.opengateway.web.WebRequestParserException;
import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Custom;
import com.microrisc.simply.iqrf.dpa.v22x.devices.IO;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.devices.UART;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_Command;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_DirectionSettings;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_OutputValueSettings;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * There are two apps in Automation demo - each one handling its own network.
 * 
 * Automation std app reads periodically and waits for requests either from 
 * broker or web gui.
 * 
 * Automation lp app listens for async packets and send requests via broker.
 * It also reads periodically and waits for requests from web gui.
 * 
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public class OpenGatewayAppStd {

    // references for DPA
    private static DPA_Simply dpaSimply = null;
    
    // map of nodes
    private static Map<String, Node> nodesMap = null;
 
    // OS info for each node (indexed by its ID)
    private static Map<String, OsInfo> osInfoMap = null;
    
    // device's indexes, used in various maps
    // values are equal to the node identifiers, which the corresponding devices reside
    private static final String UART_PROTRONIX_NODE_ID = "1";
    private static final String CUSTOM_AUSTYN_NODE_ID = "2";
    private static final String UART_DEVTECH_NODE_ID = "3";
    private static final String CUSTOM_DATMOLUX_NODE_ID = "4";
    private static final String CUSTOM_TECO_NODE_ID = "5";
    
    // devices in DPA network to communicate with 
    private static UART uartProtronix = null;
    private static Custom customAustyn = null;
    private static UART uartDevtech = null;
    private static Custom customDatmolux = null;
    private static Custom customTeco = null;
    
    private static int pidProtronix = 0;
    private static int pidAustyn = 0;
    private static int pidDevtech = 0;
    private static int pidDatmolux = 0;
    private static int pidAsync = 0;
    
    // Datmolux constants to use while sending requests using Custom DI
    private static final short DATMOLUX_PER_ID = 0x20;
    private static final short DATMOLUX_CMD_OFF = 0x00;
    private static final short DATMOLUX_CMD_ON = 0x01;
    private static final short DATMOLUX_CMD_DOWN = 0x02;
    private static final short DATMOLUX_CMD_UP = 0x03;
    private static final short cmdIdDatmoLEVEL = 0x05;
    private static final short cmdIdDatmoPOWER = 0x06;
    private static final short[] DATMOLUX_DATA = new short[] {};
    
    // references for MQTT
    private static MqttCommunicator mqttCommunicator = null;
    
    // MQTT topics
    private static MqttTopics mqttTopics = null;

    // references for APP
    private static ApplicationConfiguration appConfiguration = null;

    
    // thread synchronization mean for web requests
    private static final Object syncProcessingWebRequest = new Object();
    
    // indicator, if it is possible to process web request
    private static Boolean isPossibleToProcessWebRequest = false;

    
    // MAIN
    public static void main(String[] args) throws InterruptedException, MqttException {
        // application exit hook
        Runtime.getRuntime().addShutdownHook( new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("End via shutdown hook.");
                releaseUsedResources();
            }
        }));

        // loading application configuration
        try {
            appConfiguration = loadApplicationConfiguration("App.json");
        } catch (Exception ex) {
            printMessageAndExit("Error in loading application configuration: " + ex);
        }

        // Simply initialization
        if(appConfiguration.getCommunicationInterface().equalsIgnoreCase("cdc")) {
            dpaSimply = getDpaSimply("Simply-CDC.properties");
        }
        else if(appConfiguration.getCommunicationInterface().equalsIgnoreCase("spi")) {
            dpaSimply = getDpaSimply("Simply-SPI.properties");
        }
        else {
           printMessageAndExit("No supported communication interface: " + appConfiguration.getCommunicationInterface());
        }
        
        // loading MQTT configuration
        MqttConfiguration mqttConfiguration = null;
        try {
            mqttConfiguration = loadMqttConfiguration("Mqtt.json");
        } catch ( Exception ex ) {
            printMessageAndExit("Error in loading MQTT configuration: " + ex);
        }
        
        // topics initialization
        mqttTopics =  new MqttTopics.Builder().gwId(mqttConfiguration.getGwId()).build();

        mqttCommunicator = new MqttCommunicator(mqttConfiguration);
        mqttCommunicator.subscribe(mqttTopics.getStdActuatorsAustyn(), 2);
        mqttCommunicator.subscribe(mqttTopics.getStdActuatorsDevtech(), 2);
        mqttCommunicator.subscribe(mqttTopics.getStdActuatorsDatmolux(), 2);
        mqttCommunicator.subscribe(mqttTopics.getStdActuatorsTeco(), 2);
        
        nodesMap = getNodesMap();
        initDeviceReferences(nodesMap);
        osInfoMap = getOsInfoFromNodes(nodesMap);
        
        getAndSendIoStateAustyn();
        getAndSendIoStateDevtech();
        setHwProfiles();

        // main application loop
        while ( true ) {
            // getting data from devices
            Map<String, Object> results = getDataFromDevices(); 
            
            // publishing results to MQTT
            publishResults(results);   
            
            // space for processing web requests
            waitUntilProcessIncommingWebRequests(appConfiguration.getPollingPeriod() * 1000);
        }
    }
    
    // returns nodes map
    private static Map<String, Node> getNodesMap() {
        Map<String, Node> nodesMap = new HashMap<>();
        
        // getting reference to IQRF DPA network to use
        Network network1 = dpaSimply.getNetwork("1", Network.class);
        if ( network1 == null ) {
            printMessageAndExit("DPA Network 1 doesn't exist");
        }
        
        // protronix
        Node nodeProtronix = network1.getNode(UART_PROTRONIX_NODE_ID);
        if ( nodeProtronix == null ) {
            printMessageAndExit("Node " + UART_PROTRONIX_NODE_ID + " doesn't exist");
        }
        nodesMap.put(UART_PROTRONIX_NODE_ID, nodeProtronix);

        // austyn
        Node nodeAustyn = network1.getNode(CUSTOM_AUSTYN_NODE_ID);
        if ( nodeAustyn == null ) {
            printMessageAndExit("Node " + CUSTOM_AUSTYN_NODE_ID + " doesn't exist");
        }
        nodesMap.put(CUSTOM_AUSTYN_NODE_ID, nodeAustyn);
        
        // devtech
        Node nodeDevtech = network1.getNode(UART_DEVTECH_NODE_ID);
        if ( nodeDevtech == null ) {
            printMessageAndExit("Node " + UART_DEVTECH_NODE_ID + " doesn't exist");
        }
        nodesMap.put(UART_DEVTECH_NODE_ID, nodeDevtech);
        
        // datmolux
        Node nodeDatmolux = network1.getNode(CUSTOM_DATMOLUX_NODE_ID);
        if ( nodeDatmolux == null ) {
            printMessageAndExit("Node " + CUSTOM_DATMOLUX_NODE_ID + " doesn't exist");
        }
        nodesMap.put(CUSTOM_DATMOLUX_NODE_ID, nodeDatmolux);
       
        // teco
        Node nodeTeco = network1.getNode(CUSTOM_TECO_NODE_ID);
        if ( nodeTeco == null ) {
            printMessageAndExit("Node " + CUSTOM_TECO_NODE_ID + " doesn't exist");
        }
        nodesMap.put(CUSTOM_TECO_NODE_ID, nodeTeco);
        
        return nodesMap;
    }
    
    // inits references to devices in DPA network used for communication
    private static void initDeviceReferences(Map<String, Node> nodesMap) {
        uartProtronix = nodesMap.get(UART_PROTRONIX_NODE_ID).getDeviceObject(UART.class);
        if ( uartProtronix == null ) {
            printMessageAndExit("UART doesn't exist on node " + UART_PROTRONIX_NODE_ID);
        }
        
        customAustyn = nodesMap.get(CUSTOM_AUSTYN_NODE_ID).getDeviceObject(Custom.class);
        if ( customAustyn == null ) {
            printMessageAndExit("Custom doesn't exist on node " + CUSTOM_AUSTYN_NODE_ID);
        }
        
        uartDevtech = nodesMap.get(UART_DEVTECH_NODE_ID).getDeviceObject(UART.class);
        if ( uartDevtech == null ) {
            printMessageAndExit("UART doesn't exist on node " + UART_DEVTECH_NODE_ID);
        }
        
        customDatmolux = nodesMap.get(CUSTOM_DATMOLUX_NODE_ID).getDeviceObject(Custom.class);
        if ( customDatmolux == null ) {
            printMessageAndExit("Custom doesn't exist on node " + CUSTOM_DATMOLUX_NODE_ID);
        }
        
        customTeco = nodesMap.get(CUSTOM_TECO_NODE_ID).getDeviceObject(Custom.class);
        if ( customTeco == null ) {
            printMessageAndExit("Custom doesn't exist on node " + CUSTOM_TECO_NODE_ID);
        }
    
    }
    
    // returns reference to map of OS info objects for specified nodes map
    private static Map<String, OsInfo> getOsInfoFromNodes(Map<String, Node> nodesMap) {
        Map<String, OsInfo> osInfoMap = new LinkedHashMap<>();
        
        for ( Map.Entry<String, Node> entry : nodesMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            // node ID must be within valid interval
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
                
            System.out.println("Getting OS info on the node: " + entry.getKey());

            OS os = entry.getValue().getDeviceObject(OS.class);
            if ( os == null ) {
                System.out.println("OS doesn't exist on node " + entry.getKey());
                osInfoMap.put(entry.getKey(), null);
                continue;
            }
            
            OsInfo osInfo = os.read();
            if ( osInfo == null ) {
                CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
                if ( procState == ERROR ) {
                    CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                    System.out.println("Getting OS info failed on node " + entry.getKey() + ": " + error);
                } else {
                    System.out.println(
                            "Getting OS info hasn't been processed yet on node " + entry.getKey()
                            + ". State of the request: " + procState
                    );
                }
            }
            osInfoMap.put(entry.getKey(), osInfo);   
        }
        
        return osInfoMap;
    }
    
    // prints specified bytes in hex and puts one space between each consecutive bytes
    private static void printInHexWithSpace(short[] arr) {
        for (int i = 0; i < arr.length; i++) {
            System.out.print(Integer.toHexString(arr[i]).toUpperCase() + " ");
        }
    }
    
    // returns ID of module for specified OS info
    private static String getModuleId(OsInfo osInfo) {
        if ( osInfo != null ) {
            return osInfo.getPrettyFormatedModuleId();
        }
        return "not-known";
    }
    
    private static void getAndSendIoStateAustyn() {
        IO ioa = nodesMap.get(CUSTOM_AUSTYN_NODE_ID).getDeviceObject(IO.class);
        if ( ioa == null ) {
            printMessageAndExit("IO doesn't exist on node " + CUSTOM_AUSTYN_NODE_ID);
        }
        
        short[] ioState = ioa.get();
        if ( ioState == null ) {            
            CallRequestProcessingState procState = ioa.getCallRequestProcessingStateOfLastCall();
            if ( procState == CallRequestProcessingState.ERROR ) {
                CallRequestProcessingError error = ioa.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Getting IO state failed on node " + CUSTOM_AUSTYN_NODE_ID + ": " + error);
            } else {
                System.out.println(
                        "Getting IO state hasn't been processed yet on node " + CUSTOM_AUSTYN_NODE_ID + ": "
                        + ". State of the request: " + procState
                );
            }
            return;
        }
        
        System.out.print("Austyn IO state: ");
        printInHexWithSpace(ioState);
        System.out.println();

        short PORTC = ioState[2];
        String STATE = "off";

        if ( (PORTC & 0x20) == 0x20 ) {
            STATE = "on";
        }

        // getting additional info of the last call
        DPA_AdditionalInfo dpaAddInfoIo = ioa.getDPA_AdditionalInfoOfLastCall();
        String moduleId = getModuleId(osInfoMap.get(CUSTOM_AUSTYN_NODE_ID));
        
        String webResponseTopic = mqttTopics.getStdActuatorsAustyn();

        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
        String webResponseToBeSent
                = "{"
                + "\"e\":[{\"n\":\"io\"," + "\"sv\":\"" + STATE + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pidAustyn + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + nodesMap.get(CUSTOM_AUSTYN_NODE_ID).getId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfoIo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfoIo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfoIo.getDPA_Value() + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
/*
        try {
            mqttCommunicator.publish(webResponseTopic, 2, webResponseToBeSent.getBytes());
        } catch ( MqttException ex ) {
            System.err.println("Error while publishing web response message.");
        }
*/
    }
    
    private static void getAndSendIoStateDevtech() {
        IO ioa = nodesMap.get(UART_DEVTECH_NODE_ID).getDeviceObject(IO.class);
        if ( ioa == null ) {
            printMessageAndExit("IO doesn't exist on node " + UART_DEVTECH_NODE_ID);
        }
        
        short[] ioState = ioa.get();
        if ( ioState == null ) {            
            CallRequestProcessingState procState = ioa.getCallRequestProcessingStateOfLastCall();
            if ( procState == CallRequestProcessingState.ERROR ) {
                CallRequestProcessingError error = ioa.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Getting IO state failed on node " + UART_DEVTECH_NODE_ID + ": " + error);
            } else {
                System.out.println(
                        "Getting IO state hasn't been processed yet on node " + UART_DEVTECH_NODE_ID + ": "
                        + ". State of the request: " + procState
                );
            }
            return;
        }
        
        System.out.print("Devtech IO state: ");
        printInHexWithSpace(ioState);
        System.out.println();

        short PORTC = ioState[2];
        String STATE = "off";

        if ( (PORTC & 0x08) == 0x08 ) {
            STATE = "on";
        }

        // getting additional info of the last call
        DPA_AdditionalInfo dpaAddInfoIo = ioa.getDPA_AdditionalInfoOfLastCall();
        String moduleId = getModuleId(osInfoMap.get(UART_DEVTECH_NODE_ID));
        
        String webResponseTopic = mqttTopics.getStdActuatorsDevtech();

        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
        String webResponseToBeSent
                = "{"
                + "\"e\":[{\"n\":\"io\"," + "\"sv\":\"" + STATE + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pidAustyn + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + nodesMap.get(UART_DEVTECH_NODE_ID).getId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfoIo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfoIo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfoIo.getDPA_Value() + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";

/*        
        try {
            mqttCommunicator.publish(webResponseTopic, 2, webResponseToBeSent.getBytes());
        } catch ( MqttException ex ) {
            System.err.println("Error while publishing web response message.");
        }
*/
    }
    
    // sets specified HW profile for UART on specified node
    private static void setHwProfileForUART(Node node, int hwProfile) {
        UART uart = node.getDeviceObject(UART.class);
        if ( uart == null ) {
            printMessageAndExit("UART doesn't exist on node " + node.getId());
        } 
        uart.setRequestHwProfile(hwProfile);
    }
    
    // sets specified HW profile for Custom on specified node
    private static void setHwProfileForCustom(Node node, int hwProfile) {
        Custom custom = node.getDeviceObject(Custom.class);
        if ( custom == null ) {
            printMessageAndExit("Custom doesn't exist on node " + node.getId());
        } 
        custom.setRequestHwProfile(hwProfile);
    }
    
    // sets HW profiles for various devices on nodes
    private static void setHwProfiles() {
        setHwProfileForUART(nodesMap.get(UART_PROTRONIX_NODE_ID), 0xFFFF);
        setHwProfileForCustom(nodesMap.get(CUSTOM_AUSTYN_NODE_ID), 0x0211);
        setHwProfileForUART(nodesMap.get(UART_DEVTECH_NODE_ID), 0xFFFF);
        setHwProfileForCustom(nodesMap.get(CUSTOM_DATMOLUX_NODE_ID), 0x0611);
        setHwProfileForCustom(nodesMap.get(CUSTOM_TECO_NODE_ID), 0x0511);
    }
    
    // init dpa simply
    private static DPA_Simply getDpaSimply(String configFile) {
        DPA_Simply DPASimply = null;

        try {
            DPASimply = DPA_SimplyFactory.getSimply("config" + File.separator + "simply" + File.separator + configFile);
        } catch (SimplyException ex) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage());
        }

        return DPASimply;
    }
    
    // tests, if specified node ID is in valid interval
    private static boolean isNodeIdInValidInterval(long nodeId) {
        return ( nodeId > 0 && nodeId <= 5 );
    }
    
    // sends request and returns data from UART protronix device
    private static short[] getDataFromUartProtronix() {
        short[] result = uartProtronix.writeAndRead(0x0A, new short[] { 0x47, 0x44, 0x03 } );
        if ( result != null ) {
            return result;
        } else {
            CallRequestProcessingState procState = uartProtronix.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = uartProtronix.getCallRequestProcessingErrorOfLastCall();
                System.out.println(
                        "Getting data failed on node " + UART_PROTRONIX_NODE_ID + ": " + error.getErrorType()
                );
            } else {
                System.out.println(
                        "Getting data on node " + UART_PROTRONIX_NODE_ID + " hasn't been processed yet: " + procState
                );
            }
        }
        
        return null;
    }
    
    // sends request and returns data from Custom Austyn device
    private static short[] getDataFromCustomAustyn() {
        short[] dataTemp = new short[] {};
        short[] result = customAustyn.send((short)0x20, (short)0x01, dataTemp);
        if ( result != null ) {
            return result;
        } else {
            CallRequestProcessingState procState = customAustyn.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = customAustyn.getCallRequestProcessingErrorOfLastCall();
                System.out.println(
                        "Getting data failed on node " + CUSTOM_AUSTYN_NODE_ID + ": " + error.getErrorType()
                );
            } else {
                System.out.println(
                        "Getting data on node " + CUSTOM_AUSTYN_NODE_ID + " hasn't been processed yet: " + procState
                );
            }
        }
        
        return null;
    }
    
    // sends request and returns data from Custom Austyn device
    private static short[] getDataFromUartDevtech() {
        short[] result = uartDevtech.writeAndRead(0xEF, new short[] { 0x65, 0xFD } );
        if ( result != null ) {
            return result;
        } else {
            CallRequestProcessingState procState = uartDevtech.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = uartDevtech.getCallRequestProcessingErrorOfLastCall();
                System.out.println(
                        "Getting data failed on node " + UART_DEVTECH_NODE_ID + ": " + error.getErrorType()
                );
            } else {
                System.out.println(
                        "Getting data on node " + UART_DEVTECH_NODE_ID + " hasn't been processed yet: " + procState
                );
            }
        }
        
        return null;
    }
    
    // sends request and returns data from Custom Datmolux device
    private static short[] getDataFromCustomDatmolux() {
        short[] result = customDatmolux.send((short)0x20, (short)0x06, new short[] {} );
        if ( result != null ) {
            return result;
        } else {
            CallRequestProcessingState procState = customDatmolux.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                System.out.println(
                        "Getting data failed on node " + CUSTOM_DATMOLUX_NODE_ID + ": " + error.getErrorType()
                );
            } else {
                System.out.println(
                        "Getting data on node " + CUSTOM_DATMOLUX_NODE_ID + " hasn't been processed yet: " + procState
                );
            }
        }
        
        return null;
    }
    
    // sends requests to devices into network and returns results
    private static Map<String, Object> getDataFromDevices() {
        Map<String, Object> results = new HashMap<>();
        
        results.put(UART_PROTRONIX_NODE_ID, getDataFromUartProtronix());
        waitUntilProcessIncommingWebRequests(0);
        
        results.put(CUSTOM_AUSTYN_NODE_ID, getDataFromCustomAustyn());
        waitUntilProcessIncommingWebRequests(0);
        
        results.put(UART_DEVTECH_NODE_ID, getDataFromUartDevtech());
        waitUntilProcessIncommingWebRequests(0);
        
        results.put(CUSTOM_DATMOLUX_NODE_ID, getDataFromCustomDatmolux());
        waitUntilProcessIncommingWebRequests(0);
        
        return results;
    }
    
    // publishes result of UART protronix
    private static void publishUartProtronixResult(short[] result) {
        if ( result == null ) {
            System.out.println("Publishing UART Protronix - no result to publish.");
            return;
        }
        
        if ( result.length == 0 ) {
            System.out.print("Publishing UART Protronix - empty data");
            return;
        }
        
        pidProtronix++;

        // getting additional info of the last call
        DPA_AdditionalInfo dpaAddInfo = uartProtronix.getDPA_AdditionalInfoOfLastCall();

        System.out.print("Received data from UART on the node " + nodesMap.get(UART_PROTRONIX_NODE_ID).getId() + ": ");
        printInHexWithSpace(result);
        System.out.println();

        float temperature = (result[4] << 8) + result[5];
        temperature = temperature / 10;

        float humidity = (result[2] << 8) + result[3];
        humidity = Math.round(humidity / 10);

        int co2 = (result[0] << 8) + result[1];

        DecimalFormat df = new DecimalFormat("###.##");

        String moduleId = getModuleId(osInfoMap.get(UART_PROTRONIX_NODE_ID));

        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
        String responseData
                = "{\"e\":["
                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + df.format(temperature) + "},"
                + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "},"
                + "{\"n\":\"co2\"," + "\"u\":\"PPM\"," + "\"v\":" + df.format(co2) + "}"
                + "],"
                + "\"iqrf\":["
                + "{\"pid\":" + pidProtronix + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + nodesMap.get(UART_PROTRONIX_NODE_ID).getId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.UART + "," + "\"pcmd\":" + "\"" + UART.MethodID.WRITE_AND_READ.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";

        // send result's data to mqtt
        try {
            mqttCommunicator.publish(mqttTopics.getStdSensorsProtronix(), 0, responseData.getBytes());
        } catch ( MqttException ex ) {
            System.err.println("Error while publishing sync dpa message: " + ex);
        }
    } 
    
    private static void publishCustomAustynResult(short[] result) {
        if ( result == null ) {
            System.out.println("Publishing Custom Austyn - no result to publish.");
            return;
        }
        
        if ( result.length == 0 ) {
            System.out.print("Publishing Custom Austyn - empty data");
            return;
        }
        
        pidAustyn++;
                    
        // getting additional info of the last call
        DPA_AdditionalInfo dpaAddInfo = customAustyn.getDPA_AdditionalInfoOfLastCall();
        
        System.out.print("Received data from Custom on the node " + nodesMap.get(CUSTOM_AUSTYN_NODE_ID).getId() + ": ");
        printInHexWithSpace(result);
        System.out.println();

        short rawSixteenth = (short) (result[0] | (result[1] << 8));
        float temperature = rawSixteenth / 16.0f;

        DecimalFormat df = new DecimalFormat("###.##");

        String moduleId = getModuleId(osInfoMap.get(CUSTOM_AUSTYN_NODE_ID));

        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
        String responseData
                = "{\"e\":["
                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + df.format(temperature) + "}"
                + "],"
                + "\"iqrf\":["
                + "{\"pid\":" + pidAustyn + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + nodesMap.get(CUSTOM_AUSTYN_NODE_ID).getId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";

        // send data to mqtt
        try {
            mqttCommunicator.publish(mqttTopics.getStdSensorsAustyn(), 0, responseData.getBytes());
        } catch ( MqttException ex ) {
            System.err.println("Error while publishing sync dpa message: " + ex);
        }
    }
    
    private static void publishUartDevtechResult(short[] result) {
        if ( result == null ) {
            System.out.println("Publishing UART Devtech - no result to publish.");
            return;
        }
        
        if ( result.length == 0 ) {
            System.out.print("Publishing UART Devtech - empty data");
            return;
        }
        
        pidDevtech++;
                    
        // getting additional info of the last call
        DPA_AdditionalInfo dpaAddInfo = uartDevtech.getDPA_AdditionalInfoOfLastCall();

        System.out.print("Received data from UART on the node " + nodesMap.get(UART_DEVTECH_NODE_ID).getId() + ": ");
        printInHexWithSpace(result);
        System.out.println();

        float supplyVoltage = (result[0] * 256 + result[1]) / 100;
        float frequency = (result[2] * 256 + result[3]) / 100;
        float activePower = (result[4] * 256 + result[5]) / 100;
        float supplyCurrent = (result[6] * 256 + result[7]) / 100;
        float powerFactor = (result[8] * 256 + result[9]) / 100;
        float activeEnergy = (result[10] * 65536 + result[11] * 256 + result[12]) / 100;
        float deviceBurningHour = (result[13] * 256 + result[14]) / 100;
        float ledBurningHour = (result[15] * 256 + result[16]) / 100;
        float dimming = result[17];

        String moduleId = getModuleId(osInfoMap.get(UART_DEVTECH_NODE_ID));
                    
        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
        String responseData
                = "{\"e\":["
                + "{\"n\":\"supply voltage\"," + "\"u\":\"V\"," + "\"v\":" + supplyVoltage + "},"
                + "{\"n\":\"frequency\"," + "\"u\":\"Hz\"," + "\"v\":" + frequency + "},"
                + "{\"n\":\"active power\"," + "\"u\":\"W\"," + "\"v\":" + activePower + "},"
                + "{\"n\":\"supply current\"," + "\"u\":\"A\"," + "\"v\":" + supplyCurrent + "},"
                + "{\"n\":\"power factor\"," + "\"u\":\"cos\"," + "\"v\":" + powerFactor + "},"
                + "{\"n\":\"active energy\"," + "\"u\":\"J\"," + "\"v\":" + activeEnergy + "},"
                + "{\"n\":\"device burning hour\"," + "\"u\":\"hours\"," + "\"v\":" + deviceBurningHour + "},"
                + "{\"n\":\"led burning hour\"," + "\"u\":\"hours\"," + "\"v\":" + ledBurningHour + "},"
                + "{\"n\":\"dimming\"," + "\"u\":\"%\"," + "\"v\":" + dimming + "}"
                + "],"
                + "\"iqrf\":["
                + "{\"pid\":" + pidDevtech + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + nodesMap.get(UART_DEVTECH_NODE_ID).getId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.UART + "," + "\"pcmd\":" + "\"" + UART.MethodID.WRITE_AND_READ.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
        
        // send data to mqtt
        
        try {
            mqttCommunicator.publish(mqttTopics.getStdStatusDevtech(), 0, responseData.getBytes());
        } catch ( MqttException ex ) {
            System.err.println("Error while publishing sync dpa message: " + ex);
        }
    }
    
    private static void publishCustomDatmoluxResult(short[] result) {
        if ( result == null ) {
            System.out.println("Publishing Custom Datmolux - no result to publish.");
            return;
        }
        
        if ( result.length == 0 ) {
            System.out.print("Publishing Custom Datmolux - empty data");
            return;
        }
        
        pidDatmolux++;

        // getting additional info of the last call
        DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

        System.out.print("Received data from Custom on the node " + nodesMap.get(CUSTOM_DATMOLUX_NODE_ID).getId() + ": ");
        printInHexWithSpace(result);
        System.out.println();

        int activePower = result[0];

        String moduleId = getModuleId(osInfoMap.get(CUSTOM_DATMOLUX_NODE_ID));

        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
        String responseData
                = "{\"e\":["
                + "{\"n\":\"active power\"," + "\"u\":\"W\"," + "\"v\":" + activePower + "}"
                + "],"
                + "\"iqrf\":["
                + "{\"pid\":" + pidDevtech + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + nodesMap.get(CUSTOM_DATMOLUX_NODE_ID).getId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
        
        // send data to mqtt
        try {
            mqttCommunicator.publish(mqttTopics.getStdStatusDatmolux(), 0, responseData.getBytes());
        } catch ( MqttException ex ) {
            System.err.println("Error while publishing sync dpa message: " + ex);
        }
    }
    
    // publishes specified results to MQQT 
    private static void publishResults(Map<String, Object> results) {
        publishUartProtronixResult((short[]) results.get(UART_PROTRONIX_NODE_ID));
        publishCustomAustynResult((short[]) results.get(CUSTOM_AUSTYN_NODE_ID));
        publishUartDevtechResult((short[]) results.get(UART_DEVTECH_NODE_ID));
        publishCustomDatmoluxResult((short[]) results.get(CUSTOM_DATMOLUX_NODE_ID));
    }
    
    // waits until incomming web requests will be processed - if there is present one
    // timeToSleep (roughly) duration of time to incomming process web requests
    private static void waitUntilProcessIncommingWebRequests(long timeToSleep) {
        
        // open the space for processing a web request
        synchronized ( syncProcessingWebRequest ) {
            isPossibleToProcessWebRequest = true;
            syncProcessingWebRequest.notifyAll();
        }
        
        if ( timeToSleep > 0 ) {
            try {
                Thread.sleep(timeToSleep);
            } catch ( InterruptedException ex ) {
                printMessageAndExit("Waiting on incomming web request interrupted");
            }
        }
        
        // other tread processes incomming web requests ...
        
        // close the space for processing a web request
        synchronized ( syncProcessingWebRequest ) {
            isPossibleToProcessWebRequest = false;
            syncProcessingWebRequest.notifyAll();
        }
    }
    
    // sends specified DPA request into DPA network and returns response data
    // to publish
    private static DPA_Result sendRequestToAustynActuator(DPA_Request dpaRequest) {
        if ( !dpaRequest.getDpa().equalsIgnoreCase("REQ") ) {
            return null;
        }
        
        if ( !dpaRequest.getN().equalsIgnoreCase("IO") ) {
            return null;
        }
        
        IO io = nodesMap.get(CUSTOM_AUSTYN_NODE_ID).getDeviceObject(IO.class);
        if ( io == null ) {
            printMessageAndExit("IO doesn't exist on node " + CUSTOM_AUSTYN_NODE_ID);
        }
        
        Object result = null;
        CallRequestProcessingError error = null;
        DPA_AdditionalInfo dpaAddInfo = null;
        
        if ( dpaRequest.getSv().equalsIgnoreCase("ON") ) {
            // set all pins OUT
            IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[] {
                new IO_DirectionSettings(0x02, 0x20, 0x00)
            };

            result = io.setDirection(dirSettings);
            if ( result == null ) {
                error = io.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting IO direction failed: " + error);
            }

            // set Austyn HIGH
            IO_Command[] iocs = new IO_OutputValueSettings[] {
                new IO_OutputValueSettings(0x02, 0x20, 0x20)
            };

            for ( int i = 0; i < 3; i++ ) {
                result = io.setOutputState(iocs);
                if ( result != null ) {
                    break;
                }
            }

            if ( result == null ) {
                CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                if ( procState == CallRequestProcessingState.ERROR ) {
                    error = io.getCallRequestProcessingErrorOfLastCall();
                    System.out.println("Setting IO output state failed: " + error);
                } else {
                    System.out.println("Setting IO output state hasn't been processed yet: " + procState);
                }
                return null;
            } else {
                dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();
            }
        }

        if ( dpaRequest.getSv().equalsIgnoreCase("OFF") ) {
            // set all pins OUT
            IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[] {
                new IO_DirectionSettings(0x02, 0x20, 0x00)
            };

            result = io.setDirection(dirSettings);
            if ( result == null ) {
                error = io.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting IO direction failed: " + error);
            }

            // set Austyn LOW
            IO_Command[] iocs = new IO_OutputValueSettings[] {
                new IO_OutputValueSettings(0x02, 0x20, 0x00)
            };

            for ( int i = 0; i < 3; i++ ) {
                result = io.setOutputState(iocs);
                if ( result != null ) {
                    break;
                }
            }

            if ( result == null ) {
                CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                if ( procState == CallRequestProcessingState.ERROR ) {
                    error = io.getCallRequestProcessingErrorOfLastCall();
                    System.out.println("Setting IO output state failed: " + error);
                } else {
                    System.out.println("Setting IO output state hasn't been processed yet: " + procState);
                }
                return null;
            } else {
                dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();
            }
        }
        
        return new DPA_Result(
                result, error, dpaAddInfo, 
                new DPA_Result.Request(
                        nodesMap.get(CUSTOM_AUSTYN_NODE_ID).getId(), 
                        DPA_ProtocolProperties.PNUM_Properties.IO, 
                        IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase(), 
                        getModuleId(osInfoMap.get(CUSTOM_AUSTYN_NODE_ID))
                )
        );
    }
    
    private static DPA_Result sendRequestToDevtechActuator(DPA_Request dpaRequest) {
        if ( !dpaRequest.getDpa().equalsIgnoreCase("REQ") ) {
            System.out.println("Web request - SV: " + dpaRequest.getDpa());
            return null;
        }
        
        if ( !dpaRequest.getN().equalsIgnoreCase("IO") ) {
            System.out.println("Web request - SV: " + dpaRequest.getN());
            return null;
        }
        
        IO io = nodesMap.get(UART_DEVTECH_NODE_ID).getDeviceObject(IO.class);
        if ( io == null ) {
            printMessageAndExit("IO doesn't exist on node " + UART_DEVTECH_NODE_ID);
        }
        
        Object result = null;
        CallRequestProcessingError error = null;
        DPA_AdditionalInfo dpaAddInfo = null;
        
        System.out.println("Web request - SV: " + dpaRequest.getSv());
        if ( dpaRequest.getSv().equalsIgnoreCase("ON") ) {
            // set devtech pins OUT
            IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[] {
                new IO_DirectionSettings(0x02, 0x08, 0x00)
            };

            result = io.setDirection(dirSettings);
            if ( result == null ) {
                error = io.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting IO direction failed: " + error);
            }

            // set Devtech HIGH
            IO_Command[] iocs = new IO_OutputValueSettings[] {
                new IO_OutputValueSettings(0x02, 0x08, 0x08)
            };

            for ( int i = 0; i < 3; i++ ) {
                result = io.setOutputState(iocs);
                if ( result != null ) {
                    break;
                }
            }

            if ( result == null ) {
                CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                if ( procState == CallRequestProcessingState.ERROR ) {
                    error = io.getCallRequestProcessingErrorOfLastCall();
                    System.out.println("Setting IO output state failed: " + error);
                } else {
                    System.out.println("Setting IO output state hasn't been processed yet: " + procState);
                }
                return null;
            } else {
                dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();
            }
        }

        if ( dpaRequest.getSv().equalsIgnoreCase("OFF") ) {
            // set all pins OUT
            IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[] {
                new IO_DirectionSettings(0x02, 0x20, 0x00)
            };

            result = io.setDirection(dirSettings);
            if ( result == null ) {
                error = io.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting IO direction failed: " + error);
            }

            // set all pins OUT
            IO_Command[] iocs = new IO_OutputValueSettings[] {
                new IO_OutputValueSettings(0x02, 0x08, 0x00)
            };

            for ( int i = 0; i < 3; i++ ) {
                result = io.setOutputState(iocs);
                if ( result != null ) {
                    break;
                }
            }

            if ( result == null ) {
                CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                if ( procState == CallRequestProcessingState.ERROR ) {
                    error = io.getCallRequestProcessingErrorOfLastCall();
                    System.out.println("Setting IO output state failed: " + error);
                } else {
                    System.out.println("Setting IO output state hasn't been processed yet: " + procState);
                }
                return null;
            } else {
                dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();
            }
        }
        
        return new DPA_Result(
                result, error, dpaAddInfo, 
                new DPA_Result.Request(
                        nodesMap.get(UART_DEVTECH_NODE_ID).getId(), 
                        DPA_ProtocolProperties.PNUM_Properties.IO, 
                        IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase(), 
                        getModuleId(osInfoMap.get(UART_DEVTECH_NODE_ID))
                )
        );
    }
    
    private static DPA_Result sendRequestToDatmoluxActuator(DPA_Request dpaRequest) {
        if ( !dpaRequest.getDpa().equalsIgnoreCase("REQ") ) {
            return null;
        }
        
        if ( !dpaRequest.getN().equalsIgnoreCase("CUSTOM") ) {
            return null;
        }
        
        Object result = null;
        CallRequestProcessingError error = null;
        DPA_AdditionalInfo dpaAddInfo = null;
        
        // getting Custom interface - datmolux
        Custom customDatmolux = nodesMap.get(CUSTOM_DATMOLUX_NODE_ID).getDeviceObject(Custom.class);
        if ( customDatmolux == null ) {
            printMessageAndExit("Custom doesn't exist on node " + CUSTOM_DATMOLUX_NODE_ID);
        }
        
        if ( dpaRequest.getSv().equalsIgnoreCase("ON") ) {
            for ( int i = 0; i < 3; i++ ) {
                result = customDatmolux.send(DATMOLUX_PER_ID, DATMOLUX_CMD_ON, DATMOLUX_DATA);
                if ( result != null ) {
                    break;
                }
            }
            
            if ( result == null ) {
                error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting Custom failed on node " 
                        + CUSTOM_DATMOLUX_NODE_ID + ":" + error
                );
                return null;
            } else {
                dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();
            }
        }

        if ( dpaRequest.getSv().equalsIgnoreCase("OFF") ) {
            for ( int i = 0; i < 3; i++ ) {
                result = customDatmolux.send(DATMOLUX_PER_ID, DATMOLUX_CMD_OFF, DATMOLUX_DATA);
                if ( result != null ) {
                    break;
                }
            }
            
            if ( result == null ) {
                error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting Custom failed on node " 
                        + CUSTOM_DATMOLUX_NODE_ID + ":" + error
                );
                return null;
            } else {
                dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();
            }
        }
        
        if ( dpaRequest.getSv().equalsIgnoreCase("UP") ) {
            for ( int i = 0; i < 3; i++ ) {
                result = customDatmolux.send(DATMOLUX_PER_ID, DATMOLUX_CMD_UP, DATMOLUX_DATA);
                if ( result != null ) {
                    break;
                }
            }
            
            if ( result == null ) {
                error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting Custom failed on node " 
                        + CUSTOM_DATMOLUX_NODE_ID + ":" + error
                );
                return null;
            } else {
                dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();
            }
        }
        
        if ( dpaRequest.getSv().equalsIgnoreCase("DOWN") ) {
            for ( int i = 0; i < 3; i++ ) {
                result = customDatmolux.send(DATMOLUX_PER_ID, DATMOLUX_CMD_DOWN, DATMOLUX_DATA);
                if ( result != null ) {
                    break;
                }
            }
            
            if ( result == null ) {
                error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting Custom failed on node " 
                        + CUSTOM_DATMOLUX_NODE_ID + ":" + error
                );
                return null;
            } else {
                dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();
            }
        }
        
        return new DPA_Result(
                result, error, dpaAddInfo, 
                new DPA_Result.Request(
                        nodesMap.get(CUSTOM_DATMOLUX_NODE_ID).getId(), 
                        DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START, 
                        Custom.MethodID.SEND.name().toLowerCase(), 
                        getModuleId(osInfoMap.get(CUSTOM_DATMOLUX_NODE_ID))
                )
        );
    }
    
    private static DPA_Result sendRequestToTecoActuator(DPA_Request dpaRequest) {
        if ( !dpaRequest.getDpa().equalsIgnoreCase("REQ") ) {
            return null;
        }
        
        if ( !dpaRequest.getN().equalsIgnoreCase("CUSTOM") ) {
            return null;
        }
        
        IO io = nodesMap.get(CUSTOM_TECO_NODE_ID).getDeviceObject(IO.class);
        if ( io == null ) {
            printMessageAndExit("Custom doesn't exist on node " + CUSTOM_TECO_NODE_ID);
        }
        
        Object result = null;
        CallRequestProcessingError error = null;
        DPA_AdditionalInfo dpaAddInfo = null;
        
        if ( dpaRequest.getSv().equalsIgnoreCase("ON") ) {
            for ( int i = 0; i < 3; i++ ) {
                result = customTeco.send((short) 0x20, (short) 0x00, new short[]{});
                if ( result != null ) {
                    break;
                }
            }
            
            if ( result == null ) {
                error = customTeco.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting Custom failed on node " + CUSTOM_TECO_NODE_ID + ":" + error);
                return null;
            } else {
                dpaAddInfo = customTeco.getDPA_AdditionalInfoOfLastCall();
            }
        }

        if ( dpaRequest.getSv().equalsIgnoreCase("OFF") ) {
            for ( int i = 0; i < 3; i++ ) {
                result = customTeco.send((short) 0x20, (short) 0x01, new short[] {});
                if ( result != null ) {
                    break;
                }
            }
            
            if ( result == null ) {
                error = customTeco.getCallRequestProcessingErrorOfLastCall();
                System.out.println("Setting Custom failed on node " + CUSTOM_TECO_NODE_ID + ":" + error);
                return null;
            } else {
                dpaAddInfo = customTeco.getDPA_AdditionalInfoOfLastCall();
            }
        }
        
        return new DPA_Result(
                result, error, dpaAddInfo, 
                new DPA_Result.Request(
                        nodesMap.get(CUSTOM_TECO_NODE_ID).getId(), 
                        DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START, 
                        Custom.MethodID.SEND.name().toLowerCase(), 
                        getModuleId(osInfoMap.get(CUSTOM_TECO_NODE_ID))
                )
        );
    }
    
    // sends specified DPA request and returns result
    private static DPA_Result processWebRequest(DPA_Request dpaRequest, String topic) {
        
        if(topic.equals(mqttTopics.getStdActuatorsAustyn())) {
            return sendRequestToAustynActuator(dpaRequest);
        }
        else if (topic.equals(mqttTopics.getStdActuatorsDevtech())) {
            return sendRequestToDevtechActuator(dpaRequest);
        }
        else if (topic.equals(mqttTopics.getStdActuatorsDatmolux())) {
            return sendRequestToDatmoluxActuator(dpaRequest);
        }
        else if (topic.equals(mqttTopics.getStdActuatorsTeco())) {
            return sendRequestToTecoActuator(dpaRequest);
        }

        return null;
    }
    
    // sends request to IQRF DPA network and returns result
    public static DPA_Result sendWebRequestToDpaNetwork(DPA_Request request, String topic) 
            throws WebRequestParserException 
    {
        synchronized ( syncProcessingWebRequest ) {
            while ( !isPossibleToProcessWebRequest ) {
                try {
                    syncProcessingWebRequest.wait();
                } catch ( InterruptedException ex ) {
                    printMessageAndExit("Waiting on start to process incomming web request interrupted.");
                }
            }
            return processWebRequest( request, topic );
        }
    }

    // loads mqtt params from file
    private static MqttConfiguration loadMqttConfiguration(String configFile)
            throws IOException, ParseException 
    {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(
                new FileReader("config" + File.separator + "mqtt" + File.separator + configFile)
        );

        JSONObject jsonObject = (JSONObject) obj;

        return new MqttConfiguration(
                (String) jsonObject.get("protocol"),
                (String) jsonObject.get("broker"),
                (long) jsonObject.get("port"),
                (String) jsonObject.get("clientid"),
                (String) jsonObject.get("gwid"),
                (boolean) jsonObject.get("cleansession"),
                (boolean) jsonObject.get("quitemode"),
                (boolean) jsonObject.get("ssl"),
                (String) jsonObject.get("certfile"),
                (String) jsonObject.get("username"),
                (String) jsonObject.get("password"),
                (String) jsonObject.get("roottopic")
        );
    }

    // loads app configuration from file
    private static ApplicationConfiguration loadApplicationConfiguration(String configFile)
            throws IOException, ParseException {
        
        JSONObject appJsonObjects = (JSONObject) JSONValue
                .parseWithException(new FileReader(
                                "config" + File.separator + "app" + File.separator + configFile
                        )
                );

        // get the devices
        JSONArray devicesArray = (JSONArray) appJsonObjects.get("devices");

        Map<Integer, DeviceInfo> devicesInfos = new HashMap<>();
        for (int i = 0; i < devicesArray.size(); i++) {
            JSONObject deviceObjects = (JSONObject) devicesArray.get(i);

            DeviceInfo deviceInfo = new DeviceInfo(
                    (long) deviceObjects.get("device"),
                    (String) deviceObjects.get("manufacturer"),
                    (String) deviceObjects.get("type")
            );

            devicesInfos.put((int) deviceInfo.getId(), deviceInfo);
        }

        return new ApplicationConfiguration(
                (long) appJsonObjects.get("pollingPeriod"),
                (String) appJsonObjects.get("communicationInterface"),
                devicesInfos
        );
    }

    // releases used resources
    private static void releaseUsedResources() {
        // main dpa object
        if ( dpaSimply != null ) {
            dpaSimply.destroy();
        }
    }

    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message) {
        System.out.println(message);
        releaseUsedResources();
        System.exit(1);
    }
}
