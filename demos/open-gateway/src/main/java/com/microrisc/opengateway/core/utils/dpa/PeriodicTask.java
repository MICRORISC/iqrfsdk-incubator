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

package com.microrisc.opengateway.core.utils.dpa;

import java.util.Date;
import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rostislav Spinar
 */
public class PeriodicTask extends TimerTask {

    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(PeriodicTask.class);

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
