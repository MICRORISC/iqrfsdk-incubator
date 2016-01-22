/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.microrisc.opengateway;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author spinarr
 */
public class OpenGateway implements Daemon {

    /** Logger.*/
    private static final Logger logger = LoggerFactory.getLogger(OpenGateway.class);
    
    private OpenGatewayRunner runner;
   
    @Override
    public void init(DaemonContext daemonContext) throws DaemonInitException, Exception {

        String[] args = daemonContext.getArguments();

        // creating runner instance
        runner = new OpenGatewayRunner();
    }

    @Override
    public void start() throws Exception {
        runner.createAndStartThreads();
    }

    @Override
    public void stop() throws Exception {
        runner.terminateThread();
    }
   
    @Override
    public void destroy() {
        runner = null;
    }
}
