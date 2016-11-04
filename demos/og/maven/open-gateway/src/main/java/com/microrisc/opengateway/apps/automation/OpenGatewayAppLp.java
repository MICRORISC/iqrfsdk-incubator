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

import com.microrisc.opengateway.async.AsyncDataForMqtt;
import com.microrisc.opengateway.async.AsyncDataForMqttCreator;
import com.microrisc.opengateway.async.AsyncDataForMqttCreatorException;
import com.microrisc.opengateway.config.ApplicationConfiguration;
import com.microrisc.opengateway.config.DeviceInfo;
import com.microrisc.opengateway.dpa.DPA_Result;
import com.microrisc.opengateway.mqtt.MqttCommunicator;
import com.microrisc.opengateway.mqtt.MqttConfiguration;
import com.microrisc.opengateway.mqtt.MqttFormatter;
import com.microrisc.opengateway.mqtt.MqttTopics;
import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import com.microrisc.simply.DeviceObject;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.asynchrony.AsynchronousMessagesListener;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.errors.CallRequestProcessingErrorType;
import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Custom;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
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
public class OpenGatewayAppLp {

    // references for DPA
    private static DPA_Simply dpaSimply = null;
    
    // OS info for each node (indexed by its ID)
    private static Map<String, OsInfo> osInfoMap = null;

    // references for MQTT
    private static MqttCommunicator mqttCommunicator = null;
    
    // MQTT topics
    private static MqttTopics mqttTopics = null;

    // references for APP
    private static ApplicationConfiguration appConfiguration = null;

    // synchronization object for asynchronous messages incomming from network
    private static final Object synchroNewAsyncMessage = new Object();
    
    // listener object
    private static DPA_IncommingAsyncMessagesListener asyncMessagesListener = null;

    // queue of incomming asynchronous messages
    private static Queue<DPA_AsynchronousMessage> asynchronousMessages = new ConcurrentLinkedQueue<>();

    // async messages processor object 
    private static AsynchronousMessagesProcessor asyncMessagesProcessor = null;

    // timeout to wait for worker threads to join
    private static final long JOIN_WAIT_TIMEOUT = 2000;

    
    // ASYCHRONOUS MESSAGES PROCESSING
    // listener class of incomming asynchronous messages 
    private static class DPA_IncommingAsyncMessagesListener
            implements AsynchronousMessagesListener<DPA_AsynchronousMessage> {

        @Override
        public void onAsynchronousMessage(DPA_AsynchronousMessage message) {
            System.out.println("New asynchronous message arrived.");
            
            synchronized (synchroNewAsyncMessage) {
                OpenGatewayAppLp.asynchronousMessages.add(message);
                synchroNewAsyncMessage.notify();
            }
        }
    }
    
    // processor of asynchronous messages
    private static class AsynchronousMessagesProcessor extends Thread {
        
        @Override
        public void run() {
            
            while ( true ) {
                if ( this.isInterrupted() ) {
                    System.out.println("Worker thread end");
                    return;
                }
                
                synchronized ( synchroNewAsyncMessage ) {
                    while ( asynchronousMessages.isEmpty() ) {
                        try {
                            synchroNewAsyncMessage.wait();
                        } catch ( InterruptedException ex ) {
                            System.out.println(
                                "Asynchronous messages processor thread interrupted "
                                + "while waiting on requests on new messages: " + ex
                            );
                            return;
                        }
                    }
                }
                
                while ( !asynchronousMessages.isEmpty() ) {
                    System.out.println("Async worker thread - message processing");
                    DPA_AsynchronousMessage asyncMessage = asynchronousMessages.poll();
                    processAsynchronousMessage(asyncMessage, mqttTopics);
                }
            }
        }
    }


    // MAIN
    public static void main(String[] args) throws InterruptedException, MqttException {

        // application exit hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

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
            dpaSimply = getDPA_Simply("Simply-CDC.properties");
        }
        else if(appConfiguration.getCommunicationInterface().equalsIgnoreCase("spi")) {
            dpaSimply = getDPA_Simply("Simply-SPI.properties");
        }
        else {
           printMessageAndExit("No supported communication interface: " + appConfiguration.getCommunicationInterface());
        }
        
        // loading MQTT configuration
        MqttConfiguration mqttConfiguration = null;
        try {
            mqttConfiguration = loadMqttConfiguration("Mqtt.json");
        } catch (Exception ex) {
            printMessageAndExit("Error in loading MQTT configuration: " + ex);
        }

        // topics initialization
        mqttTopics =  new MqttTopics.Builder().gwId(mqttConfiguration.getGwId()).build();

        mqttCommunicator = new MqttCommunicator(mqttConfiguration);

        // getting reference to IQRF DPA network to use
        Network dpaNetwork = dpaSimply.getNetwork("1", Network.class);
        if (dpaNetwork == null) {
            printMessageAndExit("DPA Network doesn't exist");
        }

        // reference to map of all nodes in the network
        // getting node 1 - iqhome
        // getting node 2 - citiq
        // getting node 3 - citiq
        // getting node 4 - sleeping switch teco
        // getting node 5 - sleeping switch teco
        // getting node 6 - sleeping switch teco
        Map<String, Node> nodesMap = dpaNetwork.getNodesMap();

        // reference to OS Info
        osInfoMap = getOsInfoFromNodes(nodesMap);
        
        // printing MIDs of nodes in the network
        printMIDs(osInfoMap);
        
        // reference to sensors
        Map<String, DeviceObject> devicesMap = getDevicesMap(nodesMap);

        // initialization of asynchronous messages functionality
        initAsynchronousFunctionality();

        // main application loop
        while ( true ) {
            getAndPublishDevicesData(devicesMap, mqttTopics, osInfoMap);
            Thread.sleep(appConfiguration.getPollingPeriod() * 1000);
        }
    }

    // initializes asynchronous-related functionality
    private static void initAsynchronousFunctionality() {
        asyncMessagesProcessor = new AsynchronousMessagesProcessor();
        asyncMessagesProcessor.start();

        asyncMessagesListener = new DPA_IncommingAsyncMessagesListener();
        dpaSimply.getAsynchronousMessagingManager().registerAsyncMsgListener(asyncMessagesListener);
    }
        
    // terminates asynchronous messages processor thread
    private static void terminateAsyncMessagesProcessorThread() {
        
        // termination signal to async processor thread
        asyncMessagesProcessor.interrupt();
        
        // indicates, wheather this thread is interrupted
        boolean isInterrupted = false;
         
        try {
            if ( asyncMessagesProcessor.isAlive() ) {
                asyncMessagesProcessor.join(JOIN_WAIT_TIMEOUT);
            }
        } catch ( InterruptedException e ) {
            isInterrupted = true;
        }
        
        if ( !asyncMessagesProcessor.isAlive() ) {
            System.out.println("Asynchronous messages processor thread stopped.");
        }
        
        if ( isInterrupted ) {
            Thread.currentThread().interrupt();
        }
    }
    
    // release asynchronous-related functionality resources
    private static void releaseAsynchronousFunctionalityResources() {
        
        if ( asyncMessagesListener != null ) {
            dpaSimply.getAsynchronousMessagingManager().unregisterAsyncMsgListener(asyncMessagesListener);
            asyncMessagesListener = null;
        }
        
        if ( asyncMessagesProcessor != null ) {
            terminateAsyncMessagesProcessorThread();
        }
        
        asynchronousMessages.clear();
    }

    // processes specified asynchronous message
    private static void processAsynchronousMessage(
            DPA_AsynchronousMessage dpaAsyncMessage, MqttTopics mqttTopics
    ) {

        AsyncDataForMqtt asyncDataForMqtt = null;

        String mqttTopic = null;
        String mqttMessage = null;
        
        try {
            asyncDataForMqtt = AsyncDataForMqttCreator.create(dpaAsyncMessage, null);
            System.out.println("Async node id: " + asyncDataForMqtt.getNodeId());

        } catch (AsyncDataForMqttCreatorException ex) {
            System.err.println("Error while creating async message: " + ex.getMessage());
            return;
        }

        if (asyncDataForMqtt.getNodeId().equals("4")) {
            
            mqttTopic =  mqttTopics.getLpActuatorsTeco();
            mqttMessage = MqttFormatter.formatAsyncDataForMqtt(asyncDataForMqtt, 0);
            
            // inform web gui about event
            publishMqttMessage(mqttTopic, mqttMessage);
            System.out.println("Async Topic: " + mqttTopic);
            System.out.println("Async Message: " + mqttMessage);
        
            // app logic
            mqttTopic =  mqttTopics.getStdActuatorsDevtech();
            if(asyncDataForMqtt.getModuleState().equals("leftup")) {
                mqttMessage = MqttFormatter.formatDeviceDevtech("on", 0);
            }
            else if (asyncDataForMqtt.getModuleState().equals("leftdown")) {
                mqttMessage = MqttFormatter.formatDeviceDevtech("off", 0);
            }
            
            // based on async event it sends request to std network via broker 
            publishMqttMessage(mqttTopic, mqttMessage);
            System.out.println("Async Topic: " + mqttTopic);
            System.out.println("Async Message: " + mqttMessage);
        }
        
        if (asyncDataForMqtt.getNodeId().equals("5")) {
            
            mqttTopic =  mqttTopics.getLpActuatorsTeco();
            mqttMessage = MqttFormatter.formatAsyncDataForMqtt(asyncDataForMqtt, 0);
            
            // inform web gui about event
            publishMqttMessage(mqttTopic, mqttMessage);
            System.out.println("Async Topic: " + mqttTopic);
            System.out.println("Async Message: " + mqttMessage);
                   
            // app logic
            mqttTopic =  mqttTopics.getStdActuatorsDatmolux();
            if(asyncDataForMqtt.getModuleState().equals("leftup")) {
                mqttMessage = MqttFormatter.formatDeviceDatmolux("on", 0);
            }
            else if (asyncDataForMqtt.getModuleState().equals("leftdown")) {
                mqttMessage = MqttFormatter.formatDeviceDatmolux("off", 0);
            }
            else if(asyncDataForMqtt.getModuleState().equals("rightup")) {
                mqttMessage = MqttFormatter.formatDeviceDatmolux("up", 0);
            }
            else if (asyncDataForMqtt.getModuleState().equals("rightdown")) {
                mqttMessage = MqttFormatter.formatDeviceDatmolux("down", 0);
            }
            
            // based on async event it sends request to std network via broker 
            publishMqttMessage(mqttTopic, mqttMessage);
            System.out.println("Async Topic: " + mqttTopic);
            System.out.println("Async Message: " + mqttMessage);
        }
        
        if (asyncDataForMqtt.getNodeId().equals("6")) {
            
            mqttTopic =  mqttTopics.getLpActuatorsTeco();
            mqttMessage = MqttFormatter.formatAsyncDataForMqtt(asyncDataForMqtt, 0);
            
            // inform web gui about event
            publishMqttMessage(mqttTopic, mqttMessage);
            System.out.println("Async Topic: " + mqttTopic);
            System.out.println("Async Message: " + mqttMessage);
            
            // app logic
            if(asyncDataForMqtt.getModuleState().equals("leftup")) {
                mqttTopic =  mqttTopics.getStdActuatorsAustyn();
                mqttMessage = MqttFormatter.formatDeviceAustyn("on", 0);
            }
            else if (asyncDataForMqtt.getModuleState().equals("leftdown")) {
                mqttTopic =  mqttTopics.getStdActuatorsAustyn();
                mqttMessage = MqttFormatter.formatDeviceAustyn("off", 0);
            }
            else if(asyncDataForMqtt.getModuleState().equals("rightup")) {
                mqttTopic =  mqttTopics.getStdActuatorsTeco();
                mqttMessage = MqttFormatter.formatDeviceTeco("on", 0);
            }
            else if (asyncDataForMqtt.getModuleState().equals("rightdown")) {
                mqttTopic =  mqttTopics.getStdActuatorsTeco();
                mqttMessage = MqttFormatter.formatDeviceTeco("off", 0);
            }
            
            // based on async event it sends request to std network via broker 
            publishMqttMessage(mqttTopic, mqttMessage);
            System.out.println("Async Topic: " + mqttTopic);
            System.out.println("Async Message: " + mqttMessage);
        }
    }
    
    // gets data from sensors and publishes them
    /*
         task:
         1. Obtain data from devices.
         2. Creation of MQTT form of obtained device's data. 
         3. Sending MQTT form of device's data through MQTT to destination point.
    */
    private static void getAndPublishDevicesData(
            Map<String, DeviceObject> devicesMap, MqttTopics mqttTopics,
            Map<String, OsInfo> osInfoMap
    ) {
        Map<String, Object> dataFromDevicesMap = getDataFromDevices(devicesMap, mqttTopics);

        // getting MQTT form of data from sensors
        Map<String, List<String>> dataFromDevicesMqtt = toMqttForm(dataFromDevicesMap, osInfoMap);

        // sending data
        mqttSendAndPublish(dataFromDevicesMqtt, mqttTopics);
    }

    // init dpa simply
    private static DPA_Simply getDPA_Simply(String configFile) {
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
        return ( nodeId >0 && nodeId <= appConfiguration.getNumberOfDevices() );
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

            // OS peripheral
            OS os = entry.getValue().getDeviceObject(OS.class);
            
            if ( os != null ) {
                // get OS info about module
                OsInfo osInfo = os.read();
                if ( osInfo != null ) {
                    osInfoMap.put(entry.getKey(), osInfo);
                } else {
                    CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
                    if ( procState == ERROR ) {
                        // general call error    
                        CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                        System.err.println("Getting OS info failed: " + error);
                        
                        if ( error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL ) {
                            // specific call error
                            DPA_AdditionalInfo dpaAddInfo = os.getDPA_AdditionalInfoOfLastCall();
                            if ( dpaAddInfo != null ) {
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                System.err.println("Getting OS info failed on the node, DPA error: " + dpaResponseCode); 
                            }
                        }
                    } else {
                        System.err.println("Getting OS info hasn't been processed yet: " + procState);
                    }
                }
            } else {
                System.err.println("OS doesn't exist on node");
            }
        }
        
        return osInfoMap;
    }
    
    // prints MIDs of specified nodes in the map
    private static void printMIDs(Map<String, OsInfo> osInfoMap) {
        for ( Map.Entry<String, OsInfo> entry : osInfoMap.entrySet() ) {
            System.out.println("Node: " + entry.getKey() + " MID: " + entry.getValue().getPrettyFormatedModuleId() );
        }
    }
    
    // returns map of devices from specified map of nodes
    private static Map<String, DeviceObject> getDevicesMap(Map<String, Node> nodesMap) {
        Map<String, DeviceObject> devicesMap = new LinkedHashMap<>();
        
        for ( Map.Entry<String, Node> entry : nodesMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            // node ID must be within valid interval
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            System.out.println("Getting device: " + entry.getKey());
            DeviceInfo deviceInfo = appConfiguration.getDevicesInfoMap().get(nodeId);

            switch ( deviceInfo.getType() ) {
                case "custom":
                    Custom custom = entry.getValue().getDeviceObject(Custom.class);
                    if ( custom != null ) {
                        devicesMap.put(entry.getKey(), (DeviceObject) custom);
                        System.out.println("Device type: " + deviceInfo.getType());
                    } else {
                        System.err.println("Custom device peripheral not found on node: " + nodeId);
                    }
                break;

                default:
                    printMessageAndExit("Device type not supported:" + deviceInfo.getType());
                break;
            }
        }
        
        return devicesMap;
    }
    
    // returns data from devices as specicied by map
    private static Map<String, Object> getDataFromDevices(
            Map<String, DeviceObject> devicesMap, MqttTopics mqttTopics 
    ) {
        
        // data from devices
        Map<String, Object> dataFromDevices = new HashMap<>();
        
        // mqtt data for 1 sensor
        List<DPA_Result> deviceData = new LinkedList<>();
        
        for ( Map.Entry<String, DeviceObject> entry : devicesMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            // node ID must be within valid interval
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            DeviceInfo deviceInfo = appConfiguration.getDevicesInfoMap().get(nodeId);
            System.out.println("Getting data from device: " + entry.getKey());

            switch ( deviceInfo.getType() ) {
                case "custom":
                    DeviceObject devObject = entry.getValue();
                    if ( devObject == null ) {
                        System.err.println("Device not found. Id: " + entry.getKey());
                        break;
                    }
                    
                    if ( !(devObject instanceof Custom) ) {
                        System.err.println("Bad type of device. Got: " + devObject.getClass() 
                            + ", expected: " + Custom.class
                        );
                        break;
                    }

                    // iqhome device
                    short peripheralIdIqhome = 0x20;
                    short cmdIdTemp = 0x10;
                    short cmdIdHum = 0x11;
                    short[] data = new short[]{};

                    // custom dpa peripheral
                    Custom custom = (Custom)devObject;

                    // getting temperature, 3 attempts
                    for (int i = 0; i < 3; i++) {
                        System.out.println("Getting temperature, attempt: " + i);
                        short[] tempData = custom.send(peripheralIdIqhome, cmdIdTemp, data);

                        if ( tempData != null) {
                            DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                            DPA_Result dpaCR = new DPA_Result(tempData, null, dpaAddInfo, null);
                            deviceData.add(dpaCR);
                            break;
                        } else {
                            CallRequestProcessingState requestState = custom.getCallRequestProcessingStateOfLastCall();
                            if ( requestState == ERROR ) {       
                                // call error    
                                CallRequestProcessingError error = custom.getCallRequestProcessingErrorOfLastCall();
                                System.err.println("Error while getting data from custom iqhome device: " + error);

                                String mqttError = MqttFormatter.formatError( String.valueOf(error) );
                                mqttPublishErrors(nodeId, mqttTopics, mqttError);

                                // specific call error
                                if (error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL) {
                                    DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                                    if ( dpaAddInfo != null ) {
                                        DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                        System.err.println("DPA reponse code: " + dpaResponseCode); 
                                    }
                                }
                            } else {
                                System.err.println(
                                    "Could not get data from custom iqhome device. State of the device: " + requestState
                                );
                            }
                        }
                    }
                    
                    // getting humidity, 3 attempts
                    for (int i = 0; i < 3; i++) {
                        System.out.println("Getting humidity, attempt: " + i);
                        short[] humData = custom.send(peripheralIdIqhome, cmdIdHum, data);

                        if ( humData != null) {
                            DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                            DPA_Result dpaCR = new DPA_Result(humData, null, dpaAddInfo, null);
                            deviceData.add(dpaCR);
                            break;
                        } else {
                            CallRequestProcessingState requestState = custom.getCallRequestProcessingStateOfLastCall();
                            if ( requestState == ERROR ) {       
                                // call error    
                                CallRequestProcessingError error = custom.getCallRequestProcessingErrorOfLastCall();
                                System.err.println("Error while getting data from custom iqhome device: " + error);

                                String mqttError = MqttFormatter.formatError( String.valueOf(error) );
                                mqttPublishErrors(nodeId, mqttTopics, mqttError);

                                // specific call error
                                if (error.getErrorType() == CallRequestProcessingErrorType.NETWORK_INTERNAL) {
                                    DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                                    if ( dpaAddInfo != null ) {
                                        DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                        System.err.println("DPA reponse code: " + dpaResponseCode); 
                                    }
                                }
                            } else {
                                System.err.println(
                                    "Could not get data from custom iqhome device. State of the device: " + requestState
                                );
                            }
                        }
                    }
                    
                    if(!deviceData.isEmpty() && (deviceData.size() == 2)) {
                        dataFromDevices.put(entry.getKey(), deviceData);
                    }
                break;

                default:
                    printMessageAndExit("Device type not supported:" + deviceInfo.getType());
                break;
            }
        }
        
        return dataFromDevices;
    }
    
    // for specified sensor's data returns their equivalent MQTT form
    private static Map<String, List<String>> toMqttForm(
            Map<String, Object> dataFromDevicesMap, Map<String, OsInfo> osInfoMap
    ) {
        Map<String, List<String>> mqttAllDevicesData = new LinkedHashMap<>();
        
        // for each sensor's data
        for ( Map.Entry<String, Object> entry : dataFromDevicesMap.entrySet() ) {
            int nodeId = Integer.parseInt(entry.getKey());
            
            if ( !isNodeIdInValidInterval(nodeId) ) {
                continue;
            }
            
            // mqtt data for 1 device
            List<String> mqttDeviceData = new LinkedList<>();
            
            DeviceInfo deviceInfo = appConfiguration.getDevicesInfoMap().get(nodeId);
            System.out.println("Preparing MQTT message for node: " + entry.getKey());
            
            DecimalFormat sensorDataFormat = new DecimalFormat("##.#");
            
            switch ( deviceInfo.getType().toLowerCase() ) {
                case "custom":
                    List<DPA_Result> deviceData = (List<DPA_Result>)entry.getValue();
                    
                    if ( deviceData == null ) {
                        System.err.println(
                            "No data received from device, check log for details "
                            + "about iqhome custom data"
                        );
                        mqttAllDevicesData.put(entry.getKey(), null);
                        break;
                    }
                                       
                    String moduleId = getModuleId(entry.getKey(), osInfoMap);
                    
                    DPA_Result receivedDataTemp = deviceData.get(0);
                    DPA_Result receivedDataHum = deviceData.get(1);
                    
                    short[] receivedTemperature = (short[])receivedDataTemp.getOpResult();
                    short[] receivedHumidity = (short[])receivedDataHum.getOpResult();
                    
                    if ( receivedTemperature.length == 0 && receivedHumidity.length == 0) {
                        System.err.print("No received data from custom on the node " + nodeId);
                        mqttAllDevicesData.put(entry.getKey(), null);
                        break;
                    } else {
                        System.out.print("Received temperature from custom on the node " + nodeId + ": ");
                        for ( Short readResultLoop : receivedTemperature ) {
                            System.out.print(Integer.toHexString(readResultLoop).toUpperCase() + " ");
                        }
                        System.out.println();

                        float temperature = (receivedTemperature[1] << 8) + receivedTemperature[0];
                        temperature = temperature / 16;

                        System.out.print("Received humidity from custom on the node " + nodeId + ": ");
                        for ( Short readResultLoop : receivedHumidity ) {
                            System.out.print(Integer.toHexString(readResultLoop).toUpperCase() + " ");
                        }
                        System.out.println();

                        float humidity = (receivedHumidity[1] << 8) + receivedHumidity[0];
                        humidity = Math.round(humidity / 16);
                        
                        String mqttDataIqhome = MqttFormatter
                                .formatDeviceIqhome(
                                        nodeId,
                                        moduleId,
                                        receivedDataHum.getDpaAddInfo(),
                                        String.valueOf(sensorDataFormat.format(temperature)),
                                        String.valueOf(sensorDataFormat.format(humidity)),
                                        0
                                );

                        mqttDeviceData.add(mqttDataIqhome);
                        mqttAllDevicesData.put(entry.getKey(), mqttDeviceData);
                    }
                    break;

                default:
                    printMessageAndExit("Device type not supported:" + deviceInfo.getType());
                break;    
            }                      
        }
        
        return mqttAllDevicesData;
    }
    
    // returns ID of module for specified sensor ID
    private static String getModuleId(String sensorId, Map<String, OsInfo> osInfoMap) {
        if (osInfoMap.get(sensorId) != null) {
            return osInfoMap.get(sensorId).getPrettyFormatedModuleId();
        }
        return "not-known";
    }

    // sends and publishes prepared json messages with data from sensors to 
    // specified MQTT topics
    private static void mqttSendAndPublish(
            Map<String, List<String>> dataFromsDevicesMqtt, MqttTopics mqttTopics
    ) {
        for (Map.Entry<String, List<String>> entry : dataFromsDevicesMqtt.entrySet()) {
            int nodeId = Integer.parseInt(entry.getKey());

            if (!isNodeIdInValidInterval(nodeId)) {
                continue;
            }

            if (entry.getValue() != null) {
                System.out.println("Sending parsed data for node: " + entry.getKey());

                if(nodeId == 1) {
                    for (String mqttData : entry.getValue()) {
                        
                        System.out.println("IQHome Topic: " + mqttTopics.getLpSensorsIqHome());
                        System.out.println("IQHome Message: " + mqttData);
                        
                        try {
                            mqttCommunicator.publish(mqttTopics.getLpSensorsIqHome(), 0, mqttData.getBytes());
                        } catch (MqttException ex) {
                            System.err.println("Error while publishing sync dpa message: " + ex);
                        }
                    }
                }
            } else {
                System.err.println("No data found for sensor: " + entry.getKey());
            }
        }
    }

    // publish mqtt message
    private static void publishMqttMessage(String topic, String message) {
        if (topic != null && message != null) {
            try {
                mqttCommunicator.publish(topic, 0, message.getBytes());
            } catch (MqttException ex) {
                System.err.println("Error while publishing mqtt message: " + ex.getMessage());
            }
        }
        else {
            System.err.println("Error while publishing mqtt message: topic or message is null");
        }
    }
    
    // publish error messages to specified MQTT topics
    private static void mqttPublishErrors(int nodeId, MqttTopics mqttTopics, String errorMessage) {
        try {
            mqttCommunicator.publish(mqttTopics.getLpSensorsIqHomeErrors() + nodeId, 0, errorMessage.getBytes());
        } catch (MqttException ex) {
            System.err.println("Error while publishing error message: " + ex);
        }
    }

    // loads mqtt params from file
    private static MqttConfiguration loadMqttConfiguration(String configFile)
            throws IOException, ParseException {
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

        // asynchronous messages
        if (asyncMessagesListener != null) {
            dpaSimply.getAsynchronousMessagingManager().unregisterAsyncMsgListener(asyncMessagesListener);
            asyncMessagesListener = null;
        }

        asynchronousMessages.clear();

        // main dpa object
        if (dpaSimply != null) {
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
