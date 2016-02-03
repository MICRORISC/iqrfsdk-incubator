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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides functions for collecting results of individual requests.
 *
 * @author Martin Strouhal
 */
public class ResultCollector extends DPAReceiver {

    private final static Logger log = LoggerFactory.getLogger(ResultCollector.class);
    private final Map<UUID, DPAResponse> responseMap;

    public ResultCollector(DPAConfiguration config) {
        log.debug("ResultCollector - start: config={}", config);
        int countOfResults = config.getCountOfResults();
        //TODO check
        responseMap = createCachedMap(countOfResults);
        log.debug("ResultCollector - end");
    }

    private <K, V> Map<K, V> createCachedMap(final int maxCapacity) {
        log.debug("createCachedMap - start: maxCapacity={}", maxCapacity);
        Map<K, V> map = new LinkedHashMap<K, V>(maxCapacity * 10 / 7, 0.7f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxCapacity;
            }
        };
        log.debug("createCachedMap - end: {}", map);
        return map;
    }

    @Override
    public void onGetResponse(DPAResponse response) {
        log.debug("onGetResponse - start: response={}", response);
        //TODO check maybe?
        responseMap.put(response.getUUID(), response);
        log.debug("onGetResponse - end");
    }

    public DPAResponse getResult(UUID uid) {
        log.debug("getResult - start: uid={}", uid);
        DPAResponse response = responseMap.get(uid);
        if(response == null){
            //TODO error
            throw new RuntimeException("Result was removed!");
        }
        log.debug("getResult - end: {}", response);
        return response;
    }

    public void destroy() {
        log.debug("destroy - start");
        //TODO check correct destroying
        Set<UUID> uuids = responseMap.keySet();
        for (UUID uid : uuids) {
            DPAResponse responseToDestroy = responseMap.get(uid);
            responseToDestroy = null;
            responseMap.remove(uid);
        }
        log.debug("destroy - end");
    }
}
