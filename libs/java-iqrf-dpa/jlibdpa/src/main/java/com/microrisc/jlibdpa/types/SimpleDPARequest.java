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
import com.microrisc.jlibdpa.convertors.AbstractResponseConvertor;
import com.microrisc.jlibdpa.convertors.SimpleResponseConvertor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Martin Strouhal
 */
public class SimpleDPARequest implements DPARequest {

    private static final Logger log = LoggerFactory.getLogger(SimpleDPARequest.class);

    private final short[] data;

    public SimpleDPARequest(short[] requestData) {
        log.debug("SimpleDPARequest - start: requestData={}", requestData);
        //TODO check data for maximal length of additional data
        this.data = checkData(requestData);
        log.debug("SimpleDPARequest - end");
    }

    private short[] checkData(short[] dataToCheck) {
        log.debug("checkData - start: dataToCheck={}", dataToCheck);
        if (dataToCheck == null) {
            log.info("checkData - end: Checking data are null. It will be used data with length 0.");
            return new short[0];
        } else {
            log.debug("checkData - end: Data are correctly");
            return dataToCheck;
        }
    }

    @Override
    public int getAdress() {
        log.debug("getAddress - start");
        int address = data[DPAProperties.NADR_START];
        address <<= 8;
        address += data[DPAProperties.NADR_START + 1];
        log.debug("getAddress - end: {}", address);
        return address;
    }

    @Override
    public int getPeripheral() {
        log.debug("getPeripheral - start");
        int peripheral = data[DPAProperties.PNUM_START];
        log.debug("getPeripheral - end: {}", peripheral);
        return peripheral;
    }

    @Override
    public int getCommand() {
        log.debug("getCommand - start");
        int cmd = data[DPAProperties.PCMD_START];
        log.debug("getCommand - end: {}", cmd);
        return cmd;
    }

    @Override
    public int getHWPID() {
        log.debug("getHWPID - start");
        int hwpid = data[DPAProperties.HW_PROFILE_START];
        hwpid <<= 8;
        hwpid += data[DPAProperties.HW_PROFILE_START + 1];
        log.debug("getHWPID - end: {}", hwpid);
        return hwpid;
    }

    @Override
    public short[] getAdditionalData() {
        log.debug("getAdditionalData - start");
        short[] addData = new short[data.length + 1 - DPAProperties.PDATA_START];
        System.arraycopy(data, DPAProperties.PDATA_START, addData, 0, addData.length);
        log.debug("getAdditionalData - end: {}", addData);
        return addData;
    }

    @Override
    public short[] getAllData() {
        log.debug("getAllData - start");
        log.debug("getAllData - end: copy of {}", data);
        //prevent before change array field
        return data == null ? null : data.clone();
    }

    @Override
    public Class<? extends AbstractResponseConvertor> getResponseConvertor() {
        return SimpleResponseConvertor.class;
    }
}
