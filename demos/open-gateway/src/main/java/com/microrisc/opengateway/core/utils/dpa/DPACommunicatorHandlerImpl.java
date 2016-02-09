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

package com.microrisc.opengateway.core.utils.dpa;

import com.microrisc.simply.Network;
import com.microrisc.simply.Node;
import com.microrisc.simply.SimplyException;
import com.microrisc.simply.asynchrony.AsynchronousMessagesListener;
import com.microrisc.simply.asynchrony.AsynchronousMessagingManager;
import com.microrisc.simply.iqrf.dpa.DPA_Simply;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessageProperties;
import com.microrisc.simply.iqrf.dpa.v22x.DPA_SimplyFactory;
import java.util.Timer;
import java.util.TimerTask;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rostislav Spinar
 */
public class DPACommunicatorHandlerImpl implements AsynchronousMessagesListener<DPA_AsynchronousMessage> {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(DPACommunicatorHandlerImpl.class);
    
    // reference to DPA Simply
    private DPA_Simply simply = null;

    // reference to async manager
    private AsynchronousMessagingManager<DPA_AsynchronousMessage, DPA_AsynchronousMessageProperties> asyncManager;

    // nodes in the network
    private List<Node> listOfNodes = null;

    private final int countOfUsedNodes;

    public DPACommunicatorHandlerImpl(int countOfUsedNodes) {
        this.countOfUsedNodes = countOfUsedNodes;
    }

    public void initDPASimplyAndGetNodes() {
        // nodes in network
        listOfNodes = new LinkedList<>();

        // getting dpa simply
        try {
            simply = DPA_SimplyFactory.getSimply(
                    "config" + File.separator + "Simply.properties");
        } catch (SimplyException ex) {
            printMessage("Error while creating Simply: " + ex.getMessage());
        }

        // getting network "1"
        Network network1 = simply.getNetwork("1", Network.class);
        if (network1 == null) {
            printMessage("Network 1 doesn't exist");
        }

        // getting coordinator
        Node nodeCoordinator = network1.getNode("0");
        if (nodeCoordinator == null) {
            printMessage("Coordinator doesn't exist");
        }

        listOfNodes.add(nodeCoordinator);

        for (int i = 1; i < countOfUsedNodes; i++) {
            // getting nodes
            Node actualNode = network1.getNode(Integer.toString(i));
            if (actualNode == null) {
                printMessage("Node " + i + " doesn't exist");
                continue;
            }

            listOfNodes.add(actualNode);
        }
    }

    public void runTasks() {

        // dpa periodic task - sending requests based on fixed period
        TimerTask periodicTask = new PeriodicTask();
        Timer timerPeriodic = new Timer(true);
        timerPeriodic.scheduleAtFixedRate(periodicTask, 0, 1000);
        System.out.println("Periodic DPAPeriodicTask started");

        // dpa receiver task - checking for responses
        TimerTask receiverTask = new ReceiverTask();
        Timer timerReceiver = new Timer(true);
        timerReceiver.scheduleAtFixedRate(receiverTask, 0, 1000);
        System.out.println("Periodic DPAReceiverTask started");
    }

    @Override
    public void onAsynchronousMessage(DPA_AsynchronousMessage message) {

        // printing received async msg
        System.out.println("New message: ");

        System.out.println("Message source: "
                + "network ID= " + message.getMessageSource().getNetworkId()
                + ", node ID= " + message.getMessageSource().getNodeId()
                + ", peripheral number= " + message.getMessageSource().getPeripheralNumber()
        );

        System.out.println("Main data: " + message.getMainData());
        System.out.println();
        
        // TODO
        // create DPA request
        // DPACommunicatorHandlerImpl.send(DPARequest)
        // store Request UUID
        
        // TODO
        // create JSON Message
        // MQTTController.publish(JSONMessage)
    }

    // prints out specified message
    private void printMessage(String message) {

        // add quiteMode
        System.out.println(message);
    }
}
