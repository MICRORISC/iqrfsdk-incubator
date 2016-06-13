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
 * Holding MQTT params from JSON config file
 * 
 * @author Rostislav Spinar
 */
public class MQTTConfig {

    private String protocol;
    private String broker;
    private long port;
    private String clientId;
    private boolean cleanSession;
    private boolean quiteMode;
    private boolean ssl;
    private String certFilePath;
    private String username;
    private String password;

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the broker
     */
    public String getBroker() {
        return broker;
    }

    /**
     * @param broker the broker to set
     */
    public void setBroker(String broker) {
        this.broker = broker;
    }

    /**
     * @return the port
     */
    public long getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(long port) {
        this.port = port;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @param clientId the clientId to set
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the cleanSession
     */
    public boolean isCleanSession() {
        return cleanSession;
    }

    /**
     * @param cleanSession the cleanSession to set
     */
    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    /**
     * @return the quiteMode
     */
    public boolean isQuiteMode() {
        return quiteMode;
    }

    /**
     * @param quiteMode the quiteMode to set
     */
    public void setQuiteMode(boolean quiteMode) {
        this.quiteMode = quiteMode;
    }

    /**
     * @return the ssl
     */
    public boolean isSsl() {
        return ssl;
    }

    /**
     * @param ssl the ssl to set
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * @return the certFilePath
     */
    public String getCertFilePath() {
        return certFilePath;
    }

    /**
     * @param certFilePath the certFilePath to set
     */
    public void setCertFilePath(String certFilePath) {
        this.certFilePath = certFilePath;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
