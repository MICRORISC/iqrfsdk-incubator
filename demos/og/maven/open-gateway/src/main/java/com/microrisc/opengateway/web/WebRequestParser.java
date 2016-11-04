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
package com.microrisc.opengateway.web;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonValue;
import com.microrisc.opengateway.dpa.DPA_Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web requests parser.
 * 
 * @author Michal Konopa
 */
public final class WebRequestParser {
    
    /** Logger. */
    private static final Logger logger = LoggerFactory.getLogger(WebRequestParser.class);
    
    
    /**
     * Parses specified web request and returns its corresponding DPA request
     * representation.
     * 
     * @param webRequest web request to parse
     * @return DPA request corresponding to {@code webRequest}
     * @throws com.microrisc.opengateway.web.WebRequestParserException if some error
     *         occurs during parsing
     */
    public static DPA_Request parse(String webRequest) throws WebRequestParserException 
    {
        String n = null;
        String sv = null;
        int pid = 0;
        String dpa = null;
        int nadr = 0;
     
        JsonArray elements = Json.parse(webRequest).asObject().get("e").asArray();
        for (JsonValue element : elements) {
            n = element.asObject().getString("n", "");
            sv = element.asObject().getString("sv", "");
        }
      
        elements = Json.parse(webRequest).asObject().get("iqrf").asArray();
        for (JsonValue element : elements) {
            pid = element.asObject().getInt("pid", 0);
            dpa = element.asObject().getString("dpa", "");
            nadr = element.asObject().getInt("nadr", 0);
        }
        
        logger.info("n = {}, sv = {}, pid = {}, dpa = {}, nadr = {}", n, sv, pid, dpa, nadr);
        return new DPA_Request(n, sv, pid, dpa, nadr);
    }
}
