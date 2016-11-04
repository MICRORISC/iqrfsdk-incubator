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

/**
 * Holds information about concrete device.
 * 
 * @author Rostislav Spinar
 * @author Michal Konopa
 */
public final class DeviceInfo {
    private final long id;
    private final String manufacturer;
    private final String type;
    
    
    /**
     * Creates new object of device identification.
     * @param id ID of the device
     * @param manufacturer manufacturer name
     * @param type type
     */
    public DeviceInfo(long id, String manufacturer, String type) {
        this.id = id;
        this.manufacturer = manufacturer;
        this.type = type;
    }
    
    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @return the manufacturer
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

}
