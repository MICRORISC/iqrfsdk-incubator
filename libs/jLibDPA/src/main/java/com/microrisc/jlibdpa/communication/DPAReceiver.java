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

import com.microrisc.jlibdpa.types.DPAConfirmation;
import com.microrisc.jlibdpa.types.DPAResponse;

/**
 * Provides listening interface for receiving messages from IQRF network in DPA
 * format. Used methods must be overrided, in other case methods don't do
 * anything.
 *
 * @author Martin Strouhal
 */
public interface DPAReceiver {

    /**
     * This method is called on every receive async data from IQRF network.
     */
    public default void onGetAsyncMsg() {
    }

    /**
     * This method is called on every receive response data from IQRF network.
     * @param response which has been received
     */
    public default void onGetResponse(DPAResponse response) {
    }

    /**
     * This method is called on every receive confirmation data from IQRF
     * network.
     * @param confirmation which has been received
     */
    public default void onGetConfirmation(DPAConfirmation confirmation) {
    }
}
