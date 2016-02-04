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
package com.microrisc.jlibdpa;

import com.microrisc.jlibdpa.communication.DPACommunicator;
import com.microrisc.jlibdpa.communication.receiving.DPAReceiver;
import com.microrisc.jlibdpa.communication.receiving.ListenersManager;
import com.microrisc.jlibdpa.configuration.DPAConfiguration;
import com.microrisc.jlibdpa.types.DPARequest;
import com.microrisc.jlibdpa.types.DPAResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides services for communicating via JLibIQRF to IQRF network.
 *
 * @author Martin Strouhal
 */
public class SimpleJLibDPA implements JLibDPA {

    private final static Logger log = LoggerFactory.getLogger(SimpleJLibDPA.class);

    private final DPACommunicator communicator;
    private final ResultCollector resultCollector;
    private final ListenersManager listenersManager;

    public SimpleJLibDPA(DPAConfiguration config) {
        log.debug("SimpleJLibDPA - start: config={}", config);
        resultCollector = new ResultCollector(config);
        listenersManager = new ListenersManager();
        listenersManager.registerNewListener(resultCollector);
        communicator = new DPACommunicator(listenersManager);
        communicator.initSender(config);
        log.debug("SimpleJLibDPA - end");
    }


    @Override
    public DPAResponse sendDPARequest(DPARequest request) {
        log.debug("sendDPARequest - start: request={}", request);
        UUID uid = communicator.invokeRequest(request);
        DPAResponse response = resultCollector.getResult(uid);
        log.debug("sendDPARequest - end: {}", response);
        return response;
    }

    @Override
    public UUID sendAsyncDPARequest(DPARequest request) {
        log.debug("sendAsyncDPARequest - start: request={}", request);
        UUID uid = communicator.invokeRequest(request);
        log.debug("sendAsyncDPARequest - end: {}", uid);
        return uid;
    }

    @Override
    public DPAResponse getAsyncResult(UUID uid) {
        log.debug("getAsyncResult - start: uid={}", uid);
        DPAResponse response = resultCollector.getResult(uid);
        log.debug("getAsyncResult - end: {}", response);
        return response;
    }

    @Override
    public void addReceivingListener(DPAReceiver receiver) {
        log.debug("addReceivingListener - start: receiver={}", receiver);
        // receiver is checked while registering
        listenersManager.registerNewListener(receiver);
        log.debug("addReceivingListener - end");
    }

    @Override
    public void removeReceivingListener(DPAReceiver receiver) {
        log.debug("removeReceivingListener - start: receiver={}", receiver);
        // receiver is checked while unregistering
        listenersManager.unregisterListener(receiver);
        log.debug("removeReceivingListener - end");
    }

    @Override
    public void destroy() {
        log.debug("destroy - start");
        listenersManager.unregisterAllListeners();
        resultCollector.destroy();
        //TODO destroy communicator
        log.debug("destroy - end");
    }
}
