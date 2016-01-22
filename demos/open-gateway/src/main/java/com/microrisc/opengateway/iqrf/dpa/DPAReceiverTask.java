/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.microrisc.opengateway.iqrf.dpa;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author spinarr
 */
public class DPAReceiverTask extends TimerTask {
    
    public DPAReceiverTask () {
        
    }    
    
    @Override
    public void run() {
        System.out.println("Timer task started at:"+new Date());
        receiver();
        System.out.println("Timer task finished at:"+new Date());
    }
 
    private void receiver() {
        
        // check uuids
        // get dpa responses
        // create json message
        // publish json
        
    }
}
