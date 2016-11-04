/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.microrisc.opengateway.dpa;

/**
 * Response data to publish.
 * 
 * @author Michal Konopa
 */
public final class ResponseData {
    
    private final String n;
    private final String sv;
    private final String pid;
    private final String nodeId;
    private final String pnum;
    private final String pcmd;
    private final String hwpId;
    private final String responseCode;
    private final String dpaValue;
    private final String moduleId;
    
    
    /**
     * Creates new object of response data to publish.
     * @param n
     * @param sv
     * @param pid
     * @param nodeId
     * @param pnum
     * @param pcmd
     * @param hwpId
     * @param responseCode
     * @param dpaValue
     * @param moduleId 
     */
    public ResponseData(
            String n, String sv, String pid, String nodeId, String pnum,
            String pcmd, String hwpId, String responseCode, String dpaValue, 
            String moduleId
    ) {
        this.n = n;
        this.sv = sv;
        this.pid = pid;
        this.nodeId = nodeId;
        this.pnum = pnum;
        this.pcmd = pcmd;
        this.hwpId = hwpId;
        this.responseCode = responseCode;
        this.dpaValue = dpaValue;
        this.moduleId = moduleId;
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
    public String getPid() {
        return pid;
    }

    /**
     * @return the nodeId
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * @return the pnum
     */
    public String getPnum() {
        return pnum;
    }

    /**
     * @return the pcmd
     */
    public String getPcmd() {
        return pcmd;
    }

    /**
     * @return the HWP id
     */
    public String getHwpId() {
        return hwpId;
    }

    /**
     * @return the response code
     */
    public String getResponseCode() {
        return responseCode;
    }

    /**
     * @return the DPA value
     */
    public String getDpaValue() {
        return dpaValue;
    }

    /**
     * @return the module ID
     */
    public String getModuleId() {
        return moduleId;
    }
    
}
