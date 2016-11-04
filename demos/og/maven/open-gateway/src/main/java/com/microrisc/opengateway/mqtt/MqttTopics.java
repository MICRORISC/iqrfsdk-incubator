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

package com.microrisc.opengateway.mqtt;

/**
 * MQTT topics.
 * 
 * @author Rostislav Spinar
 * @author Michal Konopa
 */
public final class MqttTopics {
    
    private static final String DEFAULT_GW_ID = "b827eb26c73d";
    
    public static final String DEFAULT_STD_SENSORS_PROTRONIX = "/std/sensors/protronix";
    public static final String DEFAULT_STD_SENSORS_PROTRONIX_DPA_REQUESTS = "/std/sensors/protronix/dpa/requests";
    public static final String DEFAULT_STD_SENSORS_PROTRONIX_DPA_CONFIRMATIONS = "/std/sensors/protronix/dpa/confirmations";
    public static final String DEFAULT_STD_SENSORS_PROTRONIX_DPA_RESPONSES = "/std/sensors/protronix/dpa/responses";
    
    public static final String DEFAULT_STD_SENSORS_AUSTYN = "/std/sensors/austyn";
    public static final String DEFAULT_STD_SENSORS_AUSTYN_DPA_REQUESTS = "/std/sensors/austyn/dpa/requests";
    public static final String DEFAULT_STD_SENSORS_AUSTYN_DPA_CONFIRMATIONS = "/std/sensors/austyn/dpa/confirmations";
    public static final String DEFAULT_STD_SENSORS_AUSTYN_DPA_RESPONSES = "/std/sensors/austyn/dpa/responses";
    
    public static final String DEFAULT_STD_SENSORS_IQHOME = "/std/sensors/iqhome";
    public static final String DEFAULT_LP_SENSORS_IQHOME = "/lp/sensors/iqhome";
    
    public static final String DEFAULT_STD_ACTUATORS_AUSTYN = "/std/actuators/austyn";
    public static final String DEFAULT_STD_ACTUATORS_DEVTECH = "/std/actuators/devtech";
    public static final String DEFAULT_STD_ACTUATORS_DATMOLUX = "/std/actuators/datmolux";
    public static final String DEFAULT_STD_ACTUATORS_TECO = "/std/actuators/teco";
    public static final String DEFAULT_LP_ACTUATORS_TECO = "/lp/actuators/teco";
    
    public static final String DEFAULT_STD_STATUS_DEVTECH = "/std/status/devtech";
    public static final String DEFAULT_STD_STATUS_DATMOLUX = "/std/status/datmolux";
    
    public static final String DEFAULT_ERRORS = "/errors/";
    
    
    private final String gwId;
    
    private final String stdSensorsProtronix;
    private final String stdSensorsProtronixErrors;

    private final String stdSensorsAustyn;
    private final String stdSensorsAustynErrors;
    
    private final String stdActuatorsAustyn;
    private final String stdActuatorsAustynErrors;
    
    private final String stdActuatorsDevtech;
    private final String stdActuatorsDevtechErrors;
    
    private final String stdActuatorsDatmolux;
    private final String stdActuatorsDatmoluxErrors;
    
    private final String stdActuatorsTeco;
    private final String stdActuatorsTecoErrors;
    
    private final String stdSensorsIqHome;
    private final String stdSensorsIqhomeErrors;
    
    private final String lpSensorsIqHome;
    private final String lpSensorsIqhomeErrors;
    
    private final String lpActuatorsTeco;
    private final String lpActuatorsTecoErrors;
    
    private final String stdStatusDevtech;
    private final String stdStatusDatmolux;
    
    
    /**
     * Builder class.
     */
    public static class Builder {
        private String gwId;
        
        private String stdSensorsProtronix;
        private String stdSensorsProtronixErrors;

        private String stdSensorsAustyn;
        private String stdSensorsAustynErrors;
        
        private String stdActuatorsAustyn;
        private String stdActuatorsAustynErrors;
        
        private String stdActuatorsDevtech;
        private String stdActuatorsDevtechErrors;
        
        private String stdActuatorsDatmolux;
        private String stdActuatorsDatmoluxErrors;
    
        private String stdActuatorsTeco;
        private String stdActuatorsTecoErrors;
        
        private String stdSensorsIqHome;
        private String stdSensorsIqhomeErrors;
        
        private String lpSensorsIqHome;
        private String lpSensorsIqhomeErrors;

        private String lpActuatorsTeco;
        private String lpActuatorsTecoErrors;
        
        private String stdStatusDevtech;
        private String stdStatusDatmolux;
        
        
        public Builder gwId(String gwId) { 
            this.gwId = gwId;
            return this;
        }
        
        public Builder stdSensorsProtronix(String stdSensorsProtronix) { 
            this.stdSensorsProtronix = stdSensorsProtronix;
            return this;
        }
        
        public Builder stdSensorsProtronixErrors(String stdSensorsProtronixErrors) { 
            this.stdSensorsProtronixErrors = stdSensorsProtronixErrors;
            return this;
        }

        public Builder stdSensorsAustyn(String stdSensorsAustyn) {
            this.stdSensorsAustyn = stdSensorsAustyn;
            return this;
        }

        public Builder stdSensorsAustynErrors(String stdSensorsAustynErrors) {
            this.stdSensorsAustynErrors = stdSensorsAustynErrors;
            return this;
        }
        
        public Builder stdActuatorsAustyn(String stdActuatorsAustyn) { 
            this.stdActuatorsAustyn = stdActuatorsAustyn;
            return this;
        }
        
        public Builder stdActuatorsAustynErrors(String stdActuatorsAustynErrors) { 
            this.stdActuatorsAustynErrors = stdActuatorsAustynErrors;
            return this;
        }
        
        public Builder stdActuatorsDevtech(String stdActuatorsDevtech) { 
            this.stdActuatorsDevtech = stdActuatorsDevtech;
            return this;
        }
        
        public Builder stdActuatorsDevtechErrors(String stdActuatorsDevtechErrors) { 
            this.stdActuatorsDevtechErrors = stdActuatorsDevtechErrors;
            return this;
        }
        
        public Builder stdActuatorsDatmolux(String stdActuatorsDatmolux) { 
            this.stdActuatorsDatmolux = stdActuatorsDatmolux;
            return this;
        }
        
        public Builder stdActuatorsDatmoluxErrors(String stdActuatorsDatmoluxErrors) { 
            this.stdActuatorsDatmoluxErrors = stdActuatorsDatmoluxErrors;
            return this;
        }
        
        public Builder stdActuatorsTeco(String stdActuatorsTeco) { 
            this.stdActuatorsTeco = stdActuatorsTeco;
            return this;
        }
        
        public Builder stdActuatorsTecoErrors(String stdActuatorsTecoErrors) { 
            this.stdActuatorsTecoErrors = stdActuatorsTecoErrors;
            return this;
        }
        
        public Builder lpSensorsIqHome(String lpSensorsIqHome) { 
            this.lpSensorsIqHome = lpSensorsIqHome;
            return this;
        }
        
        public Builder lpSensorsIqHomeErrors(String lpSensorsIqhomeErrors) { 
            this.lpSensorsIqhomeErrors = lpSensorsIqhomeErrors;
            return this;
        }
        
        public Builder stdSensorsIqHome(String stdSensorsIqHome) { 
            this.stdSensorsIqHome = stdSensorsIqHome;
            return this;
        }
        
        public Builder stdSensorsIqHomeErrors(String stdSensorsIqhomeErrors) { 
            this.stdSensorsIqhomeErrors = stdSensorsIqhomeErrors;
            return this;
        }
        
        public Builder lpActuatorsTeco(String lpActuatorsTeco) { 
            this.lpActuatorsTeco = lpActuatorsTeco;
            return this;
        }
        
        public Builder lpActuatorsTecoErrors(String lpActuatorsTecoErrors) { 
            this.lpActuatorsTecoErrors = lpActuatorsTecoErrors;
            return this;
        }
        
        public Builder stdStatusDevtech(String stdStatusDevtech) { 
            this.stdStatusDevtech = stdStatusDevtech;
            return this;
        }
        
        public Builder stdStatusDatmolux(String stdStatusDatmolux) { 
            this.stdStatusDatmolux = stdStatusDatmolux;
            return this;
        }
        
        public MqttTopics build() {
            return new MqttTopics(this);
        }
    }
    
    
    /**
     * Creates new object of MQTT topics according to specified builder object.
     */
    private MqttTopics(Builder builder) {
        this.gwId = (builder.gwId != null)? builder.gwId : DEFAULT_GW_ID;
        
        this.stdSensorsProtronix = (builder.stdSensorsProtronix != null)? 
                this.gwId + builder.stdSensorsProtronix : this.gwId + DEFAULT_STD_SENSORS_PROTRONIX; 
        this.stdSensorsProtronixErrors = (builder.stdSensorsProtronixErrors != null)
                ? this.gwId + builder.stdSensorsProtronixErrors : this.stdSensorsProtronix + DEFAULT_ERRORS;

        this.stdSensorsAustyn = (builder.stdSensorsAustyn != null)
                ? this.gwId + builder.stdSensorsAustyn : this.gwId + DEFAULT_STD_SENSORS_AUSTYN;
        this.stdSensorsAustynErrors = (builder.stdSensorsAustynErrors != null)
                ? this.gwId + builder.stdSensorsAustynErrors : this.stdSensorsAustyn + DEFAULT_ERRORS;
        
        this.stdActuatorsAustyn = (builder.stdActuatorsAustyn != null)? 
                this.gwId + builder.stdActuatorsAustyn : this.gwId + DEFAULT_STD_ACTUATORS_AUSTYN;
        this.stdActuatorsAustynErrors = (builder.stdActuatorsAustynErrors != null)? 
                this.gwId + builder.stdActuatorsAustynErrors : this.stdActuatorsAustyn + DEFAULT_ERRORS;
        
        this.stdActuatorsDevtech = (builder.stdActuatorsDevtech != null)? 
                this.gwId + builder.stdActuatorsDevtech : this.gwId + DEFAULT_STD_ACTUATORS_DEVTECH;
        this.stdActuatorsDevtechErrors = (builder.stdActuatorsDevtechErrors != null)? 
                this.gwId + builder.stdActuatorsDevtechErrors : this.stdActuatorsDevtech + DEFAULT_ERRORS;
        
        this.stdActuatorsDatmolux = (builder.stdActuatorsDatmolux != null)? 
                this.gwId + builder.stdActuatorsDatmolux : this.gwId + DEFAULT_STD_ACTUATORS_DATMOLUX;
        this.stdActuatorsDatmoluxErrors = (builder.stdActuatorsDatmoluxErrors != null)? 
                this.gwId + builder.stdActuatorsDatmoluxErrors : this.stdActuatorsDatmolux + DEFAULT_ERRORS;
        
        this.stdActuatorsTeco = (builder.stdActuatorsTeco != null)? 
                this.gwId + builder.stdActuatorsTeco : this.gwId + DEFAULT_STD_ACTUATORS_TECO;
        this.stdActuatorsTecoErrors = (builder.stdActuatorsTecoErrors != null)? 
                this.gwId + builder.stdActuatorsTecoErrors : this.stdActuatorsTeco + DEFAULT_ERRORS;
        
        this.stdSensorsIqHome = (builder.stdSensorsIqHome != null)? 
                this.gwId + builder.stdSensorsIqHome : this.gwId + DEFAULT_STD_SENSORS_IQHOME;
        this.stdSensorsIqhomeErrors = (builder.stdSensorsIqhomeErrors != null)? 
                this.gwId + builder.stdSensorsIqhomeErrors : this.stdSensorsIqHome + DEFAULT_ERRORS;
        
        this.lpSensorsIqHome = (builder.lpSensorsIqHome != null)? 
                this.gwId + builder.lpSensorsIqHome : this.gwId + DEFAULT_LP_SENSORS_IQHOME;
        this.lpSensorsIqhomeErrors = (builder.lpSensorsIqhomeErrors != null)? 
                this.gwId + builder.lpSensorsIqhomeErrors : this.lpSensorsIqHome + DEFAULT_ERRORS;
        
        this.lpActuatorsTeco = (builder.lpActuatorsTeco != null)? 
                this.gwId + builder.lpActuatorsTeco : this.gwId + DEFAULT_LP_ACTUATORS_TECO;
        this.lpActuatorsTecoErrors = (builder.lpActuatorsTecoErrors != null)? 
                this.gwId + builder.lpActuatorsTecoErrors : this.lpActuatorsTeco + DEFAULT_ERRORS;
        
        this.stdStatusDevtech = (builder.stdStatusDevtech != null)? 
                this.gwId + builder.stdStatusDevtech : this.gwId + DEFAULT_STD_STATUS_DEVTECH;
        
        this.stdStatusDatmolux = (builder.stdStatusDatmolux != null)? 
                this.gwId + builder.stdStatusDatmolux : this.gwId + DEFAULT_STD_STATUS_DATMOLUX;
    }
    
    /**
     * @return the client ID
     */
    public String getGwId() {
        return gwId;
    }

    /**
     * @return the full std sensors Protronix path
     */
    public String getStdSensorsProtronix() {
        return stdSensorsProtronix;
    }

    /**
     * @return the full std sensors Protronix path
     */
    public String getStdSensorsProtronixErrors() {
        return stdSensorsProtronixErrors;
    }
    
    /**
     * @return the full std sensors Austyn path
     */
    public String getStdSensorsAustyn() {
        return stdSensorsAustyn;
    }

    /**
     * @return the full std sensors Protronix path
     */
    public String getStdSensorsAustynErrors() {
        return stdSensorsAustynErrors;
    }
    
    /**
     * @return the full std actuator Austyn path
     */
    public String getStdActuatorsAustyn() {
        return stdActuatorsAustyn;
    }

    /**
     * @return the full std actuator Austyn errors path
     */
    public String getStdActuatorsAustynErrors() {
        return stdActuatorsAustynErrors;
    }
    
    /**
     * @return the full std actuator Devtech path
     */
    public String getStdActuatorsDevtech() {
        return stdActuatorsDevtech;
    }

    /**
     * @return the full std actuator Devtech errors path
     */
    public String getStdActuatorsDevtechErrors() {
        return stdActuatorsDevtechErrors;
    }

    /**
     * @return the full std actuator Datmolux path
     */
    public String getStdActuatorsDatmolux() {
        return stdActuatorsDatmolux;
    }

    /**
     * @return the full std actuator Datmolux errors path
     */
    public String getStdActuatorsDatmoluxErrors() {
        return stdActuatorsDatmoluxErrors;
    }
    
    /**
     * @return the full std actuator Teco path
     */
    public String getStdActuatorsTeco() {
        return stdActuatorsTeco;
    }

    /**
     * @return the full std actuator Teco errors path
     */
    public String getStdActuatorsTecoErrors() {
        return stdActuatorsTecoErrors;
    }
    
    /**
     * @return the full std sensor Iqhome path
     */
    public String getStdSensorsIqHome() {
        return stdSensorsIqHome;
    }

    /**
     * @return the full std sensor Iqhome errors path
     */
    public String getStdSensorsIqHomeErrors() {
        return stdSensorsIqhomeErrors;
    }
    
    /**
     * @return the full lp sensor Iqhome path
     */
    public String getLpSensorsIqHome() {
        return lpSensorsIqHome;
    }

    /**
     * @return the full lp sensor Iqhome errors path
     */
    public String getLpSensorsIqHomeErrors() {
        return lpSensorsIqhomeErrors;
    }

    /**
     * @return the full lp actuators Teco path
     */
    public String getLpActuatorsTeco() {
        return lpActuatorsTeco;
    }

    /**
     * @return the full std actuator Teco errors path
     */
    public String getLpActuatorsTecoErrors() {
        return lpActuatorsTecoErrors;
    }
    
    /**
     * @return the full std devtech 
     */
    public String getStdStatusDevtech() {
        return stdStatusDevtech;
    }
    
    /**
     * @return the full std datmolux 
     */
    public String getStdStatusDatmolux() {
        return stdStatusDatmolux;
    }
}
