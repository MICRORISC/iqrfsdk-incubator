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
 * Holds MQTT configuration parameters.
 * 
 * @author Rostislav Spinar
 */
public final class MqttConfiguration {

    private final String protocol;
    private final String broker;
    private final long port;
    private final String clientId;
    private final String gwId;
    private final boolean cleanSession;
    private final boolean quiteMode;
    private final boolean ssl;
    private final String certFilePath;
    private final String username;
    private final String password;
    private final String rootTopic;    
    
    /**
     * Creates new object holding information about MQTT configuration
     * parameters.
     */
    public MqttConfiguration(
            String protocol, String broker, long port, String clientId,
            String gwId, boolean cleanSession, boolean quiteMode, boolean ssl,
            String certFilePath, String username, String password, String rootTopic
    ) {
        this.protocol = protocol;
        this.broker = broker;
        this.port = port;
        this.clientId = clientId;
        this.gwId = gwId;
        this.cleanSession = cleanSession;
        this.quiteMode = quiteMode;
        this.ssl = ssl;
        this.certFilePath = certFilePath;
        this.username = username;
        this.password = password;
        this.rootTopic = rootTopic;
    }
    
    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @return the broker
     */
    public String getBroker() {
        return broker;
    }

    /**
     * @return the port
     */
    public long getPort() {
        return port;
    }

    /**
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return the indication of clean session
     */
    public boolean isCleanSession() {
        return cleanSession;
    }

    /**
     * @return the indication of quite mode
     */
    public boolean isQuiteMode() {
        return quiteMode;
    }

    /**
     * @return the ssl
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * @return the path to certification file
     */
    public String getCertFilePath() {
        return certFilePath;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the gwId
     */
    public String getGwId() {
        return gwId;
    }
    
    /**
     * @return the rootTopicPath
     */
    public String getRootTopic() {
        return rootTopic;
    }
}
