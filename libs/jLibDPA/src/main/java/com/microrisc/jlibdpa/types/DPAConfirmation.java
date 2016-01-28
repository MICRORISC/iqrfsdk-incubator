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
package com.microrisc.jlibdpa.types;

/**
 * Encapsulates confirmation message from DPA network. Confirmation message is
 * received always if the request is sent to the whatever node except
 * coordinator and confirming successful reception by slave from master device.
 * If the request is incorrect, is returned only DPA response.
 *
 * @author Martin Strouhal
 */
public interface DPAConfirmation {

    /**
     * DPA value of the device. It's used for identification of confirmation
     * primary.
     *
     * @return DPA value of the device
     */
    short getDPAValue();

    /**
     * Number of hops used to deliver the DPA request to the addressed node. A
     * hop represents any sending of packet including sending from the sender as
     * well as from any routing node.
     *
     * @return number of hops for the request
     */
    short getHops();

    /**
     * Timeslot length used to deliver the DPA request to the addressed node.
     * Please note that the timeslot used to deliver the response message from
     * node to coordinator can have a different length.
     *
     * @return timeslost length in 10ms units
     */
    short getTimeslot();

    /**
     * Number of hops used to deliver the DPA response from the addressed node
     * back to coordinator. In case of broadcast this parameter is 0 as there is
     * no response sent back to coordinator.
     *
     * @return number of hops for the response
     */
    short getHopsResponse();
}
