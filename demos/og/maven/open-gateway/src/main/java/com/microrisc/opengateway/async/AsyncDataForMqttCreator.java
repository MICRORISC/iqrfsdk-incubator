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

import com.microrisc.simply.iqrf.dpa.asynchrony.DPA_AsynchronousMessage;
import com.microrisc.simply.iqrf.dpa.v22x.types.OsInfo;

/**
 * Creator of {@link AsyncDataForMqtt} objects.
 * 
 * @author Michal Konopa
 * @author Rostislav Spinar
 */
public final class AsyncDataForMqttCreator {
            
    /**
     * From specified DPA asynchronous message creates corresponding 
     * {@link AsyncDataForMqtt} object.
     * 
     * @param dpaAsyncMessage source DPA asynchronous message
     * @param osInfo OS info relating to source node
     * @return AsyncDataForMqtt corresponding to source DPA asynchronous message
     * @throws com.microrisc.opengateway.async.AsyncDataForMqttCreatorException
     *         if some error occurs during process of creation
     */
    public static AsyncDataForMqtt create(
            DPA_AsynchronousMessage dpaAsyncMessage, OsInfo osInfo
    )
        throws AsyncDataForMqttCreatorException 
    {
        if ( dpaAsyncMessage.getMessageSource() == null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message source not known.");
        }
        
        if ( dpaAsyncMessage.getMessageSource().getNodeId() == null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message source node not known.");
        }
        
        if ( dpaAsyncMessage.getMainData() == null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message main data not known.");
        }
        
        if ( dpaAsyncMessage.getAdditionalData() == null ) {
            throw new AsyncDataForMqttCreatorException("Asynchronous message additional data not known.");
        }
        
        short[] mainData = dpaAsyncMessage.getMainData();
        if ( mainData.length == 0 ) {
            throw new AsyncDataForMqttCreatorException(
                "No asynchronously received data from the node: "
                + dpaAsyncMessage.getMessageSource().getNodeId()
            );
        }
        
        //for (int i = 0; i < mainData.length; i++) {
        //    System.out.println("Async main data: " + mainData[i]);
        //}
        
        return new AsyncDataForMqtt(
                getModuleState(mainData), 
                getModuleId(osInfo), 
                dpaAsyncMessage.getMessageSource().getNodeId(), 
                dpaAsyncMessage.getMessageSource().getPeripheralNumber(),
                dpaAsyncMessage.getAdditionalData()
        );
    }
    
    // return module STATE
    private static String getModuleState(short[] moduleData) {
    
        String state = "unknown";
        
        if ((moduleData[2] & 0x01) == 0x01) {
            state = "leftup";
        } 
        else if ((moduleData[2] & 0x02) == 0x02) {
            state = "leftdown";
        }
        else if ((moduleData[2] & 0x04) == 0x04) {
            state = "rightup";
        }
        else if ((moduleData[2] & 0x08) == 0x08) {
            state = "rightdown";
        }

        return state;
    }
    
    // return module ID
    private static String getModuleId(OsInfo osInfo) {
        if ( osInfo != null ) {
            return osInfo.getPrettyFormatedModuleId();
        }
        return "unknown";
    }
}
