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
package com.microrisc.jlibdpa.dpaTypes;

import com.microrisc.jlibdpa.DPAProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martin Strouhal
 */
public class SimpleDPARequest implements DPARequest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleDPARequest.class);

    private final short[] data;

    public SimpleDPARequest(short[] requestData) {
        //TODO check data for maximal length of additional data
        this.data = checkData(requestData);
    }

    private short[] checkData(short[] dataToCheck) {
        if (dataToCheck == null) {
            logger.info("Checking data are null. It will be used data with length 0.");
            return new short[0];
        } else {
            return dataToCheck;
        }
    }

    @Override
    public int getAdress() {
        int address = data[DPAProperties.NADR_START];
        address <<= 8;
        address += data[DPAProperties.NADR_START+1];
        return address;
    }

    @Override
    public int getPeripheral() {
        return data[DPAProperties.PNUM_START];
    }

    @Override
    public int getCommand() {
        return data[DPAProperties.PCMD_START];
    }

    @Override
    public int getHWPID() {
        int hwpid = data[DPAProperties.HW_PROFILE_START];
        hwpid <<= 8;
        hwpid += data[DPAProperties.HW_PROFILE_START+1];
        return hwpid;
    }

    @Override
    public short[] getAdditionalData() {
        short[] addData = new short[data.length + 1 - DPAProperties.PDATA_START];
        System.arraycopy(data, DPAProperties.PDATA_START, addData, 0, addData.length);
        return addData;
    }

    @Override
    public short[] getAllData() {
        //prevent before change array field
        return data == null ? null : data.clone();
    }
}
