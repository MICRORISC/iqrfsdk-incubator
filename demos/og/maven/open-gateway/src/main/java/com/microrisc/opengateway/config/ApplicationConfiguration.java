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

package com.microrisc.opengateway.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds application configuration parameters.
 * 
 * @author Rostislav Spinar
 * @author Michal Konopa
 */
public final class ApplicationConfiguration {
    private final long pollingPeriod;
    private final String communicationInterface;
    private final Map<Integer, DeviceInfo> devicesInfoMap;
    
    
    /**
     * Creates new object holding information about application configuration parameters.
     * @param pollingPeriod polling period
     * @param communicationInterface communication interface
     * @param devicesInfoMap info about each device
     */
    public ApplicationConfiguration(
            long pollingPeriod, String communicationInterface, Map<Integer, DeviceInfo> devicesInfoMap
    ) {
        this.pollingPeriod = pollingPeriod;
        this.communicationInterface = communicationInterface;
        this.devicesInfoMap = new HashMap<>(devicesInfoMap);
    }
    
    /**
     * @return the number of devices
     */
    public long getNumberOfDevices() {
        return devicesInfoMap.size();
    }

    /**
     * @return the polling period
     */
    public long getPollingPeriod() {
        return pollingPeriod;
    }

    /**
     * @return the communication interface
     */
    public String getCommunicationInterface() {
        return communicationInterface;
    }

    /**
     * Returns information about devices. Information relating to device on
     * node, at which that device resides, is at the index equal to the node ID. 
     * @return map of information about devices on nodes
     */
    public Map<Integer, DeviceInfo> getDevicesInfoMap() {
        return devicesInfoMap;
    }
    
}
