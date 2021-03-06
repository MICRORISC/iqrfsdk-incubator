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

import com.microrisc.jlibdpa.types.DPARequest;
import java.util.UUID;

/**
 *
 * @author Martin Strouhal
 */
public class SimpleCallableRequest implements CallableRequest {
    
    private final DPARequest sourceRequest;
    private final UUID uid;

    public SimpleCallableRequest(DPARequest request) {
        //TODO check parameters
        this.sourceRequest = request;
        this.uid = UUID.randomUUID();
    }
    
    @Override
    public short[] getData() {
        return sourceRequest.getAllData();
    }

    @Override
    public UUID getUUID() {
        return uid;
    }

    @Override
    public DPARequest getSourceRequest() {
        return sourceRequest;
    }
    
}
