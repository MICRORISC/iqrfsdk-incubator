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

import com.microrisc.jlibdpa.communication.DPAReceiver;
import com.microrisc.jlibdpa.types.DPARequest;
import com.microrisc.jlibdpa.types.DPAResponse;
import java.util.UUID;

/**
 *  Public interface for using JLibDPA library.
 * 
 * @author Martin Strouhal
 */
public interface JLibDPA {
    
    public DPAResponse sendDPARequest(DPARequest request);
    
    public UUID sendAsyncDPARequest(DPARequest request);
    
    public DPAResponse getAsyncResult(UUID uid);
    
    public void addReceivingListener(DPAReceiver receiver);
    
    public void removeReceivingListener(DPAReceiver receiver);
    
    public void destroy();
    
}
