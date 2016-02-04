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

import com.microrisc.jlibdpa.DPAProperties;
import com.microrisc.jlibdpa.types.DPARequest;
import com.microrisc.jlibdpa.types.DPAResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Providing services for identification of received data.
 *
 * @author Martin Strouhal
 */
public final class DPAIdentifier {

    private static final Logger log = LoggerFactory.getLogger(DPAIdentifier.class);

    public enum DPAReplyMessagesTypes {
        CONFIRMATION,
        RESPONSE,
        ASYNC,
        UNKNOWN;
    }

    public static DPAReplyMessagesTypes identify(DPARequest lastRequest, short[] receivedData) {
        log.debug("identify - start: lastRequest={}, receivedData={}", lastRequest, receivedData);
        DPAReplyMessagesTypes typeToReturn = DPAReplyMessagesTypes.UNKNOWN;
        // checking for illegall data
        if (receivedData != null && receivedData.length >= DPAProperties.HW_PROFILE_START + DPAProperties.HW_PROFILE_LENGTH) {
            // checking for response
            if (receivedData[DPAProperties.PCMD_START] == (lastRequest.getCommand() & 0x80)) {
                typeToReturn = DPAReplyMessagesTypes.RESPONSE;
            }

            // checking confirmation
            if (receivedData[DPAProperties.RESPONSE_CODE_START] == DPAResponseCode.CONFIRMATION.getCodeValue()) {
                typeToReturn = DPAReplyMessagesTypes.CONFIRMATION;
            }
            //TODO identification of async msg
           
            if (false) {
                typeToReturn = DPAReplyMessagesTypes.ASYNC;
            }
        }

        log.debug("identify - end: {}", typeToReturn);
        return typeToReturn;
    }

}
