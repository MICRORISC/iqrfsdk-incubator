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
package com.microrisc.jlibdpa.communication.receiving;

import com.microrisc.jlibdpa.types.DPAConfirmation;
import com.microrisc.jlibdpa.types.DPAResponse;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Martin Strouhal
 */
public class ListenersManager {

    private final Set<DPAReceiver> listeners;

    public ListenersManager(){
        listeners = new LinkedHashSet<>();
    }
    
    //TODO checking
    public void registerNewListener(DPAReceiver receiver) {
        listeners.add(receiver);
    }

    public void unregisterListener(DPAReceiver receiver) {
        listeners.remove(receiver);
    }

    public void unregisterAllListeners() {
        listeners.clear();
    }
    
    public void callListenerResponse(DPAResponse response){
        for (DPAReceiver listener : listeners) {
            listener.onGetResponse(response);
        }
    }
    
    public void callListenerConfirmation(DPAConfirmation confirmation){
        for (DPAReceiver listener : listeners) {
            listener.onGetConfirmation(confirmation);
        }
    }
    
    public void callListenerAsyncMsg(){
        for (DPAReceiver listener : listeners) {
            listener.onGetAsyncMsg();
        }
    }
}
