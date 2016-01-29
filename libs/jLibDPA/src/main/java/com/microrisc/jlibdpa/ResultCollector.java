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
package com.microrisc.jlibdpa;

import com.microrisc.jlibdpa.communication.receiving.DPAReceiver;
import com.microrisc.jlibdpa.configuration.DPAConfiguration;
import com.microrisc.jlibdpa.types.DPAResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Provides functions for collecting results of individual requests.
 *
 * @author Martin Strouhal
 */
public class ResultCollector extends DPAReceiver {

    private final Map<UUID, DPAResponse> responseMap;

    public ResultCollector(DPAConfiguration config) {
        int countOfResults = config.getCountOfResults();
        //TODO check
        responseMap = createCachedMap(countOfResults);
    }

    private <K, V> Map<K, V> createCachedMap(final int maxCapacity) {
        return new LinkedHashMap<K, V>(maxCapacity * 10 / 7, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxCapacity;
            }
        };
    }

    @Override
    public void onGetResponse(DPAResponse response) {
        //TODO log
        //TODO check maybe?
        responseMap.put(response.getUUID(), response);
    }

    public DPAResponse getResult(UUID uid) {
        DPAResponse response = responseMap.get(uid);
        if(response == null){
            //TODO error
            throw new RuntimeException("Result was removed!");
        }
        return response;
    }

    public void destroy() {
        //TODO check correct destroying
        Set<UUID> uuids = responseMap.keySet();
        for (UUID uid : uuids) {
            DPAResponse responseToDestroy = responseMap.get(uid);
            responseToDestroy = null;
            responseMap.remove(uid);
        }
    }
}
