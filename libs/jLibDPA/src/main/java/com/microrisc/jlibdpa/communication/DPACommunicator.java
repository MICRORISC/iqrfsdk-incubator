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
package com.microrisc.jlibdpa.communication;


import com.microrisc.jlibdpa.configuration.DPAConfiguration;
import com.microrisc.jlibdpa.timing.TimeManager;
import com.microrisc.jlibdpa.types.DPARequest;
import com.microrisc.jlibiqrf.IQRFListener;
import com.microrisc.jlibiqrf.JLibIQRF;
import java.util.LinkedList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleJLibDPA poskytuje a implementuje základní rozhraní, kde předává data k
 * odeslání, zde se data odesílají, řídí se časování a synchronizuje se v rámci
 * vláken. Přijatá komunikace se rozlišuje a předává se do listenerů.
 *
 * @author Martin Strouhal
 */
public final class DPACommunicator implements IQRFListener {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(DPACommunicator.class);

    private final Thread sendingThread;
    private final LinkedList<CallableRequest> requestsToSend;
    private final Object sync_Sending;
    private volatile DPARequest lastSentRequest;
    private JLibIQRF iqrf;
    private TimeManager timeManager;

    public DPACommunicator() {
        requestsToSend = new LinkedList<>();
        sync_Sending = new Object();
        sendingThread = new SendingThread();
    }

    public void initSender(DPAConfiguration config) {
        logger.debug("initSender - start: config={}", config);
        try {
            timeManager = config.getTimeManagerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(ex.getLocalizedMessage());
            //TODO throw exception
        }
        timeManager.init(config);
        iqrf = JLibIQRF.init(config.getIQRFConfiguration());
        if (iqrf == null) {
            String txt = "IQRF layer cannot be created with specified parameters.";
            logger.error(txt);
            throw new IllegalArgumentException(txt);
        }
        //TODO maybe init result collector and etc.
        sendingThread.start();
        logger.debug("initSender - end");
    }

    public UUID invokeRequest(DPARequest request) {
        logger.debug("invokeRequest - start: request={}", request);
        CallableRequest callableRequest = new SimpleCallableRequest(request);
        synchronized (sync_Sending) {
            requestsToSend.add(callableRequest);
        }
        UUID uid = callableRequest.getUUID();
        logger.debug("invokeRequest - end: {}", uid);
        return uid;
    }


    @Override
    public void onGetIQRFData(short[] data) {
        // identify data
        DPAIdentifier.DPAReplyMessagesTypes type = DPAIdentifier.identify(lastSentRequest, data);

        // call specific listeners
    }

    private class SendingThread extends Thread {

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                if (timeManager.isFree()) {
                    synchronized (sync_Sending) {
                        if (requestsToSend.size() > 0) {
                            CallableRequest sendingRequest = requestsToSend.pop();
                            lastSentRequest = sendingRequest.getSourceRequest();

                            // sends request to iqrf layer
                            int sendResult = iqrf.sendData(sendingRequest.getData());
                            if (sendResult != JLibIQRF.SUCCESS_OPERATION) {
                                //todo add error
                            }

                            // start timing machine
                            timeManager.startWithUnknownTime();
                        }
                    }
                } else {
                    try {
                        this.sleep(timeManager.getRemaingTime());
                    } catch (InterruptedException ex) {
                        logger.warn(ex.getMessage());
                    }
                }
            }
        }
    }
}
