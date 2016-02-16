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
import com.microrisc.simply.iqrf.dpa.v22x.devices.GeneralLED;
import com.microrisc.simply.iqrf.dpa.v22x.devices.IO;
import com.microrisc.simply.iqrf.dpa.v22x.devices.LEDR;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Custom;
import com.microrisc.simply.iqrf.dpa.v22x.devices.UART;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_Command;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_DirectionSettings;
import com.microrisc.simply.iqrf.dpa.v22x.types.IO_OutputValueSettings;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import com.microrisc.simply.iqrf.types.VoidType;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Running first tests with DPA <-> MQTT.
 *
 * @author Rostislav Spinar
 */
public class OpenGatewayTest implements AsynchronousMessagesListener<DPA_AsynchronousMessage> {

    // references for DPA

    public static DPA_Simply simply = null;
    public static Network network1 = null;

    public static Node node1 = null;
    public static Node node2 = null;
    public static Node node3 = null;
    public static Node node4 = null;
    public static OsInfo osInfoNode1 = null;
    public static OsInfo osInfoNode2 = null;
    public static OsInfo osInfoNode3 = null;
    public static OsInfo osInfoNode4 = null;

    public static boolean asyncRequestReceived = false;

    // references for MQTT
    public static String protocol = "tcp://";
    public static String broker = "localhost";
    public static int port = 1883;

    public static String clientId = "b827eb26c73d-std";
    public static boolean cleanSession = true;
    public static boolean quietMode = false;
    public static boolean ssl = false;

    public static String password = null;
    public static String userName = null;

    public static MQTTCommunicator mqttCommunicator = null;
    public static String webResponseToBeSent = null;
    public static String webResponseTopic = null;
    public static boolean webRequestReceived = false;

    public static int pidProtronix = 0;
    public static int pidAustyn = 0;
    public static int pidDevtech = 0;
    public static int pidDatmolux = 0;
    public static int pidAsync = 0;
    
    // data for datmolux
    private static short peripheralDatmolux = 0x20;
    private static short cmdIdDatmoOFF = 0x00;
    private static short cmdIdDatmoON = 0x01;
    private static short cmdIdDatmoDOWN = 0x02;
    private static short cmdIdDatmoUP = 0x03;
    private static short cmdIdDatmoPOWER = 0x06;
    private static short[] dataDatmo = new short[]{};
    private static Short[] receivedCustomDatmoluxData = null;

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
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "spi" + File.separator + "Simply.properties");
        } catch (SimplyException ex) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage(), true);
        }

        // MQTT INIT
        String url = protocol + broker + ":" + port;
        mqttCommunicator = new MQTTCommunicator(url, clientId, cleanSession, quietMode, userName, password);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_AUSTYN, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_DEVTECH, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_DATMOLUX, 2);

        // ASYNC REQUESTS FROM DPA
        OpenGatewayTest msgListener = new OpenGatewayTest();

        // getting access to asynchronous messaging manager
        AsynchronousMessagingManager<DPA_AsynchronousMessage, DPA_AsynchronousMessageProperties> asyncManager
                = simply.getAsynchronousMessagingManager();

        // register the listener of asynchronous messages
        asyncManager.registerAsyncMsgListener(msgListener);

        // SYNC REQUESTS TO DPA
        // getting network 1
        network1 = simply.getNetwork("1", Network.class);
        if (network1 == null) {
            printMessageAndExit("Network 1 doesn't exist", true);
        }

        // getting node 1 - protronix
        node1 = network1.getNode("1");
        if (node1 == null) {
            printMessageAndExit("Node 1 doesn't exist", true);
        }

        // getting node 2 - austyn
        node2 = network1.getNode("2");
        if (node2 == null) {
            printMessageAndExit("Node 2 doesn't exist", true);
        }

        // getting node 3 - devtech
        node3 = network1.getNode("3");
        if (node3 == null) {
            printMessageAndExit("Node 3 doesn't exist", true);
        }

        // getting node 4 - datmolux
        node4 = network1.getNode("4");
        if (node4 == null) {
            printMessageAndExit("Node 4 doesn't exist", true);
        }

        // getting OS interface
        OS os = node1.getDeviceObject(OS.class);
        if (os == null) {
            printMessageAndExit("OS doesn't exist on node 1", false);
        }

        // get info about module
        osInfoNode1 = os.read();
        if (osInfoNode1 == null) {
            CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node1: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node1: " + procState, false);
            }
        }

        // getting OS interface
        os = node2.getDeviceObject(OS.class);
        if (os == null) {
            printMessageAndExit("OS doesn't exist on node 2", false);
        }

        // get info about module
        osInfoNode2 = os.read();
        if (osInfoNode2 == null) {
            CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 2: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 2: " + procState, false);
            }
        }

        // getting OS interface
        os = node3.getDeviceObject(OS.class);
        if (os == null) {
            printMessageAndExit("OS doesn't exist on node 3", false);
        }

        // get info about module
        osInfoNode3 = os.read();
        if (osInfoNode3 == null) {
            CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 3: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 3: " + procState, false);
            }
        }

        // getting OS interface
        os = node4.getDeviceObject(OS.class);
        if (os == null) {
            printMessageAndExit("OS doesn't exist on node 4", false);
        }

        // get info about module
        osInfoNode4 = os.read();
        if (osInfoNode4 == null) {
            CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 4: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 4: " + procState, false);
            }
        }

        // getting UART interface - protronix
        UART uartP = node1.getDeviceObject(UART.class);
        if (uartP == null) {
            printMessageAndExit("UART doesn't exist on node 1", true);
        }

        // getting Custom interface - austyn
        Custom customAustyn = node2.getDeviceObject(Custom.class);
        if (customAustyn == null) {
            printMessageAndExit("Custom doesn't exist on node 2", true);
        }

        // getting UART interface - devtech
        UART uartD = node3.getDeviceObject(UART.class);
        if (uartD == null) {
            printMessageAndExit("UART doesn't exist on node 3", true);
        }

        // getting Custom interface - datmolux
        Custom customDatmolux = node4.getDeviceObject(Custom.class);
        if (customDatmolux == null) {
            printMessageAndExit("Custom doesn't exist on node 4", true);
        }

        // getting results
        // set up maximal number of cycles according to your needs - only testing
        final int MAX_CYCLES = 5000;
        for (int cycle = 0; cycle < MAX_CYCLES; cycle++) {

            // after 100ms reading
            int timeoutP = 0x0A;
            short[] dataP = {0x47, 0x44, 0x03};
            short[] receivedUARTPData = null;

            // getting austyn temperature
            short peripheralAustyn = 0x20;
            short cmdIdTemp = 0x01;
            short[] dataTemp = new short[]{};
            Short[] receivedDataTemp = null;
            
            // after 2390ms reading
            int timeoutD = 0xEF;
            short[] dataD = {0x65, 0xFD};
            short[] receivedUARTDData = null;

            // write UART peripheral
            //Thread.sleep(100);
            //TODO: set hwpid
            UUID uartPRequestUid = uartP.async_writeAndRead(timeoutP, dataP);

            // send Custom 
            //Thread.sleep(100);
            //TODO: set hwpid
            UUID tempRequestUid = customAustyn.async_send(peripheralAustyn, cmdIdTemp, dataTemp);

            // write UART peripheral
            uartD.setDefaultWaitingTimeout(5000);
            //Thread.sleep(100);
            //TODO: set hwpid
            UUID uartDRequestUid = uartD.async_writeAndRead(timeoutD, dataD);

            // send Custom 
            //Thread.sleep(100);
            //TODO: set hwpid
            UUID datmoPowerRequestUid = customDatmolux.async_send(peripheralDatmolux, cmdIdDatmoPOWER, dataDatmo);
            
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
                                System.err.println("Error while publishing web response message.");
                            }
                        }
                    }

                    // periodic task ever 15s
                    if (checkResponse == 1500) {
                        checkResponse = 0;
                        break;
                    }
                }

                // get request call state
                CallRequestProcessingState procStateP = uartP.getCallRequestProcessingState(uartPRequestUid);
                CallRequestProcessingState procStateTemp = customAustyn.getCallRequestProcessingState(tempRequestUid);
                CallRequestProcessingState procStateD = uartD.getCallRequestProcessingState(uartDRequestUid);
                CallRequestProcessingState procStatePower = customDatmolux.getCallRequestProcessingState(datmoPowerRequestUid);

                // if any error occured
                if (procStateP == CallRequestProcessingState.ERROR) {

                    // general call error
                    CallRequestProcessingError error = uartP.getCallRequestProcessingError(uartPRequestUid);
                    printMessageAndExit("Getting UART data failed on node1: " + error.getErrorType(), false);

                    // specific call error
                    //DPA_AdditionalInfo dpaAddInfo = uartP.getDPA_AdditionalInfoOfLastCall();
                    //DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                    //printMessageAndExit("Getting UART data failed on node1: " + error + ", DPA error: " + dpaResponseCode, false);
                } else {
                    // have result already - protronix
                    if (procStateP == CallRequestProcessingState.RESULT_ARRIVED) {
                        receivedUARTPData = uartP.getCallResultImmediately(uartPRequestUid, short[].class);
                    } else {
                        System.out.println("Getting UART data on node 1 hasn't been processed yet: " + procStateP);
                    }
                }

                // if any error occured
                if (procStateTemp == CallRequestProcessingState.ERROR) {

                    // general call error
                    CallRequestProcessingError error = customAustyn.getCallRequestProcessingError(tempRequestUid);
                    printMessageAndExit("Getting Custom data failed on node2: " + error.getErrorType(), false);

                    // specific call error
                    //DPA_AdditionalInfo dpaAddInfo = custom.getDPA_AdditionalInfoOfLastCall();
                    //DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                    //printMessageAndExit("Getting Custom data failed on node2: " + error + ", DPA error: " + dpaResponseCode, false);
                } else {
                    // have result already - austyn
                    if (procStateTemp == CallRequestProcessingState.RESULT_ARRIVED) {
                        receivedDataTemp = customAustyn.getCallResultImmediately(tempRequestUid, Short[].class);
                    } else {
                        System.out.println("Getting Custom data on node 2 hasn't been processed yet: " + procStateTemp);
                    }
                }

                // if any error occured
                if (procStateD == CallRequestProcessingState.ERROR) {

                    // general call error
                    CallRequestProcessingError error = uartD.getCallRequestProcessingError(uartDRequestUid);
                    printMessageAndExit("Getting UART data failed on node3: " + error.getErrorType(), false);

                    // specific call error
                    //DPA_AdditionalInfo dpaAddInfo = uartD.getDPA_AdditionalInfoOfLastCall();
                    //DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                    //printMessageAndExit("Getting UART data failed on node3: " + error + ", DPA error: " + dpaResponseCode, false);
                } else {
                    // have result already - devtech
                    if (procStateD == CallRequestProcessingState.RESULT_ARRIVED) {
                        receivedUARTDData = uartD.getCallResultImmediately(uartDRequestUid, short[].class);
                    } else {
                        System.out.println("Getting UART data on node3 hasn't been processed yet: " + procStateD);
                    }
                }

                // if any error occured
                if (procStatePower == CallRequestProcessingState.ERROR) {

                    // general call error
                    CallRequestProcessingError error = customDatmolux.getCallRequestProcessingError(datmoPowerRequestUid);
                    printMessageAndExit("Getting Custom data failed on node 4: " + error.getErrorType(), false);

                    // specific call error
                    //DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();
                    //DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                    //printMessageAndExit("Getting Custom data failed on node 4: " + error + ", DPA error: " + dpaResponseCode, false);
                } else {
                    // have result already - datmolux
                    if (procStateD == CallRequestProcessingState.RESULT_ARRIVED) {
                        receivedCustomDatmoluxData = customDatmolux.getCallResultImmediately(datmoPowerRequestUid, Short[].class);
                    } else {
                        System.out.println("Getting Custom data on node 4 hasn't been processed yet: " + procStatePower);
                    }
                }
                break;
            }

            if (receivedUARTPData != null) {
                
                if (receivedUARTPData.length == 0) {
                    System.out.print("No received data from UART on the node " + node1.getId());
                } else {

                    pidProtronix++;

                    // fixed
                    String protronixDPARequest = new String("01.00.0C.02.FF.FF.0A.47.44.03");

                    // header ... 1, 0, 12, 130, 0, 0, 0, 63 & data ... 6, 3, 1, 175, 0, 219, 112
                    String protronixDPAResponsesHeader = new String("01.00.0C.82.");

                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = uartP.getDPA_AdditionalInfoOfLastCall();

                    StringBuilder protronixResponse = new StringBuilder();

                    //protronixResponse.append(Integer.toHexString(dpaAddInfo.getHwProfile() & 0x00FF).toUpperCase());
                    protronixResponse.append(String.format("%02X", dpaAddInfo.getHwProfile() & 0x00FF ));
                    protronixResponse.append(".");
                    //protronixResponse.append(Integer.toHexString(dpaAddInfo.getHwProfile() & 0xFF00).toUpperCase());
                    protronixResponse.append(String.format("%02X", dpaAddInfo.getHwProfile() & 0xFF00));
                    protronixResponse.append(".");
                    //protronixResponse.append(Integer.toHexString(dpaAddInfo.getResponseCode().getCodeValue()).toUpperCase());
                    protronixResponse.append(String.format("%02X", dpaAddInfo.getResponseCode().getCodeValue()));
                    protronixResponse.append(".");
                    //protronixResponse.append(Integer.toHexString(dpaAddInfo.getDPA_Value()).toUpperCase());
                    protronixResponse.append(String.format("%02X", dpaAddInfo.getDPA_Value()));
                    protronixResponse.append(".");

                    System.out.print("Received data from UART on the node " + node1.getId() + ": ");

                    for (int i = 0; i < receivedUARTPData.length; i++) {

                        //protronixResponse.append(Integer.toHexString(readResultLoop).toUpperCase());
                        protronixResponse.append(String.format("%02X", receivedUARTPData[i]));

                        if(i < receivedUARTPData.length - 1)
                            protronixResponse.append(".");

                        System.out.print(Integer.toHexString(receivedUARTPData[i]).toUpperCase() + " ");
                    }

                    System.out.println();

                    // complete response
                    String protronixDPAResponse = protronixDPAResponsesHeader + protronixResponse;

                    float temperature = (receivedUARTPData[4] << 8) + receivedUARTPData[5];
                    temperature = temperature / 10;

                    float humidity = (receivedUARTPData[2] << 8) + receivedUARTPData[3];
                    humidity = Math.round(humidity / 10);

                    int co2 = (receivedUARTPData[0] << 8) + receivedUARTPData[1];
                    
                    DecimalFormat df = new DecimalFormat("###.##");

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String protronixValuesToBeSent
                            = "{\"e\":["
                            + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + df.format(temperature) + "},"
                            + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "},"
                            + "{\"n\":\"co2\"," + "\"u\":\"PPM\"," + "\"v\":" + df.format(co2) + "}"
                            + "],"
                            + "\"iqrf\":["
                            + "{\"pid\":" + pidProtronix + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node1.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.UART + "," + "\"pcmd\":" + "\"" + UART.MethodID.WRITE_AND_READ.name().toLowerCase() + "\","
                            + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                            + "],"
                            + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode1.getPrettyFormatedModuleId() + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_PROTRONIX, 2, protronixValuesToBeSent.getBytes());
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_PROTRONIX_DPA_REQUESTS, 2, protronixDPARequest.getBytes());
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_PROTRONIX_DPA_RESPONSES, 2, protronixDPAResponse.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                } 
            } else {
                System.out.println("Protronix result has not arrived.");
            }

            if (receivedDataTemp != null) {
                
                if (receivedDataTemp.length == 0) {
                    System.out.print("No received data from Custom on the node " + node2.getId());
                } else {

                    pidAustyn++;
                    
                    // fixed
                    String austynDPARequest = new String("02.00.20.01.FF.FF");

                    // header ... 2, 0, 32, 129, 17, 2, 0, 84 & data ... 114, 1
                    String austynDPAResponsesHeader = new String("02.00.20.81.");
                    
                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = customAustyn.getDPA_AdditionalInfoOfLastCall();
                    //DPA_AdditionalInfo dpaAddInfoTemp = (DPA_AdditionalInfo)custom.getCallResultAdditionalInfo(tempRequestUid);
                    
                    StringBuilder austynResponse = new StringBuilder();
                    
                    austynResponse.append(String.format("%02X", dpaAddInfo.getHwProfile() & 0x00FF ));
                    austynResponse.append(".");
                    austynResponse.append(String.format("%02X", dpaAddInfo.getHwProfile() & 0xFF00));
                    austynResponse.append(".");
                    austynResponse.append(String.format("%02X", dpaAddInfo.getResponseCode().getCodeValue()));
                    austynResponse.append(".");
                    austynResponse.append(String.format("%02X", dpaAddInfo.getDPA_Value()));
                    austynResponse.append(".");

                    System.out.print("Received data from Custom on the node " + node2.getId() + ": ");
                    
                    for (int i = 0; i < receivedDataTemp.length; i++) {
                        
                        austynResponse.append(String.format("%02X", receivedDataTemp[i]));

                        if (i < receivedDataTemp.length - 1) {
                            austynResponse.append(".");
                        }

                        System.out.print(Integer.toHexString(receivedDataTemp[i]).toUpperCase() + " ");
                    }
                    
                    System.out.println();
                    
                    // complete response
                    String austynDPAResponse = austynDPAResponsesHeader + austynResponse;

                    short rawSixteenth = (short) (receivedDataTemp[0] | (receivedDataTemp[1] << 8));
                    float temperature = rawSixteenth / 16.0f;
                    
                    DecimalFormat df = new DecimalFormat("###.##");

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String austynValuesToBeSent
                            = "{\"e\":["
                            + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + df.format(temperature) + "}"
                            + "],"
                            + "\"iqrf\":["
                            + "{\"pid\":" + pidAustyn + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                            + "],"
                            + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode2.getPrettyFormatedModuleId() + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_AUSTYN, 2, austynValuesToBeSent.getBytes());
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_AUSTYN_DPA_REQUESTS, 2, austynDPARequest.getBytes());
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_AUSTYN_DPA_RESPONSES, 2, austynDPAResponse.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                }    
            } else {
                System.out.println("Austyn result has not arrived.");
            }

            if (receivedUARTDData != null) {

                if(receivedUARTDData.length == 0) {
                    System.out.print("No received data from UART on the node " + node3.getId());
                }
                else {

                    pidDevtech++;

                    System.out.print("Received data from UART on the node " + node3.getId() + ": ");
                    for (Short readResultLoop : receivedUARTDData) {
                        System.out.print(Integer.toHexString(readResultLoop).toUpperCase() + " ");
                    }
                    System.out.println();   

                    float supplyVoltage = (receivedUARTDData[0] * 256 + receivedUARTDData[1]) / 100;
                    float frequency = (receivedUARTDData[2] * 256 + receivedUARTDData[3]) / 100;
                    float activePower = (receivedUARTDData[4] * 256 + receivedUARTDData[5]) / 100;
                    float supplyCurrent = (receivedUARTDData[6] * 256 + receivedUARTDData[7]) / 100;
                    float powerFactor = (receivedUARTDData[8] * 256 + receivedUARTDData[9]) / 100;
                    float activeEnergy = (receivedUARTDData[10] * 65536 + receivedUARTDData[11] * 256 + receivedUARTDData[12]) / 100;
                    float deviceBurningHour = (receivedUARTDData[13] * 256 + receivedUARTDData[14]) / 100;
                    float ledBurningHour = (receivedUARTDData[15] * 256 + receivedUARTDData[16]) / 100;
                    float dimming = receivedUARTDData[17];

                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = uartD.getDPA_AdditionalInfoOfLastCall();

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String devtechValuesToBeSent
                            = "{\"e\":["
                            + "{\"n\":\"supply voltage\"," + "\"u\":\"V\"," + "\"v\":" + supplyVoltage + "},"
                            + "{\"n\":\"frequency\"," + "\"u\":\"Hz\"," + "\"v\":" + frequency + "},"
                            + "{\"n\":\"active power\"," + "\"u\":\"W\"," + "\"v\":" + activePower + "},"
                            + "{\"n\":\"supply current\"," + "\"u\":\"A\"," + "\"v\":" + supplyCurrent + "},"
                            + "{\"n\":\"power factor\"," + "\"u\":\"cos\"," + "\"v\":" + powerFactor + "},"
                            + "{\"n\":\"active energy\"," + "\"u\":\"J\"," + "\"v\":" + supplyCurrent + "},"
                            + "{\"n\":\"device burning hour\"," + "\"u\":\"hours\"," + "\"v\":" + deviceBurningHour + "},"
                            + "{\"n\":\"led burning hour\"," + "\"u\":\"hours\"," + "\"v\":" + ledBurningHour + "},"
                            + "{\"n\":\"dimming\"," + "\"u\":\"%\"," + "\"v\":" + dimming + "}"
                            + "],"
                            + "\"iqrf\":["
                            + "{\"pid\":" + pidDevtech + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.UART + "," + "\"pcmd\":" + "\"" + UART.MethodID.WRITE_AND_READ.name().toLowerCase() + "\","
                            + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                            + "],"
                            + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode3.getPrettyFormatedModuleId() + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_STATUS_DEVTECH, 2, devtechValuesToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                }
            } else {
                System.out.println("Devtech result has not arrived.");
            }
                    
            if (receivedCustomDatmoluxData != null) {
                
                if (receivedCustomDatmoluxData.length == 0) {
                    System.out.print("No received data from Custom on the node " + node4.getId());
                } else {

                    pidDatmolux++;

                    System.out.print("Received data from Custom on the node " + node4.getId() + ": ");
                    for (Short readResultLoop : receivedCustomDatmoluxData) {
                        System.out.print(Integer.toHexString(readResultLoop).toUpperCase() + " ");
                    }
                    System.out.println();

                    int activePower = receivedCustomDatmoluxData[0];

                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String datmoluxValuesToBeSent
                            = "{\"e\":["
                            + "{\"n\":\"active power\"," + "\"u\":\"W\"," + "\"v\":" + activePower + "}"
                            + "],"
                            + "\"iqrf\":["
                            + "{\"pid\":" + pidDevtech + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                            + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                            + "],"
                            + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode4.getPrettyFormatedModuleId() + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_STATUS_DATMOLUX, 2, datmoluxValuesToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                }
            } else {
                System.out.println("Datmolux result has not arrived.");
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
        if (MQTTTopics.STD_ACTUATORS_AUSTYN.equals(topic)) {

            // TODO: check nodeID and add selection
            if (valueDPA.equalsIgnoreCase("REQ")) {

                webResponseTopic = MQTTTopics.STD_ACTUATORS_AUSTYN;

                if (valueN.equalsIgnoreCase("IO")) {

                    // getting IO interface
                    IO io = node2.getDeviceObject(IO.class);
                    if (io == null) {
                        printMessageAndExit("IO doesn't exist on node 2", true);
                    }

                    if (valueSV.equalsIgnoreCase("ON")) {

                        // set all pins OUT
                        IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[]{
                            new IO_DirectionSettings(0x02, 0x20, 0x00)
                        };

                        VoidType result = io.setDirection(dirSettings);
                        if (result == null) {
                            CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting IO direction failed: " + error, false);
                        }

                        // set Austyn HIGH
                        IO_Command[] iocs = new IO_OutputValueSettings[]{
                            new IO_OutputValueSettings(0x02, 0x20, 0x20)
                        };

                        result = io.setOutputState(iocs);
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        webResponseToBeSent
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"on\"}],"
                                + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode2.getPrettyFormatedModuleId() + "\""
                                + "}";

                        webRequestReceived = true;
                        return webResponseToBeSent;
                    }

                    if (valueSV.equalsIgnoreCase("OFF")) {

                        // set all pins OUT
                        IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[]{
                            new IO_DirectionSettings(0x02, 0x20, 0x00)
                        };

                        VoidType result = io.setDirection(dirSettings);
                        if (result == null) {
                            CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting IO direction failed: " + error, false);
                        }

                        // set Austyn LOW
                        IO_Command[] iocs = new IO_OutputValueSettings[]{
                            new IO_OutputValueSettings(0x02, 0x20, 0x00)
                        };

                        result = io.setOutputState(iocs);
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        webResponseToBeSent
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"off\"}],"
                                + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode2.getPrettyFormatedModuleId() + "\""
                                + "}";

                        webRequestReceived = true;
                        return webResponseToBeSent;
                    }
                }
            }
        }

        // there is a need to select topic
        if (MQTTTopics.STD_ACTUATORS_DEVTECH.equals(topic)) {

            // TODO: check nodeID and add selection
            if (valueDPA.equalsIgnoreCase("REQ")) {

                webResponseTopic = MQTTTopics.STD_ACTUATORS_DEVTECH;

                if (valueN.equalsIgnoreCase("IO")) {

                    // getting IO interface
                    IO io = node3.getDeviceObject(IO.class);
                    if (io == null) {
                        printMessageAndExit("IO doesn't exist on node 3", true);
                    }

                    if (valueSV.equalsIgnoreCase("ON")) {

                        // set devtech pins OUT
                        IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[]{
                            new IO_DirectionSettings(0x02, 0x08, 0x00)
                        };

                        VoidType result = io.setDirection(dirSettings);
                        if (result == null) {
                            CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting IO direction failed: " + error, false);
                        }

                        // set Devtech HIGH
                        IO_Command[] iocs = new IO_OutputValueSettings[]{
                            new IO_OutputValueSettings(0x02, 0x08, 0x08)
                        };

                        result = io.setOutputState(iocs);
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        webResponseToBeSent
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"on\"}],"
                                + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode3.getPrettyFormatedModuleId() + "\""
                                + "}";

                        webRequestReceived = true;
                        return webResponseToBeSent;
                    }

                    if (valueSV.equalsIgnoreCase("OFF")) {

                        // set all pins OUT
                        IO_DirectionSettings[] dirSettings = new IO_DirectionSettings[]{
                            new IO_DirectionSettings(0x02, 0x08, 0x00)
                        };

                        VoidType result = io.setDirection(dirSettings);
                        if (result == null) {
                            CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting IO direction failed: " + error, false);
                        }

                        // set Devtech LOW
                        IO_Command[] iocs = new IO_OutputValueSettings[]{
                            new IO_OutputValueSettings(0x02, 0x08, 0x00)
                        };

                        result = io.setOutputState(iocs);
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        webResponseToBeSent
                                = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"off\"}],"
                                + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
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
        
        // there is a need to select topic
        if (MQTTTopics.STD_ACTUATORS_DATMOLUX.equals(topic)) {

            // TODO: check nodeID and add selection
            if (valueDPA.equalsIgnoreCase("REQ")) {

                webResponseTopic = MQTTTopics.STD_ACTUATORS_DATMOLUX;

                if (valueN.equalsIgnoreCase("CUSTOM")) {

                    if (valueSV.equalsIgnoreCase("ON")) {

                        // getting Custom interface - datmolux
                        Custom customDatmolux = node4.getDeviceObject(Custom.class);
                        if (customDatmolux == null) {
                            printMessageAndExit("Custom doesn't exist on node 4", true);
                        }
                        
                        Short[] result = customDatmolux.send(peripheralDatmolux, cmdIdDatmoON, dataDatmo);
                        if (result == null) {
                            CallRequestProcessingError error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 4: " + error, false);
                        }

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        webResponseToBeSent
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"on\"}],"
                                + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode4.getPrettyFormatedModuleId() + "\""
                                + "}";

                        webRequestReceived = true;
                        return webResponseToBeSent;
                    }

                    if (valueSV.equalsIgnoreCase("OFF")) {

                        // getting Custom interface - datmolux
                        Custom customDatmolux = node4.getDeviceObject(Custom.class);
                        if (customDatmolux == null) {
                            printMessageAndExit("Custom doesn't exist on node 4", true);
                        }
                        
                        Short[] result = customDatmolux.send(peripheralDatmolux, cmdIdDatmoOFF, dataDatmo);
                        if (result == null) {
                            CallRequestProcessingError error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 4: " + error, false);
                        }

                        // getting additional info of the last call
                        DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

                        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                        webResponseToBeSent
                                = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"off\"}],"
                                + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode4.getPrettyFormatedModuleId() + "\""
                                + "}";

                        webRequestReceived = true;
                        return webResponseToBeSent;
                    }
                }
            }
        }

        return null;
    }

    public static boolean sendDPAAsyncRequest() throws InterruptedException {

        pidAsync++;
        Thread.sleep(100);

        // getting LEDR interface
        LEDR ledr = node1.getDeviceObject(LEDR.class);
        if (ledr == null) {
            printMessageAndExit("LEDR doesn't exist or is not enabled", false);
            return false;
        }

        // TODO: - do action based on published request - pulsing for now
        VoidType setResult = ledr.pulse();
        if (setResult == null) {
            processNullResult(ledr, "Pulsing LEDR failed",
                    "Pulsing LEDR hasn't been processed yet"
            );
            return false;
        }

        // getting additional info of the last call
        DPA_AdditionalInfo dpaAddInfo = ledr.getDPA_AdditionalInfoOfLastCall();

        // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
        String asyncRequestToBeSent
                = "{\"e\":[{\"n\":\"ledr\"," + "\"sv\":" + "\"" + LEDR.MethodID.PULSE.name().toLowerCase() + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pidAsync + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node1.getId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.LEDR + "," + "\"pcmd\":" + "\"" + LEDR.MethodID.PULSE.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + osInfoNode1.getPrettyFormatedModuleId() + "\""
                + "}";

        // send data to mqtt
        try {
            mqttCommunicator.publish(MQTTTopics.ASYNCHRONOUS_RESPONSES_STD, 2, asyncRequestToBeSent.getBytes());
        } catch (MqttException ex) {
            System.err.println("Error while publishing sync dpa message.");
        }

        return true;
    }

    // processes NULL result
    private static void processNullResult(GeneralLED led, String errorMsg, String notProcMsg) {
        CallRequestProcessingState procState = led.getCallRequestProcessingStateOfLastCall();
        if (procState == CallRequestProcessingState.ERROR) {
            CallRequestProcessingError error = led.getCallRequestProcessingErrorOfLastCall();
            printMessageAndExit(errorMsg + ": " + error, false);
        } else {
            printMessageAndExit(notProcMsg + ": " + procState, false);
        }
    }

    @Override
    public void onAsynchronousMessage(DPA_AsynchronousMessage message) {

        System.out.println("New asynchronous message.");

        //String nodeId = message.getMessageSource().getNodeId();
        //int peripheralNumber = message.getMessageSource().getPeripheralNumber();
        //short[] mainData = (short[])message.getMainData();        
        //DPA_AdditionalInfo additionalData = (DPA_AdditionalInfo)message.getAdditionalData();
        // sending control message back to network based on received message
        //asyncRequestReceived = true;
    }
}
