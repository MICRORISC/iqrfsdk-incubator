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
package com.microrisc.opengateway.async;

import com.microrisc.simply.iqrf.dpa.DPA_ResponseCode;

/**
 * Data from DPA asynchronous message combined with some other data for use in MQTT.
 * 
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public final class AsyncDataForMqtt { 
    
    // module data
    private final String moduleState;
    
    // ID of source module
    private final String moduleId;
        
    // ID of source node
    private final String nodeId;
    
    // source peripheral ID
    private final int pnum;
    
    // HW profile ID
    private final int hwpid;
    
    
    /**
     * Creates new DPA asynchronous message data for MQTT.
     *
     * @param moduleState module satte
     * @param moduleId ID of source module
     * @param nodeId source node ID
     * @param pnum source peripheral ID
     * @param hwpid HW profile ID
     */
    public AsyncDataForMqtt(
            String moduleState, String moduleId, String nodeId, int pnum, int hwpid
    ) {
        this.moduleState = moduleState;
        this.moduleId = moduleId;
        this.nodeId = nodeId;
        this.pnum = pnum;
        this.hwpid = hwpid;
    }
    
    /**
     * @return the module data
     */
    public String getModuleState() {
        return moduleState;
    }
    
    /**
     * @return the module ID
     */
    public String getModuleId() {
        return moduleId;
    }
    
    /**
     * @return the source node ID
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return the source peripheral number
     */
    public int getPnum() {
        return pnum;
    }

    /**
     * @return the HW profile ID
     */
    public int getHwpid() {
        return hwpid;
    }

}
