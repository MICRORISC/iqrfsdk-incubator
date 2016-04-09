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

package com.microrisc.opengateway.core.internal;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonContext;
import org.apache.commons.daemon.DaemonInitException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rostislav Spinar
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
