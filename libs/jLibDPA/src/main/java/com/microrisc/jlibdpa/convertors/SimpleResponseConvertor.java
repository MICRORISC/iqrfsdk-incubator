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
package com.microrisc.jlibdpa.convertors;

import com.microrisc.jlibdpa.types.DPAResponse;

/**
 *
 * @author Martin Strouhal
 */
public class SimpleResponseConvertor extends AbstractResponseConvertor {

    @Override
    public DPAResponse convert(short[] data) {
        //TODO documentation
        //TODO log
        //TODO implementation
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    private final static SimpleResponseConvertor instance = new SimpleResponseConvertor();
    
    private SimpleResponseConvertor(){}
    
    @ConvertorFactoryMethod
    public static SimpleResponseConvertor getInstance() {
        return instance;
    }
    
}
