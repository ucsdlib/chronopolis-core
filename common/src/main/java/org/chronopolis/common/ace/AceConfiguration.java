package org.chronopolis.common.ace;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for ACE properties
 *
 * Created by shake on 3/27/17.
 */
@ConfigurationProperties(prefix = "ace")
public class AceConfiguration {

    /**
     * IMS Configuration class
     */
    private Ims ims = new Ims();

    /**
     * The endpoint of the Audit Manager to connect to
     */
    private String am = "http://localhost:8080/ace-am/";

    /**
     * The username to connect to the Audit Manager with
     */
    private String username = "user";

    /**
     * The password to connect to the Audit Manager with
     */
    private String password = "change-me";

    /**
     * The audit period to use when creating collections in the Audit Manager
     */
    private Integer auditPeriod = 45;


    public Ims getIms() {
        return ims;
    }

    public AceConfiguration setIms(Ims ims) {
        this.ims = ims;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AceConfiguration setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AceConfiguration setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getAm() {
        return am;
    }

    public AceConfiguration setAm(String am) {
        this.am = am;
        return this;
    }

    public Integer getAuditPeriod() {
        return auditPeriod;
    }

    public AceConfiguration setAuditPeriod(Integer auditPeriod) {
        this.auditPeriod = auditPeriod;
        return this;
    }

    public static class Ims {
        /**
         * The port to connect to the IMS with
         */
        private int port = 80;

        /**
         * The max time to wait for token requests
         */
        private int waitTime = 5000;

        /**
         * The max queue length when making token requests
         */
        private int queueLength = 1000;

        /**
         * The token class to use with the IMS
         */
        private String tokenClass = "SHA-256";

        /**
         * The fqdn of the ims
         */
        private String endpoint   = "ims.umiacs.umd.edu";

        /**
         * Enable/Disable ssl when connecting to the IMS
         */
        private boolean ssl = false;

        public int getPort() {
            return port;
        }

        public Ims setPort(int port) {
            this.port = port;
            return this;
        }

        public int getWaitTime() {
            return waitTime;
        }

        public Ims setWaitTime(int waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public int getQueueLength() {
            return queueLength;
        }

        public Ims setQueueLength(int queueLength) {
            this.queueLength = queueLength;
            return this;
        }

        public String getTokenClass() {
            return tokenClass;
        }

        public Ims setTokenClass(String tokenClass) {
            this.tokenClass = tokenClass;
            return this;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public Ims setEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public boolean isSsl() {
            return ssl;
        }

        public Ims setSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }
    }
}
