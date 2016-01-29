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

/**
 * Provides services for communicating via JLibIQRF to IQRF network.
 *
 * @author Martin Strouhal
 */
public class SimpleJLibDPA implements JLibDPA {

    private final DPACommunicator communicator;
    private final ResultCollector resultCollector;
    private final ListenersManager listenersManager;

    public SimpleJLibDPA(DPAConfiguration config) {
        resultCollector = new ResultCollector(config);
        listenersManager = new ListenersManager();
        listenersManager.registerNewListener(resultCollector);
        communicator = new DPACommunicator();
        communicator.initSender(config, listenersManager);
    }


    @Override
    public DPAResponse sendDPARequest(DPARequest request) {
        communicator.invokeRequest(request);
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public UUID sendAsyncDPARequest(DPARequest request) {
        return communicator.invokeRequest(request);
    }

    @Override
    public DPAResponse getAsyncResult(UUID uid) {
        return resultCollector.getResult(uid);
    }

    @Override
    public void addReceivingListener(DPAReceiver receiver) {
        listenersManager.registerNewListener(receiver);
    }

    @Override
    public void removeReceivingListener(DPAReceiver receiver) {
        listenersManager.unregisterListener(receiver);
    }

    @Override
    public void destroy() {
        listenersManager.unregisterAllListeners();
        resultCollector.destroy();
        //TODO destroy communicator
    }
}
