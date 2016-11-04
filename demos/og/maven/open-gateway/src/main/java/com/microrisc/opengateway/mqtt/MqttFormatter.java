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

package com.microrisc.opengateway.mqtt;

import com.microrisc.opengateway.async.AsyncDataForMqtt;
import com.microrisc.opengateway.dpa.ResponseData;
import com.microrisc.simply.iqrf.dpa.v22x.devices.Custom;
import com.microrisc.simply.iqrf.dpa.v22x.devices.IO;
import com.microrisc.simply.iqrf.dpa.v22x.protocol.DPA_ProtocolProperties;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;

/**
 * Formats various types od sensor data to MQTT form.
 *
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public final class MqttFormatter {
    
    /**
     * Returns formated value of CO2.
     *
     * @param co2 CO2 value
     * @param moduleId ID of source module
     * @return formated value of CO2
     */
    public static String formatCO2(String co2, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"co2\"," + "\"u\":\"PPM\"," + "\"v\":" + co2 + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }

    /**
     * Returns formated value of VOC.
     *
     * @param voc VOC value
     * @param moduleId ID of source module
     * @return formated value of VOC
     */
    public static String formatVOC(String voc, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"voc\"," + "\"u\":\"PPM\"," + "\"v\":" + voc + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }

    /**
     * Returns formated value of temperature.
     *
     * @param temperature temperature
     * @param moduleId ID of source module
     * @return formated value of temperature
     */
    public static String formatTemperature(String temperature, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + temperature + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }

    /**
     * Returns formated value of humidity.
     *
     * @param humidity humidity
     * @param moduleId ID of source module
     * @return formated value of humidity
     */
    public static String formatHumidity(String humidity, String moduleId) {
        return "{\"e\":["
                + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }
    
    /**
     * Returns formatted value of specified response data.
     * 
     * @param responseData response data
     * @return formatted value of response data
     */
    public static String formatResponseData(ResponseData responseData) {
        String levelStr = "";
        if ( 
            (responseData.getN().toLowerCase().equals("custom")) 
            && 
            (responseData.getSv().toLowerCase().equals("up") || responseData.getSv().toLowerCase().equals("down"))
        ) {
            levelStr = ", \"v\":" + "0";
        }
                        
        String firstLine = "{\"e\":[{\"n\":\"" + responseData.getN().toLowerCase() 
                + "\"," + "\"sv\":" + "\"" + responseData.getSv().toLowerCase() + "\""
                + levelStr
                + "}],";
        
        return firstLine
                + "\"iqrf\":[{\"pid\":" + responseData.getPid() + "," 
                + "\"dpa\":\"resp\"," + "\"nadr\":" + responseData.getNodeId() + ","
                + "\"pnum\":" + responseData.getPnum() + ","
                + "\"pcmd\":" + "\"" + responseData.getPcmd() + "\","
                + "\"hwpid\":" + responseData.getHwpId()
                + "," + "\"rcode\":" + "\"" + responseData.getResponseCode() + "\","
                + "\"dpavalue\":" + responseData.getDpaValue() + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + responseData.getModuleId() + "\""
                + "}";
    }
    
    /**
     * Returns formated DPA asynchronous message data.
     *
     * @param asyncMsgForMqtt DPA asynchronous message data for MQTT
     * @param pid identifier of asynchronous packet
     * @return MQTT form of {@code asyncMsgForMqtt}
     */
    public static String formatAsyncDataForMqtt(AsyncDataForMqtt asyncMsgForMqtt, int pid) {
        return "{\"e\":[{\"n\":\"switch\"," + "\"sv\":" + "\"" + asyncMsgForMqtt.getModuleState() + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pid++ + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + asyncMsgForMqtt.getNodeId() + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                + "\"hwpid\":" + asyncMsgForMqtt.getHwpid() +  "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + "unknown" + "\""
                + "}";
    }
    
    /**
     * Returns formated value of Devtech request message.
     *
     * @param state to be set
     * @param pidDevtech identifier of packet
     * @return formated MQTT message
     */
    public static String formatDeviceDevtech(String state, int pidDevtech) {
        int devtechNodeId = 0x03;
        int devtechHWPID = 0xFFFF;
        String devtechModuleId = "8100401F";

        return "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"" + state + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pidDevtech++ + "," + "\"dpa\":\"req\"," + "\"nadr\":" + devtechNodeId + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                + "\"hwpid\":" + devtechHWPID + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + devtechModuleId + "\""
                + "}";
    }
    
    /**
     * Returns formated value of Devtech request message.
     *
     * @param state to be set
     * @param pidDatmolux identifier of packet
     * @return formated MQTT message
     */
    public static String formatDeviceDatmolux(String state, int pidDatmolux) {
        int datmoluxNodeId = 0x04;
        int datmoluxHWPID = 0xFFFF;
        String datmoluxModuleId = "8100401F";

        return "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"" + state + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pidDatmolux + "," + "\"dpa\":\"req\"," + "\"nadr\":" + datmoluxNodeId + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                + "\"hwpid\":" + datmoluxHWPID + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + datmoluxModuleId + "\""
                + "}";
    }
    
    /**
     * Returns formated value of Devtech request message.
     *
     * @param state to be set
     * @param pidAustyn identifier of packet
     * @return formated MQTT message
     */
    public static String formatDeviceAustyn(String state, int pidAustyn) {
        int austynNodeId = 0x02;
        int austynHWPID = 0xFFFF;
        String austynModuleId = "8100401F";

        return "{\"e\":[{\"n\":\"io\"," + "\"sv\":" + "\"" + state + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pidAustyn++ + "," + "\"dpa\":\"req\"," + "\"nadr\":" + austynNodeId + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.IO + "," + "\"pcmd\":" + "\"" + IO.MethodID.SET_OUTPUT_STATE.name().toLowerCase() + "\","
                + "\"hwpid\":" + austynHWPID + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + austynModuleId + "\""
                + "}";
    }
    
    /**
     * Returns formated value of Devtech request message.
     *
     * @param state to be set
     * @param pidTeco identifier of packet
     * @return formated MQTT message
     */
    public static String formatDeviceTeco(String state, int pidTeco) {
        int tecoNodeId = 0x05;
        int tecoHWPID = 0xFFFF;
        String tecoModuleId = "8100401F";

        return "{\"e\":[{\"n\":\"custom\"," + "\"sv\":" + "\"" + state + "\"}],"
                + "\"iqrf\":[{\"pid\":" + pidTeco + "," + "\"dpa\":\"req\"," + "\"nadr\":" + tecoNodeId + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                + "\"hwpid\":" + tecoHWPID + "}],"
                + "\"bn\":" + "\"urn:dev:mid:" + tecoModuleId + "\""
                + "}";
    }
    
    /**
     * Returns formated value of Devtech request message.
     *
     * @param nodeId
     * @param moduleId
     * @param dpaAddInfo
     * @param temperature
     * @param humidity
     * @param pid identifier of packet
     * 
     * @return formated MQTT message
     */
    public static String formatDeviceIqhome(
            int nodeId, String moduleId, DPA_AdditionalInfo dpaAddInfo, 
            String temperature, String humidity, int pid
    ) {
        return 
                "{\"e\":["
                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + temperature + "},"
                + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "}"
                + "],"
                + "\"iqrf\":["
                + "{\"pid\":" + pid++ + "," + "\"dpa\":\"resp\"," + "\"nadr\":" + nodeId + ","
                + "\"pnum\":" + DPA_ProtocolProperties.PNUM_Properties.USER_PERIPHERAL_START + "," + "\"pcmd\":" + "\"" + Custom.MethodID.SEND.name().toLowerCase() + "\","
                + "\"hwpid\":" + dpaAddInfo.getHwProfile() + "," + "\"rcode\":" + "\"" + dpaAddInfo.getResponseCode().name().toLowerCase() + "\","
                + "\"dpavalue\":" + dpaAddInfo.getDPA_Value() + "}"
                + "],"
                + "\"bn\":" + "\"urn:dev:mid:" + moduleId + "\""
                + "}";
    }
    
    /**
     * Returns formated value of Protronix response message.
     *
     * @param nodeId
     * @param clientId
     * @param co2
     * @param temperature
     * @param humidity
     * 
     * @return formated MQTT message
     */
    public static String formatDeviceProtronix(int nodeId, String clientId, String co2, String temperature, String humidity) {

        String timeSec = Long.toString( System.currentTimeMillis() / 1000);
        String timeMsec = Long.toString( System.currentTimeMillis() % 1000);

        return "["
                + "{\"bn\":" + "\"urn:clid:" + clientId + ":ba:" + nodeId + "\"," + "\"bt\":" + timeSec + "." + timeMsec + "},"
                + "{\"n\":\"co2\"," + "\"u\":\"ppm\"," + "\"v\":" + co2 + "},"
                + "{\"n\":\"humidity\"," + "\"u\":\"%RH\"," + "\"v\":" + humidity + "},"
                + "{\"n\":\"temperature\"," + "\"u\":\"Cel\"," + "\"v\":" + temperature + "}"
                + "]";
    }
    
    /**
     * Returns formated value of specified error string.
     *
     * @param error error message
     * @return formated error message
     */
    public static String formatError(String error) {
        return "{\"e\":["
                + "{\"n\":\"error\"," + "\"u\":\"description\"," + "\"v\":" + "\"" + error + "\"}"
                + "]}";
    }
}
