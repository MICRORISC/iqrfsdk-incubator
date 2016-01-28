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

package com.microrisc.opengateway;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.microrisc.opengateway.mqtt.Communicator;
import com.microrisc.opengateway.mqtt.Topics;
import com.microrisc.simply.CallRequestProcessingState;
import static com.microrisc.simply.CallRequestProcessingState.ERROR;
import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.asynchrony.AsynchronousMessagesListener;
import com.microrisc.simply.asynchrony.AsynchronousMessagingManager;
import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessageProperties;
import com.microrisc.simply.iqrf.dpa.v22x.devices.GeneralLED;
import com.microrisc.simply.iqrf.dpa.v22x.devices.LEDG;
import com.microrisc.simply.iqrf.dpa.v22x.devices.LEDR;
import com.microrisc.simply.iqrf.dpa.v22x.devices.OS;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Thermometer;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;
import com.microrisc.simply.iqrf.dpa.v22x.types.Thermometer_values;
import com.microrisc.simply.iqrf.types.VoidType;
import java.io.File;
import java.util.UUID;
import org.eclipse.paho.client.mqttv3.MqttException;


/**
 * Running first tests with DPA <-> MQTT.
 * 
 * @author Rostislav Spinar
 */
public class OpenGatewayTest implements AsynchronousMessagesListener<DPA_AsynchronousMessage> 
{
    // references for DPA
    public static DPA_Simply simply = null;
    public static Network network1 = null;
    public static Node node1 = null;
    public static Node node2 = null;
    
    // references for MQTT
    public static String protocol = "tcp://";
    public static String broker = "localhost";
    public static int port = 1883;
    
    public static String clientId = "b827eb26c73d";
    public static String subTopic = "in";    
    public static String pubTopic = "out";
    
    public static boolean cleanSession = true;
    public static boolean quietMode = false;
    public static boolean ssl = false;
    
    public static String password = null;
    public static String userName = null;
    
    public static Communicator mqttCommunicator = null;

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
            simply = DPA_SimplyFactory.getSimply("config" + File.separator + "Simply.properties");
        } catch ( SimplyException ex ) {
            printMessageAndExit("Error while creating Simply: " + ex.getMessage(), true);
        }
        
        // MQTT INIT
        
        String url = protocol + broker + ":" + port;
        mqttCommunicator = new Communicator(url, clientId, cleanSession, quietMode, userName, password);
        mqttCommunicator.subscribe(Topics.ACTUATOR_LEDG, 2);
        mqttCommunicator.subscribe(Topics.ACTUATOR_LEDR, 2);
        mqttCommunicator.subscribe(subTopic, 2);
        
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
        if ( node2 == null ) {
            printMessageAndExit("Node 2 doesn't exist", true);
        }
        
        // getting Thermometer interface
        Thermometer thermo = node1.getDeviceObject(Thermometer.class);
        if ( thermo == null ) {
            printMessageAndExit("Thermometer doesn't exist on node 1", true);
        }
        
        // getting OS interface
        OS os = node1.getDeviceObject(OS.class);
        if ( os == null ) {
            printMessageAndExit("OS doesn't exist on node 1", false);
        }
         
        // get info about module
        OsInfo osInfo = os.read();
        if (osInfo == null) {
            CallRequestProcessingState procState = os.getCallRequestProcessingStateOfLastCall();
            if ( procState == ERROR ) {
                CallRequestProcessingError error = os.getCallRequestProcessingErrorOfLastCall();
                printMessageAndExit("Getting OS info failed: " + error, false);
            } else {
                printMessageAndExit("Getting OS info hasn't been processed yet: " + procState, false);
            }
        }
        
        // getting results
        // set up maximal number of cycles according to your needs - only testing
        final int MAX_CYCLES = 5000;        
        for ( int cycle = 0; cycle < MAX_CYCLES; cycle++ ) {
        
            // getting actual temperature
            Thermometer_values thermoValues = null;
            UUID tempRequestUid = thermo.async_get();

            // maximal number of attempts of getting a result
            final int RETRIES = 3;
            int attempt = 0;
            
            while (attempt++ < RETRIES) {
                
                // wait for response
                Thread.sleep(30000);
                
                // get request call state
                CallRequestProcessingState procState = thermo.getCallRequestProcessingState(tempRequestUid);

                // have result already
                if (procState == CallRequestProcessingState.RESULT_ARRIVED) {
                    thermoValues = thermo.getCallResultImmediately(tempRequestUid, Thermometer_values.class);
                    break;
                }

                // if any error occured
                if (procState == CallRequestProcessingState.ERROR) {

                    // general call error
                    CallRequestProcessingError error = thermo.getCallRequestProcessingErrorOfLastCall();
                    printMessageAndExit("Getting temperature failed: " + error.getErrorType(), false);
                    
                    // specific call error
                    //DPA_AdditionalInfo dpaAddInfo = thermo.getDPA_AdditionalInfoOfLastCall();
                    //DPA_ResponseCode dpaResponseCode = dpaAddInfo.getResponseCode();
                    //printMessageAndExit("Getting temperature failed: " + error + ", DPA error: " + dpaResponseCode);
                    
                    break;
                } else {
                    System.out.println("Getting temperature hasn't been processed yet: " + procState);
                }
            }

            if (thermoValues != null) {
                
                // printing results        
                System.out.println("Temperature on the node " + node1.getId() + ": "
                        + thermoValues.getValue() + "." + thermoValues.getFractialValue() + " *C"
                );
                
                //String msqMqtt = thermoValues.toPrettyFormattedString();
                //Float temperature = Float.parseFloat(thermoValues.getValue() + "." + thermoValues.getFractialValue());
                
                // https://www.ietf.org/archive/id/draft-jennings-senml-10.txt
                String temperatureToBeSent = "{\"e\":["
			+ "{\"n\": \"temperature\"," + "\"u\": \"Cel\"," + "\"v\":" + thermoValues.getValue() + "." + thermoValues.getFractialValue() + "}],"
			+ "\"bn\":" + "\"urn:dev:mid:" + osInfo.getPrettyFormatedModuleId() + ":net-addr:" + node1.getId() + "\""
                        + "}";
                
                // try to get JsonObject using minimal-json
                //JsonObject jsonObject = Json.parse(temperatureToBeSent).asObject();
                        
                // send data to mqtt
                try {
                    mqttCommunicator.publish(pubTopic, 2, temperatureToBeSent.getBytes());
                    mqttCommunicator.publish(Topics.SENSOR_THERMOMETER, 2, temperatureToBeSent.getBytes());
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
    
    public static boolean sendDPARequest(String topic, String msgSenML) {
        
        // parse senml json msg request       
        JsonArray elements = Json.parse(msgSenML).asObject().get("e").asArray();
        
        for (JsonValue element : elements) {
            String value  = element.asObject().getString("sv", "");
            System.out.println("Published action: " + value);
        }
        
        // there is a need to select topic
        if (Topics.ACTUATOR_LEDR.equals(topic)) {
        
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
        } else if (Topics.ACTUATOR_LEDG.equals(topic)) {
            
            // getting LEDG interface
            LEDG ledg = node1.getDeviceObject(LEDG.class);
            if (ledg == null) {
                printMessageAndExit("LEDG doesn't exist or is not enabled", false);
                return false;
            }

            // TODO: - do action based on published request - pulsing for now
            VoidType setResult = ledg.pulse();
            if (setResult == null) {
                processNullResult(ledg, "Pulsing LEDG failed",
                        "Pulsing LEDG hasn't been processed yet"
                );
                return false;
            } 
        }
        
        return true;
    }
    
    // processes NULL result
    private static void processNullResult(GeneralLED led, String errorMsg, String notProcMsg) 
    {
        CallRequestProcessingState procState = led.getCallRequestProcessingStateOfLastCall();
        if ( procState == CallRequestProcessingState.ERROR ) {
            CallRequestProcessingError error = led.getCallRequestProcessingErrorOfLastCall();
            printMessageAndExit(errorMsg + ": " + error, false);
        } else {
            printMessageAndExit(notProcMsg + ": " + procState, false);
        }
    }

    @Override
    public void onAsynchronousMessage(DPA_AsynchronousMessage message) {
        
        // now only for testing
        
        System.out.println("New asynchronous message: ");
        
        System.out.println("Message source: "
            + "network ID= " + message.getMessageSource().getNetworkId()
            + ", node ID= " + message.getMessageSource().getNodeId()
            + ", peripheral number= " + message.getMessageSource().getPeripheralNumber()
        );
        
        System.out.println("Main data: " + message.getMainData());
        System.out.println("Additional data: " + message.getAdditionalData());
        System.out.println();
        
        
        // publishing async message
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
            mqttCommunicator.publish(pubTopic, 2, msqMqtt.getBytes());
        } catch (MqttException ex) {
            System.err.println("Error while publishing async dpa message: " + ex.getMessage());
        }
    }
}
