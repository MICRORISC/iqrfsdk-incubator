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
import com.microrisc.simply.iqrf.dpa.v22x.devices.IO;
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
    public static Node node5 = null;
    
    public static OsInfo osInfoNode1 = null;
    public static OsInfo osInfoNode2 = null;
    public static OsInfo osInfoNode3 = null;
    public static OsInfo osInfoNode4 = null;
    public static OsInfo osInfoNode5 = null;
    public static String moduleId = null;

    public static IO ioa = null;
    public static IO iodev = null;
    public static IO iodat = null;
    
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
    public static String certFile = null;
    
    public static String netType = "std";

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
    private static short cmdIdDatmoLEVEL = 0x05;
    private static short cmdIdDatmoPOWER = 0x06;
    private static short[] dataDatmo = new short[]{};
    private static short[] receivedCustomDatmoluxData = null;

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
        mqttCommunicator = new MQTTCommunicator(url, clientId, cleanSession, quietMode, userName, password, certFile, netType);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_AUSTYN, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_DEVTECH, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_DATMOLUX, 2);
        mqttCommunicator.subscribe(MQTTTopics.STD_ACTUATORS_TECO, 2);

        // ASYNC REQUESTS FROM DPA
        final OpenGatewayTest msgListener = new OpenGatewayTest();

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
       
        // getting node 5 - teco
        node5 = network1.getNode("5");
        if (node5 == null) {
            printMessageAndExit("Node 5 doesn't exist", true);
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
        
        // getting OS interface
        os = node5.getDeviceObject(OS.class);
        if (os == null) {
            printMessageAndExit("OS doesn't exist on node 5", false);
        }
        
        // get info about module
        osInfoNode5 = os.read();
        if (osInfoNode5 == null) {
            CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
            if (procState == ERROR) {
                CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed on node 5: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet on node 5: " + procState, false);
            }
        }
        
        // getting IO interface
        IO ioa = node2.getDeviceObject(IO.class);
        if (ioa == null) {
            printMessageAndExit("IO doesn't exist on node 2", false);
        }

        // get state of IO
        short[] ioStateAustyn = ioa.get();
        if ( ioStateAustyn == null ) {            
            CallRequestProcessingState procState = ioa.getCallRequestProcessingStateOfLastCall();
            if ( procState == CallRequestProcessingState.ERROR ) {
                CallRequestProcessingError error = ioa.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting IO state failed on node 2: " + error, false);
            } else {
                printMessageAndExit("Getting IO state hasn't been processed yet on node 2: " + procState, false);
            }
        } else {
            System.out.print("Austyn IO state: ");
            for (int i = 0; i < ioStateAustyn.length; i++) {
                System.out.print(Integer.toHexString(ioStateAustyn[i]).toUpperCase() + " ");
            }
            System.out.println();

            short PORTC = ioStateAustyn[2];
            String STATE = " ";

            if( (PORTC & 0x20) == 0x20)
               STATE = "on";
            else
               STATE = "off"; 

            // getting additional info of the last call
            DPA_AdditionalInfo dpaAddInfoIo = ioa.getDPA_AdditionalInfoOfLastCall();

            if (osInfoNode2 != null) {
                moduleId = osInfoNode2.getPrettyFormatedModuleId();
            } else {
                moduleId = "not-known";
            }

            webResponseTopic = MQTTTopics.STD_ACTUATORS_AUSTYN;

            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
            webResponseToBeSent
                    = "{\"e\":[{\"n\":\"io\"," + "\"sv\":\"" + STATE + "\"}],"
                    + "\"iqrf\":[{\"pid\":" + pidAustyn + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                    + "\"hwpid\":" + dpaAddInfoIo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfoIo.getResponseCode().name().toLowerCase() + "\","
                    + "\"dpavalue\":" + dpaAddInfoIo.getDPA_Value() + "}],"
                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                    + "}";

            try {
                mqttCommunicator.publish(webResponseTopic, 2, webResponseToBeSent.getBytes());
            } catch (MqttException ex) {
                System.err.println("Error while publishing web response message.");
            }
        }
        
        // getting IO interface
        IO iodev = node3.getDeviceObject(IO.class);
        if (iodev == null) {
            printMessageAndExit("IO doesn't exist on node 3", false);
        }

        // get state of IO
        short[] ioStateDevtech = iodev.get();
        if ( ioStateDevtech == null ) {            
            CallRequestProcessingState procState = ioa.getCallRequestProcessingStateOfLastCall();
            if ( procState == CallRequestProcessingState.ERROR ) {
                CallRequestProcessingError error = ioa.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting IO state failed on node 3: " + error, false);
            } else {
                printMessageAndExit("Getting IO state hasn't been processed yet on node 3: " + procState, false);
            }
        } else {
            System.out.print("Devtech IO state: ");
            for (int i = 0; i < ioStateDevtech.length; i++) {
                System.out.print(Integer.toHexString(ioStateDevtech[i]).toUpperCase() + " ");
            }
            System.out.println();
        
            short PORTC = ioStateDevtech[2];
            String STATE = " ";

            if( (PORTC & 0x08) == 0x08)
               STATE = "on";
            else
               STATE = "off"; 

            // getting additional info of the last call
            DPA_AdditionalInfo dpaAddInfoIo = iodev.getDPA_AdditionalInfoOfLastCall();

            moduleId = null;
            if (osInfoNode3 != null) {
                moduleId = osInfoNode3.getPrettyFormatedModuleId();
            } else {
                moduleId = "not-known";
            }

            webResponseTopic = MQTTTopics.STD_ACTUATORS_DEVTECH;

            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
            webResponseToBeSent
                    = "{\"e\":[{\"n\":\"io\"," + "\"sv\":\"" + STATE + "\"}],"
                    + "\"iqrf\":[{\"pid\":" + pidDevtech + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                    + "\"hwpid\":" + dpaAddInfoIo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfoIo.getResponseCode().name().toLowerCase() + "\","
                    + "\"dpavalue\":" + dpaAddInfoIo.getDPA_Value() + "}],"
                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                    + "}";

            try {
                mqttCommunicator.publish(webResponseTopic, 2, webResponseToBeSent.getBytes());
            } catch (MqttException ex) {
                System.err.println("Error while publishing web response message.");
            }
        }

        // getting UART interface - protronix
        UART uartP = node1.getDeviceObject(UART.class);
        if (uartP == null) {
            printMessageAndExit("UART doesn't exist on node 1", true);
        } else {
            //int protronixHWPID = 0x0132;
            int protronixHWPID = 0xFFFF;
            uartP.setRequestHwProfile(protronixHWPID);
        }

        // getting Custom interface - austyn
        Custom customAustyn = node2.getDeviceObject(Custom.class);
        if (customAustyn == null) {
            printMessageAndExit("Custom doesn't exist on node 2", true);
        } else {
            int austynHWPID = 0x0211;
            customAustyn.setRequestHwProfile(austynHWPID);
        }

        // getting UART interface - devtech
        UART uartD = node3.getDeviceObject(UART.class);
        if (uartD == null) {
            printMessageAndExit("UART doesn't exist on node 3", true);
        } else {
            //int devtechHWPID = 0x0121;
            int devtechHWPID = 0xFFFF;
            uartD.setRequestHwProfile(devtechHWPID);
        }

        // getting Custom interface - datmolux
        Custom customDatmolux = node4.getDeviceObject(Custom.class);
        if (customDatmolux == null) {
            printMessageAndExit("Custom doesn't exist on node 4", true);
        } else {
            int datmoluxHWPID = 0x0611;
            customDatmolux.setRequestHwProfile(datmoluxHWPID);
        }
        
        // getting Custom interface - teco
        Custom customTeco = node5.getDeviceObject(Custom.class);
        if (customTeco == null) {
            printMessageAndExit("Custom doesn't exist on node 5", true);
        } else {
            int tecoHWPID = 0x0511;
            customTeco.setRequestHwProfile(tecoHWPID);
        }
        
        // getting results
        // set up maximal number of cycles according to your needs - only testing
        //final int MAX_CYCLES = 5000;
        //for (int cycle = 0; cycle < MAX_CYCLES; cycle++) {
        while( true ) {
            
            // after 100ms reading
            int timeoutP = 0x0A;
            short[] dataP = {0x47, 0x44, 0x03};
            short[] receivedUARTPData = null;

            // getting austyn temperature
            short peripheralAustyn = 0x20;
            short cmdIdTemp = 0x01;
            short[] dataTemp = new short[]{};
            short[] receivedDataTemp = null;
            
            // after 2390ms reading
            int timeoutD = 0xEF;
            short[] dataD = {0x65, 0xFD};
            short[] receivedUARTDData = null;

            // write UART peripheral
            UUID uartPRequestUid = null;
            if(uartP != null) {
                uartPRequestUid = uartP.async_writeAndRead(timeoutP, dataP);
            }
                
            // send Custom 
            UUID tempRequestUid = null;
            if(customAustyn != null) {
                tempRequestUid = customAustyn.async_send(peripheralAustyn, cmdIdTemp, dataTemp);
            }
                
            // write UART peripheral
            UUID uartDRequestUid = null;
            if(uartD != null) {
                uartD.setDefaultWaitingTimeout(5000);
                uartDRequestUid = uartD.async_writeAndRead(timeoutD, dataD);
            }
                
            // send Custom 
            UUID datmoPowerRequestUid = null;
            if(customDatmolux != null) {
                datmoPowerRequestUid = customDatmolux.async_send(peripheralDatmolux, cmdIdDatmoPOWER, dataDatmo);
            }
                
            // maximal number of attempts of getting a result
            final int RETRIES = 3;
            int attempt = 0;
            int checkResponse = 0;

            while (attempt++ < RETRIES) {

                // main job is here for now - quick hack to test
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
                                System.err.println("Error while publishing web response message.");
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
                CallRequestProcessingState procStateP =  null;
                if (uartP != null) {
                    procStateP = uartP.getCallRequestProcessingState(uartPRequestUid);
                }
                
                CallRequestProcessingState procStateTemp = null;
                if(customAustyn != null) {
                    procStateTemp = customAustyn.getCallRequestProcessingState(tempRequestUid);
                }
                
                CallRequestProcessingState procStateD = null;
                if(uartD != null) {    
                    procStateD = uartD.getCallRequestProcessingState(uartDRequestUid);
                }
                
                CallRequestProcessingState procStatePower = null;
                if(customDatmolux != null) {
                    procStatePower = customDatmolux.getCallRequestProcessingState(datmoPowerRequestUid);
                }

                // if any error occured
                if(procStateP != null) {
                    // if any error occured
                    if (procStateP == CallRequestProcessingState.ERROR) {

                        // general call error
                        CallRequestProcessingError error = uartP.getCallRequestProcessingError(uartPRequestUid);
                        printMessageAndExit("Getting UART data failed on node1: " + error.getErrorType(), false);

                    } else {
                        // have result already - protronix
                        if (procStateP == CallRequestProcessingState.RESULT_ARRIVED) {
                            receivedUARTPData = uartP.getCallResultImmediately(uartPRequestUid, short[].class);

                            if(receivedUARTPData != null && receivedUARTPData.length == 0) {
                                // specific call error
                                DPA_AdditionalInfo dpaAddInfo = uartP.getDPA_AdditionalInfoOfLastCall();
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                printMessageAndExit("Getting UART data failed on node1, DPA error: " + dpaResponseCode, false);
                            }                 
                        } else {
                            System.out.println("Getting UART data on node 1 hasn't been processed yet: " + procStateP);
                        }
                    }
                }

                // if any error occured
                if (procStateTemp != null) {
                    // if any error occured
                    if (procStateTemp == CallRequestProcessingState.ERROR) {

                        // general call error
                        CallRequestProcessingError error = customAustyn.getCallRequestProcessingError(tempRequestUid);
                        printMessageAndExit("Getting Custom data failed on node2: " + error.getErrorType(), false);

                    } else {
                        // have result already - austyn
                        if (procStateTemp == CallRequestProcessingState.RESULT_ARRIVED) {
                            receivedDataTemp = customAustyn.getCallResultImmediately(tempRequestUid, short[].class);

                            if(receivedDataTemp != null && receivedDataTemp.length == 0) {
                                // specific call error
                                DPA_AdditionalInfo dpaAddInfo = customAustyn.getDPA_AdditionalInfoOfLastCall();
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                printMessageAndExit("Getting Custom data failed on node2, DPA error: " + dpaResponseCode, false);
                            }
                        } else {
                            System.out.println("Getting Custom data on node 2 hasn't been processed yet: " + procStateTemp);
                        }
                    }
                }

                // if any error occured
                if(procStateD != null) {
                    // if any error occured
                    if (procStateD == CallRequestProcessingState.ERROR) {

                        // general call error
                        CallRequestProcessingError error = uartD.getCallRequestProcessingError(uartDRequestUid);
                        printMessageAndExit("Getting UART data failed on node3: " + error.getErrorType(), false);
                    } else {
                        // have result already - devtech
                        if (procStateD == CallRequestProcessingState.RESULT_ARRIVED) {
                            receivedUARTDData = uartD.getCallResultImmediately(uartDRequestUid, short[].class);

                            if(receivedUARTDData != null && receivedUARTDData.length == 0) {
                                // specific call error
                                DPA_AdditionalInfo dpaAddInfo = uartD.getDPA_AdditionalInfoOfLastCall();
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                printMessageAndExit("Getting UART data failed on node3, DPA error: " + dpaResponseCode, false);
                            }
                        } else {
                            System.out.println("Getting UART data on node3 hasn't been processed yet: " + procStateD);
                        }
                    }
                }

                // if any error occured
                if(procStatePower != null) {
                    // if any error occured
                    if (procStatePower == CallRequestProcessingState.ERROR) {

                        // general call error
                        CallRequestProcessingError error = customDatmolux.getCallRequestProcessingError(datmoPowerRequestUid);
                        printMessageAndExit("Getting Custom data failed on node 4: " + error.getErrorType(), false);

                    } else {
                        // have result already - datmolux
                        if (procStateD == CallRequestProcessingState.RESULT_ARRIVED) {
                            receivedCustomDatmoluxData = customDatmolux.getCallResultImmediately(datmoPowerRequestUid, short[].class);

                            if (receivedCustomDatmoluxData != null && receivedCustomDatmoluxData.length == 0) {
                                // specific call error
                                DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();
                                DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                                printMessageAndExit("Getting Custom data failed on node 4, DPA error: " + dpaResponseCode, false);
                            }
                        } else {
                            System.out.println("Getting Custom data on node 4 hasn't been processed yet: " + procStatePower);
                        }
                    }
                    break;
                }
            }

            if (receivedUARTPData != null) {
                
                if (receivedUARTPData.length == 0) {
                    System.out.print("No received data from UART on the node " + node1.getId());
                } else {

                    pidProtronix++;

                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = uartP.getDPA_AdditionalInfoOfLastCall();

                    System.out.print("Received data from UART on the node " + node1.getId() + ": ");
                    for (int i = 0; i < receivedUARTPData.length; i++) {
                        System.out.print(Integer.toHexString(receivedUARTPData[i]).toUpperCase() + " ");
                    }
                    System.out.println();

                    float temperature = (receivedUARTPData[4] << 8) + receivedUARTPData[5];
                    temperature = temperature / 10;

                    float humidity = (receivedUARTPData[2] << 8) + receivedUARTPData[3];
                    humidity = Math.round(humidity / 10);

                    int co2 = (receivedUARTPData[0] << 8) + receivedUARTPData[1];
                    
                    DecimalFormat df = new DecimalFormat("###.##");
                   
                    if (osInfoNode1 != null) {
                        moduleId = osInfoNode1.getPrettyFormatedModuleId();
                    } else {
                        moduleId = "not-known";
                    }

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
                            + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_PROTRONIX, 2, protronixValuesToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                    
                    receivedUARTPData = null;
                } 
            } else {
                System.out.println("Protronix result has not arrived.");
            }

            if (receivedDataTemp != null) {
                
                if (receivedDataTemp.length == 0) {
                    System.out.print("No received data from Custom on the node " + node2.getId());
                } else {

                    pidAustyn++;
                    
                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = customAustyn.getDPA_AdditionalInfoOfLastCall();
                    //DPA_AdditionalInfo dpaAddInfoTemp = (DPA_AdditionalInfo)custom.getCallResultAdditionalInfo(tempRequestUid);

                    System.out.print("Received data from Custom on the node " + node2.getId() + ": ");
                    for (int i = 0; i < receivedDataTemp.length; i++) {
                        System.out.print(Integer.toHexString(receivedDataTemp[i]).toUpperCase() + " ");
                    }
                    
                    System.out.println();
                    
                    short rawSixteenth = (short) (receivedDataTemp[0] | (receivedDataTemp[1] << 8));
                    float temperature = rawSixteenth / 16.0f;
                    
                    DecimalFormat df = new DecimalFormat("###.##");
                    
                    if (osInfoNode2 != null) {
                        moduleId = osInfoNode2.getPrettyFormatedModuleId();
                    } else {
                        moduleId = "not-known";
                    }

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
                            + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_SENSORS_AUSTYN, 2, austynValuesToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                    
                    receivedDataTemp = null;
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
                    
                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = uartD.getDPA_AdditionalInfoOfLastCall();

                    System.out.print("Received data from UART on the node " + node3.getId() + ": ");
                    for (int i = 0; i < receivedUARTDData.length; i++) {
                        System.out.print(Integer.toHexString(receivedUARTDData[i]).toUpperCase() + " ");
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
                    
                    if (osInfoNode3 != null) {
                        moduleId = osInfoNode3.getPrettyFormatedModuleId();
                    } else {
                        moduleId = "not-known";
                    }
                    
                    // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                    String devtechValuesToBeSent
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
                            + "{\"pid\":" + pidDevtech + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                            + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.UART + "," + "\"pcmd\":" + "\"" + UART.MethodID.WRITE_AND_READ.name().toLowerCase() + "\","
                            + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                            + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                            + "],"
                            + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_STATUS_DEVTECH, 2, devtechValuesToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                    
                    receivedUARTDData = null;
                }
            } else {
                System.out.println("Devtech result has not arrived.");
            }
                    
            if (receivedCustomDatmoluxData != null) {
                
                if (receivedCustomDatmoluxData.length == 0) {
                    System.out.print("No received data from Custom on the node " + node4.getId());
                } else {

                    pidDatmolux++;
                    
                    // getting additional info of the last call
                    DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();
                 
                    System.out.print("Received data from Custom on the node " + node4.getId() + ": ");
                    for (int i = 0; i < receivedCustomDatmoluxData.length; i++) {   
                        System.out.print(Integer.toHexString(receivedCustomDatmoluxData[i]).toUpperCase() + " ");
                    }
                    System.out.println();

                    int activePower = receivedCustomDatmoluxData[0];
                    
                    if (osInfoNode4 != null) {
                        moduleId = osInfoNode4.getPrettyFormatedModuleId();
                    } else {
                        moduleId = "not-known";
                    }
                    
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
                            + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                            + "}";

                    // send data to mqtt
                    try {
                        mqttCommunicator.publish(MQTTTopics.STD_STATUS_DATMOLUX, 2, datmoluxValuesToBeSent.getBytes());
                    } catch (MqttException ex) {
                        System.err.println("Error while publishing sync dpa message.");
                    }
                    
                    receivedCustomDatmoluxData = null;
                }
            } else {
                System.out.println("Datmolux result has not arrived.");
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

                        int j = 3;
                        do {
                            result = io.setOutputState(iocs);
                        } while ( result == null && --j > 0 );
                                
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode2 != null) {
                                moduleId = osInfoNode2.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"on\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
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
                        
                        int j = 3;
                        do {
                            result = io.setOutputState(iocs);
                        } while ( result == null && --j > 0 );
                        
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode2 != null) {
                                moduleId = osInfoNode2.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"off\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node2.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
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
                        
                        int j = 3;
                        do {
                            result = io.setOutputState(iocs);
                        } while ( result == null && --j > 0 );
                        
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode3 != null) {
                                moduleId = osInfoNode3.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"on\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
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
                        
                        int j = 3;
                        do {
                            result = io.setOutputState(iocs);
                        } while ( result == null && --j > 0 );
                        
                        if (result == null) {
                            CallRequestProcessingState procState = io.getCallRequestProcessingStateOfLastCall();
                            if (procState == CallRequestProcessingState.ERROR) {
                                CallRequestProcessingError error = io.getCallRequestProcessingErrorOfLastCall();
                                printMessageAndExit("Setting IO output state failed: " + error, false);
                            } else {
                                printMessageAndExit("Setting IO output state hasn't been processed yet: " + procState, false);
                            }
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = io.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode3 != null) {
                                moduleId = osInfoNode3.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"off\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node3.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
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
                       
                        short[] result = null;
                        int j = 3;
                        do {
                            result = customDatmolux.send(peripheralDatmolux, cmdIdDatmoON, dataDatmo);
                        } while ( result == null && --j > 0 );
                        
                        if (result == null) {
                            CallRequestProcessingError error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 4: " + error, false);
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode4 != null) {
                                moduleId = osInfoNode4.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"on\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
                    }

                    if (valueSV.equalsIgnoreCase("OFF")) {

                        // getting Custom interface - datmolux
                        Custom customDatmolux = node4.getDeviceObject(Custom.class);
                        if (customDatmolux == null) {
                            printMessageAndExit("Custom doesn't exist on node 4", true);
                        }
                        
                        short[] result = null;
                        int j = 3;
                        do {
                            result = customDatmolux.send(peripheralDatmolux, cmdIdDatmoOFF, dataDatmo);
                        } while ( result == null && --j > 0 );
                        
                        if (result == null) {
                            CallRequestProcessingError error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 4: " + error, false);
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode4 != null) {
                                moduleId = osInfoNode4.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"off\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
                    }
                    
                    if (valueSV.equalsIgnoreCase("UP")) {

                        // getting Custom interface - datmolux
                        Custom customDatmolux = node4.getDeviceObject(Custom.class);
                        if (customDatmolux == null) {
                            printMessageAndExit("Custom doesn't exist on node 4", true);
                        }
                        
                        short[] resultUp = null;
                        //short[] resultLevel = null;
                        int j = 3;
                        do {
                            resultUp = customDatmolux.send(peripheralDatmolux, cmdIdDatmoUP, dataDatmo);
                            // not working for now
                            //resultLevel = customDatmolux.send(peripheralDatmolux, cmdIdDatmoLEVEL, dataDatmo);
                        } while ( resultUp == null && --j > 0 );
                       
                        if (resultUp == null) {
                            CallRequestProcessingError error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 4: " + error, false);
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode4 != null) {
                                moduleId = osInfoNode4.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }
                            
                            //String level = Short.toString(resultLevel[0]);
                            String level = "0";
                            
                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"up\"," + "\"v\":" + level + "}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
                    }
                    
                    if (valueSV.equalsIgnoreCase("DOWN")) {

                        // getting Custom interface - datmolux
                        Custom customDatmolux = node4.getDeviceObject(Custom.class);
                        if (customDatmolux == null) {
                            printMessageAndExit("Custom doesn't exist on node 4", true);
                        }
                        
                        short[] resultDown = null;
                        //short[] resultLevel = null;
                        int j = 3;
                        do {
                            resultDown = customDatmolux.send(peripheralDatmolux, cmdIdDatmoDOWN, dataDatmo);
                            //resultLevel = customDatmolux.send(peripheralDatmolux, cmdIdDatmoLEVEL, dataDatmo);
                        } while ( resultDown == null && --j > 0 );
                        
                        if (resultDown == null) {
                            CallRequestProcessingError error = customDatmolux.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 4: " + error, false);
                        } 
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = customDatmolux.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode4 != null) {
                                moduleId = osInfoNode4.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }
                            
                            //String level = Short.toString(resultLevel[0]);
                            String level = "0";
                            
                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"down\"," + "\"v\":" + level + "}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node4.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
                    }
                }
            }
        }
        
        // there is a need to select topic
        if (MQTTTopics.STD_ACTUATORS_TECO.equals(topic)) {
            
            // TODO: check nodeID and add selection
            if (valueDPA.equalsIgnoreCase("REQ")) {

                webResponseTopic = MQTTTopics.STD_ACTUATORS_TECO;

                if (valueN.equalsIgnoreCase("CUSTOM")) {

                    if (valueSV.equalsIgnoreCase("ON")) {

                        // getting Custom interface - teco
                        Custom customTeco = node5.getDeviceObject(Custom.class);
                        if (customTeco == null) {
                            printMessageAndExit("Custom doesn't exist on node 5", true);
                        }
                        
                        short[] result = null;
                        int j = 3;
                        do {
                            result = customTeco.send((short) 0x20, (short) 0x00, new short[]{});
                        } while ( result == null && --j > 0 );
                        
                        if (result == null) {
                            CallRequestProcessingError error = customTeco.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 5: " + error, false);
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = customTeco.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode5 != null) {
                                moduleId = osInfoNode5.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"on\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node5.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
                    }

                    if (valueSV.equalsIgnoreCase("OFF")) {

                        // getting Custom interface - teco
                        Custom customTeco = node5.getDeviceObject(Custom.class);
                        if (customTeco == null) {
                            printMessageAndExit("Custom doesn't exist on node 5", true);
                        }                      
                        
                        short[] result = null;
                        int j = 3;
                        do {
                            result = customTeco.send((short) 0x20, (short) 0x01, new short[]{});
                        } while (result == null && --j > 0);
                        
                        if (result == null) {
                            CallRequestProcessingError error = customTeco.getCallRequestProcessingErrorOfLastCall();
                            printMessageAndExit("Setting Custom failed on node 5: " + error, false);
                        }
                        else {
                            // getting additional info of the last call
                            DPA_AdditionalInfo dpaAddInfo = customTeco.getDPA_AdditionalInfoOfLastCall();

                            if (osInfoNode5 != null) {
                                moduleId = osInfoNode5.getPrettyFormatedModuleId();
                            } else {
                                moduleId = "not-known";
                            }

                            // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                            webResponseToBeSent
                                    = "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"off\"}],"
                                    + "\"iqrf\":[{\"pid\":" + valuePID + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + node5.getId() + ","
                                    + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                                    + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                                    + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}],"
                                    + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                                    + "}";

                            webRequestReceived = true;
                            return webResponseToBeSent;
                        }
                    }
                }
            }
        }

        return null;
    }

    public static boolean sendDPAAsyncRequest() {
        return true;
    }

    @Override
    public void onAsynchronousMessage(DPA_AsynchronousMessage message) {

        System.out.println("New asynchronous message.");

        //String nodeId = message.getMessageSource().getNodeId();
        //int peripheralNumber = message.getMessageSource().getPeripheralNumber();
        //Short[] mainData = (Short[])message.getMainData();        
        //DPA_AdditionalInfo additionalData = (DPA_AdditionalInfo)message.getAdditionalData();
        // sending control message back to network based on received message
        //asyncRequestReceived = true;
    }
}
