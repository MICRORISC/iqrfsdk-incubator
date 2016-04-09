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
import com.microrisc.simply.iqrf.dpa.v22x.devices.IO;
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
public class OpenGatewayTestLp implements AsynchronousMessagesListener<DPA_AsynchronousMessage> {

    // references for DPA

    public static DPA_Simply simply = null;
    public static Network network1 = null;

    public static Node node1 = null;
    public static Node node2 = null;
    public static Node node3 = null;
    public static Node node4 = null;
    public static Node node5 = null;
    public static Node node6 = null;

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
    public static String certFile = null;

    public static String netType = "lp";
    
    public static MQTTCommunicator mqttCommunicator = null;
    public static String webResponseTopic = null;
    public static String webResponseToBeSent = null;
    public static boolean webRequestReceived = false;

    public static int pid = 0;
    public static int pidAsyncCitiq = 0;
    public static int pidAsyncTeco = 0;
    public static int pidAsyncDevtechControl = 0;
    public static int pidAsyncDatmoluxControl = 0;
    public static int pidAsyncAustynControl = 0;
    public static int pidAsyncTecoControl = 0;

    // prints out specified message, destroys the Simply and exits
    private static void printMessageAndExit(String message, boolean exit) {
        System.out.println(message);

        if (exit) {
            if (simply != null) {
                simply.destroy();
            }
            System.exit(1);
        }
    }

    public static void main(String[] args) throws InterruptedException, MqttException {

        // DPA INIT
        try {
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "cdc" + File.separator + "Simply.properties");
        } catch (SimplyException ex) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage(), true);
        }

        // MQTT INIT
        String url = protocol + broker + ":" + port;
        mqttCommunicator = new MQTTCommunicator(url, clientId, cleanSession, quietMode, userName, password, certFile, netType);
        mqttCommunicator.subscribe(MQTTTopics.LP_SETTING_CITIQ, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_DEVTECH, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_DATMOLUX, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_AUSTYN, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_TECO, 2);

        // ASYNC REQUESTS FROM DPA
        final OpenGatewayTestLp msgListener = new OpenGatewayTestLp();

        // getting access to asynchronous messaging manager
        final AsynchronousMessagingManager<DPA_AsynchronousMessage, DPA_AsynchronousMessageProperties> asyncManager
                = simply.getAsynchronousMessagingManager();

        // register the listener of asynchronous messages
        asyncManager.registerAsyncMsgListener(msgListener);

        // APP EXIT
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

            @Override
            public void run() {
                System.out.println("End via shutdown hook.");

                // after end of work with asynchronous messages, unrergister the listener
                asyncManager.unregisterAsyncMsgListener(msgListener);

                // end working with Simply
                simply.destroy();
            }

        }));

        // SYNC REQUESTS TO DPA
        // getting network 1
        network1 = simply.getNetwork("1", Network.class);
        if (network1 == null) {
            printMessageAndExit("Network 1 doesn't exist", true);
        }

        // getting node 1 - iqhome
        node1 = network1.getNode("1");
        if (node1 == null) {
            printMessageAndExit("Node 1 doesn't exist", true);
        }

        // getting node 2 - citiq
        node2 = network1.getNode("2");
        if (node2 == null) {
            printMessageAndExit("Node 2 doesn't exist", true);
        }

        // getting node 3 - citiq
        node3 = network1.getNode("3");
        if (node3 == null) {
            printMessageAndExit("Node 3 doesn't exist", true);
        }

        // getting node 4 - sleeping switch
        node4 = network1.getNode("4");
        if (node4 == null) {
            printMessageAndExit("Node 4 doesn't exist", true);
        }

        // getting node 5 - sleeping switch
        node5 = network1.getNode("5");
        if (node5 == null) {
            printMessageAndExit("Node 5 doesn't exist", true);
        }

        // getting node 6 - sleeping switch
        node6 = network1.getNode("6");
        if (node4 == null) {
            printMessageAndExit("Node 6 doesn't exist", true);
        }

        // getting OS interface
        OS osn1 = node1.getDeviceObject(OS.class);
        if (osn1 == null) {
            printMessageAndExit("OS doesn't exist on node 1", false);
        }

        // get info about module
        osInfoNode1 = osn1.read();
        if (osInfoNode1 == null) {
            CallRequestProcessingState procState = osn1.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = osn1.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 1: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 1: " + procState, false);
            }
        }

        // getting OS interface
        OS osn2 = node2.getDeviceObject(OS.class);
        if (osn2 == null) {
            printMessageAndExit("OS doesn't exist on node 2", false);
        }

        // get info about module
        osInfoNode2 = osn2.read();
        if (osInfoNode2 == null) {
            CallRequestProcessingState procState = osn2.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = osn2.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 2: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 2: " + procState, false);
            }
        }

        // getting OS interface
        OS osn3 = node3.getDeviceObject(OS.class);
        if (osn3 == null) {
            printMessageAndExit("OS doesn't exist on node 3", false);
        }

        // get info about module
        osInfoNode3 = osn3.read();
        if (osInfoNode3 == null) {
            CallRequestProcessingState procState = osn3.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = osn3.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 3: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 3: " + procState, false);
            }
        }

        // getting custom interface
        Custom custom = node1.getDeviceObject(Custom.class);
        if (custom == null) {
            printMessageAndExit("Custom doesn't exist on node 1", true);
        }
        
        int iqHomeHWPID = 0x0212;
        custom.setRequestHwProfile(iqHomeHWPID);

        // getting results
        // set up maximal number of cycles according to your needs - only testing
        //final int MAX_CYCLES = 5000;        
        //for ( int cycle = 0; cycle < MAX_CYCLES; cycle++ ) {
        while (true) {

            // getting actual temperature
            short peripheralIQHome = 0x20;
            short cmdIdTemp = 0x10;
            short cmdIdHum = 0x11;
            short[] data = new short[]{};

            short[] receivedDataTemp = null;
            short[] receivedDataHum = null;

            UUID tempRequestUid = null;
            UUID humRequestUid = null;
            if(custom != null) {
                tempRequestUid = custom.async_send(peripheralIQHome, cmdIdTemp, data);
                humRequestUid = custom.async_send(peripheralIQHome, cmdIdHum, data);
            }
                
            // maximal number of attempts of getting a result
            final int RETRIES = 3;
            int attempt = 0;
            int checkResponse = 0;

            while (attempt++ < RETRIES) {

                // main job is here for now - quick hack to test things first
                while (true) {

                    Thread.sleep(1);
                    checkResponse++;

                    // dpa async task
                    if (asyncRequestReceived) {
                        asyncRequestReceived = false;
                        sendDPAAsyncRequest();
                    }

                    // mqtt web confirmation task
                    if (webRequestReceived) {
                        webRequestReceived = false;

                        if (webResponseTopic != null) {
                            try {
                                mqttCommunicator.publish(webResponseTopic, 2, webResponseToBeSent.getBytes());
                            } catch (MqttException ex) {
                                System.err.println("Error while publishing web response message." + ex.getMessage());
                            }
                        }
                    }

                    // periodic task ever 60s
                    if (checkResponse == 60000) {
                        checkResponse = 0;
                        break;
                    }
                }

                // get request call state
                CallRequestProcessingState procStateTemp = null;
                CallRequestProcessingState procStateHum = null;
                if(custom != null) {
                    procStateTemp = custom.getCallRequestProcessingState(tempRequestUid);
                    procStateHum = custom.getCallRequestProcessingState(humRequestUid);
                }
                    
                // if any error occured
                if(procStateTemp != null) {
                    // if any error occured
                    if (procStateTemp == CallRequestProcessingState.ERROR) {

                        // general call error
                        CallRequestProcessingError error = custom.getCallRequestProcessingError(tempRequestUid);
                        printMessageAndExit("Getting custom temperature failed on node 1: " + error.getErrorType(), false);

                    } else {
                        // have result iqhome
                        if (procStateTemp == CallRequestProcessingState.RESULT_ARRIVED) {
                            receivedDataTemp = custom.getCallResultImmediately(tempRequestUid, short[].class);

                            if (receivedDataTemp != null && receivedDataTemp.length == 0) {

                                // specific call error
                                DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                printMessageAndExit("Getting custom temperature failed on node 1, DPA error: " + dpaResponseCode, false);
                            }
                        } else {
                            System.out.println("Getting custom temperature hasn't been processed yet on node 1: " + procStateTemp);
                        }
                    }
                }

                if(procStateHum != null) {
                    // if any error occured
                    if (procStateHum == CallRequestProcessingState.ERROR) {

                        // general call error
                        CallRequestProcessingError error = custom.getCallRequestProcessingError(humRequestUid);
                        printMessageAndExit("Getting custom humidity failed on node 1: " + error.getErrorType(), false);

                    } else {

                        if (procStateHum == CallRequestProcessingState.RESULT_ARRIVED) {
                            receivedDataHum = custom.getCallResultImmediately(humRequestUid, short[].class);

                            if (receivedDataTemp != null && receivedDataTemp.length == 0) {

                                // specific call error
                                DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                printMessageAndExit("Getting custom humidity failed on node 1, DPA error: " + dpaResponseCode, false);
                            }
                        } else {
                            System.out.println("Getting custom humidity hasn't been processed yet on node 1: " + procStateHum);
                        }
                    }
                    break;
                }
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
                    String iqhomeValuesToBeSent
                            = "{\"e\":["
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
                        System.err.println("Error while publishing sync dpa message on node 1:" + ex.getMessage());
                    }

                    receivedDataTemp = null;
                    receivedDataHum = null;
                }
            } else {
                System.out.println("IQHome Result has not arrived.");
            }
        }

        // after end of work with asynchronous messages, unrergister the listener
        //asyncManager.unregisterAsyncMsgListener(msgListener);
        // end working with Simply
        //simply.destroy();
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
            valueN = element.asObject().getString("n", "");
            valueSV = element.asObject().getString("sv", "");
            //System.out.println("Published action on e element: " + "n:" + valueN + " - " + "sv:" + valueSV);
        }

        // parse senml json msg request       
        elements = Json.parse(msgSenML).asObject().get("iqrf").asArray();

        for (JsonValue element : elements) {
            valuePID = element.asObject().getInt("pid", 0);
            valueDPA = element.asObject().getString("dpa", "");
            valueNADR = element.asObject().getInt("nadr", 0);
            //System.out.println("Published action on iqrf element: " + "pid:" + valuePID + " - " + "dpa:" + valueDPA + " - " + "nadr:" + valueNADR);
        }

        // there is a need to select topic
        if (MQTTTopics.LP_SETTING_CITIQ.equals(topic)) {

            // TODO: check nodeID and add selection
            if (valueDPA.equalsIgnoreCase("REQ")) {

                webResponseTopic = MQTTTopics.LP_SETTING_CITIQ;

                if (valueN.equalsIgnoreCase("CUSTOM")) {

                    if (valueSV.equalsIgnoreCase("RESET")) {

                        if (valueNADR == 0x02) {

                            // getting Custom interface - citiq
                            Custom customCitiq = node2.getDeviceObject(Custom.class);
                            if (customCitiq == null) {
                                printMessageAndExit("Custom doesn't exist on node 2", true);
                            }

                            short[] result = null;
                            int j = 3;
                            do {
                                result = customCitiq.send((short) 0x20, (short) 0x01, new short[]{});
                            } while (result == null && --j > 0);
                            
                            if (result == null) {
                                CallRequestProcessingError error = customCitiq.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting Custom failed on node 2: " + error, false);
                            } else {
                                // getting additional info of the last call
                                DPA_AdditionalInfo dpaAddInfo = customCitiq.getDPA_AdditionalInfoOfLastCall();

                                // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                                webResponseToBeSent
                                        = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"reset\"}],"
                                        + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                                        + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                        + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                        + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                        + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode2.getPrettyFormatedModuleId() + "\""
                                        + "}";

                                webRequestReceived = true;
                                return webResponseToBeSent;
                            }
                        }

                        if (valueNADR == 0x03) {

                            // getting Custom interface - citiq
                            Custom customCitiq = node3.getDeviceObject(Custom.class);
                            if (customCitiq == null) {
                                printMessageAndExit("Custom doesn't exist on node 3", true);
                            }

                            short[] result = null;
                            int j = 3;
                            do {
                                result = customCitiq.send((short) 0x20, (short) 0x01, new short[]{});
                            } while (result == null && --j > 0);                            
                            
                            if (result == null) {
                                CallRequestProcessingError error = customCitiq.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting Custom failed on node 3: " + error, false);
                            } else {
                                // getting additional info of the last call
                                DPA_AdditionalInfo dpaAddInfo = customCitiq.getDPA_AdditionalInfoOfLastCall();

                                // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                                webResponseToBeSent
                                        = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"reset\"}],"
                                        + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                                        + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                        + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                        + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                        + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode3.getPrettyFormatedModuleId() + "\""
                                        + "}";

                                webRequestReceived = true;
                                return webResponseToBeSent;
                            }
                        }
                    }
                }
            }

            // there is a need to select topic
            if (MQTTTopics.STD_ACTUATORS_DEVTECH.equals(topic)) {

                // TODO: check nodeID and add selection
                if (valueDPA.equalsIgnoreCase("RESP")) {
                    // TODO: checking that rcode if correct - request for STD network has been processed correctly
                    // for now there are not sent if there is error
                }
            }
        }

        return null;
    }

    public static boolean sendDPAAsyncRequest() throws InterruptedException {

        if (asyncNodeId == null || asyncMainData == null || asyncAdditionalData == null) {
            System.out.println("Null from Asynchronny on the node " + asyncNodeId);
        } else {

            if (asyncMainData.length == 0) {
                System.out.println("No received data from Asynchronny on the node " + asyncNodeId);
            } else {
                if (asyncNodeId.equals("2")) {

                    pidAsyncCitiq++;
                    String STATE = "unknown";

                    if (asyncMainData[0] == 0) {
                        STATE = "free";
                    } else if (asyncMainData[0] == 1) {
                        STATE = "occupied";
                    }

                    String moduleId = null;
                    if (osInfoNode2 != null) {
                        moduleId = osInfoNode2.getPrettyFormatedModuleId();
                    } else {
                        moduleId = "unknown";
                    }

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String asyncRequestToBeSent
                            = "{\"e\":[{\"n\":\"carplace\"," + "\"sv\":" + "\"" + STATE + "\"}],"
                            + "\"iqrf\":[{\"pid\":" + pidAsyncCitiq + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + asyncAdditionalData.getHwProfile() + "," + "\"rcode\":" + "\"" + asyncAdditionalData.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + asyncAdditionalData.getDPA_Value() + "}],"
                            + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.LP_STATUS_CITIQ, 2, asyncRequestToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message from node 2: " + ex.getMessage());
                    }
                }

                if (asyncNodeId.equals("3")) {

                    pidAsyncCitiq++;
                    String STATE = "unknown";

                    if (asyncMainData[0] == 0) {
                        STATE = "free";
                    } else if (asyncMainData[0] == 1) {
                        STATE = "occupied";
                    }

                    String moduleId = null;
                    if (osInfoNode3 != null) {
                        moduleId = osInfoNode3.getPrettyFormatedModuleId();
                    } else {
                        moduleId = "unknown";
                    }

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String asyncRequestToBeSent
                            = "{\"e\":[{\"n\":\"carplace\"," + "\"sv\":" + "\"" + STATE + "\"}],"
                            + "\"iqrf\":[{\"pid\":" + pidAsyncCitiq + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + asyncAdditionalData.getHwProfile() + "," + "\"rcode\":" + "\"" + asyncAdditionalData.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + asyncAdditionalData.getDPA_Value() + "}],"
                            + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.LP_STATUS_CITIQ, 2, asyncRequestToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message from node 3: " + ex.getMessage());
                    }
                }

                if (asyncNodeId.equals("4")) {

                    pidAsyncTeco++;

                    String STATE = null;
                    String devtechControl = null;

                    if ((asyncMainData[0] & 0x01) == 1) {
                        STATE = "up";
                    } else if ((asyncMainData[0] & 0x02) == 0x02) {
                        STATE = "down";
                    } else {
                        STATE = "unknown";
                    }

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String asyncRequestToBeSent
                            = "{\"e\":[{\"n\":\"switch\"," + "\"sv\":" + "\"" + STATE + "\"}],"
                            + "\"iqrf\":[{\"pid\":" + pidAsyncTeco + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + asyncAdditionalData.getHwProfile() + "," + "\"rcode\":" + "\"" + asyncAdditionalData.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + asyncAdditionalData.getDPA_Value() + "}],"
                            + "\"bn\":" + "\"urn:dev:mid:" + "unknown" + "\""
                            + "}";

                    int devtechNodeId = 0x03;
                    int devtechHWPID = 0xFFFF;
                    String devtechModuleId = "8100401F";

                    if (STATE.equals("up")) {
                        pidAsyncDevtechControl++;

                        devtechControl
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"on\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncDevtechControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + devtechNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + devtechHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + devtechModuleId + "\""
                                + "}";
                    } else if (STATE.equals("down")) {
                        pidAsyncDevtechControl++;

                        devtechControl
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"off\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncDevtechControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + devtechNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + devtechHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + devtechModuleId + "\""
                                + "}";
                    }

                    // send data to mqtt
                    try {
                        if (STATE != null) {
                            mqttCommunicator.publish(MQTTTopics.LP_ACTUATORS_TECO, 2, asyncRequestToBeSent.getBytes());
                        }

                        if (devtechControl != null) {
                            mqttCommunicator.publish(MQTTTopics.STD_ACTUATORS_DEVTECH, 2, devtechControl.getBytes());
                        }
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message from node 4: " + ex.getMessage());
                    }
                }

                if (asyncNodeId.equals("5")) {

                    pidAsyncTeco++;

                    String STATE = null;
                    String datmoluxControl = null;

                    if ((asyncMainData[0] & 0x01) == 1) {
                        STATE = "left up";
                    } else if ((asyncMainData[0] & 0x02) == 0x02) {
                        STATE = "left down";
                    } else if ((asyncMainData[0] & 0x04) == 0x04) {
                        STATE = "right up";
                    } else if ((asyncMainData[0] & 0x08) == 0x08) {
                        STATE = "right down";
                    } else {
                        STATE = "unknown";
                    }

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String asyncRequestToBeSent
                            = "{\"e\":[{\"n\":\"switch\"," + "\"sv\":" + "\"" + STATE + "\"}],"
                            + "\"iqrf\":[{\"pid\":" + pidAsyncTeco + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node5.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + asyncAdditionalData.getHwProfile() + "," + "\"rcode\":" + "\"" + asyncAdditionalData.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + asyncAdditionalData.getDPA_Value() + "}],"
                            + "\"bn\":" + "\"urn:dev:mid:" + "unknown" + "\""
                            + "}";

                    int datmoluxNodeId = 0x04;
                    int datmoluxHWPID = 0xFFFF;
                    String datmoluxModuleId = "8100401F";

                    if (STATE.equals("left up")) {
                        pidAsyncDatmoluxControl++;

                        datmoluxControl
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"on\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncDevtechControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + datmoluxNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + datmoluxHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + datmoluxModuleId + "\""
                                + "}";
                    } else if (STATE.equals("left down")) {
                        pidAsyncDatmoluxControl++;

                        datmoluxControl
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"off\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncDevtechControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + datmoluxNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + datmoluxHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + datmoluxModuleId + "\""
                                + "}";
                    } else if (STATE.equals("right up")) {
                        pidAsyncDatmoluxControl++;

                        datmoluxControl
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"up\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncDevtechControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + datmoluxNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + datmoluxHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + datmoluxModuleId + "\""
                                + "}";
                    } else if (STATE.equals("right down")) {
                        pidAsyncDatmoluxControl++;

                        datmoluxControl
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"down\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncDevtechControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + datmoluxNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + datmoluxHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + datmoluxModuleId + "\""
                                + "}";
                    }

                    // send data to mqtt
                    try {
                        if (STATE != null) {
                            mqttCommunicator.publish(MQTTTopics.LP_ACTUATORS_TECO, 2, asyncRequestToBeSent.getBytes());
                        }

                        if (datmoluxControl != null) {
                            mqttCommunicator.publish(MQTTTopics.STD_ACTUATORS_DATMOLUX, 2, datmoluxControl.getBytes());
                        }
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message from node 5: " + ex.getMessage());
                    }
                }

                if (asyncNodeId.equals("6")) {

                    pidAsyncTeco++;

                    String STATE = null;
                    String austynControl = null;
                    String tecoControl = null;

                    if ((asyncMainData[0] & 0x01) == 1) {
                        STATE = "left up";
                    } else if ((asyncMainData[0] & 0x02) == 0x02) {
                        STATE = "left down";
                    } else if ((asyncMainData[0] & 0x04) == 0x04) {
                        STATE = "right up";
                    } else if ((asyncMainData[0] & 0x08) == 0x08) {
                        STATE = "right down";
                    } else {
                        STATE = "unknown";
                    }

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String asyncRequestToBeSent
                            = "{\"e\":[{\"n\":\"switch\"," + "\"sv\":" + "\"" + STATE + "\"}],"
                            + "\"iqrf\":[{\"pid\":" + pidAsyncTeco + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node6.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + asyncAdditionalData.getHwProfile() + "," + "\"rcode\":" + "\"" + asyncAdditionalData.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + asyncAdditionalData.getDPA_Value() + "}],"
                            + "\"bn\":" + "\"urn:dev:mid:" + "unknown" + "\""
                            + "}";

                    int austynNodeId = 0x02;
                    int austynHWPID = 0xFFFF;
                    String austynModuleId = "8100401F";

                    int tecoNodeId = 0x05;
                    int tecoHWPID = 0xFFFF;
                    String tecoModuleId = "8100401F";

                    if (STATE.equals("left up")) {
                        pidAsyncAustynControl++;

                        austynControl
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"on\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncAustynControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + austynNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + austynHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + austynModuleId + "\""
                                + "}";
                    } else if (STATE.equals("left down")) {
                        pidAsyncAustynControl++;

                        austynControl
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"off\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncAustynControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + austynNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + austynHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + austynModuleId + "\""
                                + "}";
                    } else if (STATE.equals("right up")) {
                        pidAsyncTecoControl++;

                        tecoControl
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"on\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncTecoControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + tecoNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + tecoHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + tecoModuleId + "\""
                                + "}";
                    } else if (STATE.equals("right down")) {
                        pidAsyncTecoControl++;

                        tecoControl
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"off\"}],"
                                + "\"iqrf\":[{\"pid\":" + pidAsyncTecoControl + "," + "\"dpa\":\"req\"," + "\"nadr\":" + tecoNodeId + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + tecoHWPID + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + tecoModuleId + "\""
                                + "}";
                    }

                    // send data to mqtt
                    try {
                        if (STATE != null) {
                            mqttCommunicator.publish(MQTTTopics.LP_ACTUATORS_TECO, 2, asyncRequestToBeSent.getBytes());
                        }

                        if (austynControl != null) {
                            mqttCommunicator.publish(MQTTTopics.STD_ACTUATORS_AUSTYN, 2, austynControl.getBytes());
                        }

                        if (tecoControl != null) {
                            mqttCommunicator.publish(MQTTTopics.STD_ACTUATORS_TECO, 2, tecoControl.getBytes());
                        }
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message from node 6: " + ex.getMessage());
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
        asyncMainData = (short[]) message.getMainData();
        asyncAdditionalData = (DPA_AdditionalInfo) message.getAdditionalData();

        // sending control message back to network based on received message
        asyncRequestReceived = true;
    }
}
