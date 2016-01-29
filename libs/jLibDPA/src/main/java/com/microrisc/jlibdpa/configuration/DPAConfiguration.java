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
package com.microrisc.jlibdpa.configuration;

import com.microrisc.jlibdpa.timing.TimeManager;
import com.microrisc.jlibiqrf.configuration.IQRFConfiguration;

/**
 *
 * @author Martin Strouhal
 */
public class DPAConfiguration {

    /** When isn't computed timing from confirmation, this timeout is used. This
     * timeout is default value. Time is in [s] */
    private static final int DEFAULT_UNKWNOWN_TIMEOUT = 5;
    /** When isn't computed timing from confirmation, this timeout is used. Time
     * is in [s] */
    private final int unknownTimeout;
    private Class<? extends TimeManager> timeManagerClass;
    
    private IQRFConfiguration iqrfConfig;

    //TODO improve by factory pattern
    
    public DPAConfiguration(IQRFConfiguration config, Class<? extends TimeManager> timeManager){
        this(config, timeManager, DEFAULT_UNKWNOWN_TIMEOUT);
    }
    
    public DPAConfiguration(IQRFConfiguration config, Class<? extends TimeManager> timeManager, int unknownTimeout) {
        this.unknownTimeout = unknownTimeout;
        iqrfConfig = config;
        timeManagerClass = timeManager;
    }

    public int getUnknownTimeout() {
        return unknownTimeout;
    }
    
    public IQRFConfiguration getIQRFConfiguration(){
        return iqrfConfig;
    }

    public Class<? extends TimeManager> getTimeManagerClass() {
        return timeManagerClass;
    }

    public int getCountOfResults() {
        //TODO count of results
        return 100;
    }
}
