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
package com.microrisc.jlibdpa.timing;

import com.microrisc.jlibdpa.communication.receiving.DPAReceiver;
import com.microrisc.jlibdpa.configuration.DPAConfiguration;
import com.microrisc.jlibdpa.types.DPAConfirmation;

/**
 *
 * @author Martin Strouhal
 */
public class NoTimingManager extends DPAReceiver implements TimeManager {

    public void init(DPAConfiguration config) {
        //TODO init
    }

    public boolean isFree() {
        //TODO implement time checking
        return true;
    }

    /** Reaming time in milliseconds. */
    public int getRemaingTime() {
        return 0;
    }

    public void startWithUnknownTime() {
        //TODO
    }

    private void computeAndAddNewTime(DPAConfirmation confirmation) {
        //TODO implement adding time
    }

    @Override
    public void onGetConfirmation(DPAConfirmation confirmation) {
        computeAndAddNewTime(confirmation);
    }
}