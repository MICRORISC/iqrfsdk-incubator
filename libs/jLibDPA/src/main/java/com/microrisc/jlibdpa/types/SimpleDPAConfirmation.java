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
package com.microrisc.jlibdpa.types;

import com.microrisc.jlibdpa.DPAProperties;

/**
 *
 * @author Martin Strouhal
 */
public class SimpleDPAConfirmation implements DPAConfirmation {

    private final short dpaValue;
    private final short hops;
    private final short timeslot;
    private final short hopsResponse;
    
    public SimpleDPAConfirmation(short[] confirmationData){
        //TODO check data
        dpaValue = confirmationData[DPAProperties.DPA_VALUE_START];
        hops = confirmationData[DPAProperties.CONFIRMATION_HOPS_START];
        timeslot = confirmationData[DPAProperties.CONFIRMATION_TIMESLOT_START];
        hopsResponse = confirmationData[DPAProperties.CONFIRMATION_HOPS_RESPONSE_START];
    }
    
    @Override
    public short getDPAValue() {
        return dpaValue;
    }

    @Override
    public short getHops() {
        return hops;
    }

    @Override
    public short getTimeslot() {
        return timeslot;
    }

    @Override
    public short getHopsResponse() {
        return hopsResponse;
    }
}
