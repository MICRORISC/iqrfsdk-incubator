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
package com.microrisc.opengateway.dpa;

import com.microrisc.simply.errors.CallRequestProcessingError;
import com.microrisc.simply.iqrf.dpa.v22x.types.DPA_AdditionalInfo;

/**
 * Holds information about DPA request's result.  
 * Operation result, DPA additional value and possible error
 * 
 * @author Michal Konopa
 */
public final class DPA_Result {
    
    /** Data about issued request. */
    public static class Request {
        private final String nadr;
        private final int pnum;
        private final String pcmd;
        private final String moduleId;
        
        
        /**
         * Creates new object of the issued request.
         * 
         * @param nadr
         * @param pnum
         * @param pcmd
         * @param moduleId 
         */
        public Request(String nadr, int pnum, String pcmd, String moduleId) {
            this.nadr = nadr;
            this.pnum = pnum;
            this.pcmd = pcmd;
            this.moduleId = moduleId;
        }

        /**
         * @return the nadr
         */
        public String getNadr() {
            return nadr;
        }

        /**
         * @return the pnum
         */
        public int getPnum() {
            return pnum;
        }

        /**
         * @return the pcmd
         */
        public String getPcmd() {
            return pcmd;
        }

        /**
         * @return the module id
         */
        public String getModuleId() {
            return moduleId;
        }
        
    }
    
    // operational result
    private final Object opResult;
    
    // potential error
    private final CallRequestProcessingError error;
    
    // additional information
    private final DPA_AdditionalInfo dpaAddInfo;
    
    // info about issued request
    private final Request request;
    
    
    /**
     * Creates object of result.
     * 
     * @param opResult result of DPA operation
     * @param error potential error
     * @param dpaAddInfo DPA additional info
     * @param request issued request
     */
    public DPA_Result(
        Object opResult, CallRequestProcessingError error, DPA_AdditionalInfo dpaAddInfo,
        Request request
    ) {
        this.opResult = opResult;
        this.error = error;
        this.dpaAddInfo = dpaAddInfo;
        this.request = request;
    }

    /**
     * @return result of DPA operation
     */
    public Object getOpResult() {
        return opResult;
    }

    /**
     * @return the error
     */
    public CallRequestProcessingError getError() {
        return error;
    }

    /**
     * @return the DPA additional info
     */
    public DPA_AdditionalInfo getDpaAddInfo() {
        return dpaAddInfo;
    }

    /**
     * @return the request
     */
    public Request getRequest() {
        return request;
    }

}
