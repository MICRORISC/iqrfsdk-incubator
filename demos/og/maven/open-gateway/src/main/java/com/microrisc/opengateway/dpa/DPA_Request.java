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

/**
 * Encapsulates data of DPA request.
 * 
 * @author Michal Konopa
 */
public final class DPA_Request {
    
    // TODO: add comments - what is the meaning of each field?
    private final String n;
    private final String sv;
    
    private final int pid;
    private final String dpa;
    private final int nadr; 
    
    
    /**
     * Creates new DPA request filled by specified data.
     * @param n
     * @param sv
     * @param pid
     * @param dpa
     * @param nadr
     */
    public DPA_Request(String n, String sv, int pid, String dpa, int nadr) {
        // TODO: add checking of arguments values 
        this.n = n;
        this.sv = sv;
        this.pid = pid;
        this.dpa = dpa;
        this.nadr = nadr;
    }

    /**
     * @return the n
     */
    public String getN() {
        return n;
    }

    /**
     * @return the sv
     */
    public String getSv() {
        return sv;
    }

    /**
     * @return the pid
     */
    public int getPid() {
        return pid;
    }

    /**
     * @return the dpa
     */
    public String getDpa() {
        return dpa;
    }

    /**
     * @return the nadr
     */
    public int getNadr() {
        return nadr;
    }
    
    
}
