/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.microrisc.opengateway.iqrf.dpa;

import java.util.Date;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author spinarr
 */
public class DPAPeriodicTask extends TimerTask {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(DPAPeriodicTask.class);

    /** Runnable method which has implemented periodic tasks which will be
        started. **/
    private Runnable actualPeriodicRunnable;

    @Override
    public void run() {
        System.out.println("Timer task started at:" + new Date());

        if (actualPeriodicRunnable != null) {
            actualPeriodicRunnable.run();
        }else{
            logger.warn("Periodic runnable is null!");
        }
        
        System.out.println("Timer task finished at:" + new Date());
    }

    public void setPeriodicRunnable(Runnable r) {
        actualPeriodicRunnable = r;
    }
}
