package org.chronopolis.common.ace;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by shake on 3/27/17.
 */
@ConfigurationProperties(prefix = "ace")
public class AceConfiguration {

    /**
     * The fqdn of the IMS to connect to
     */
    private String ims = "ims.umiacs.umd.edu";

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


    public String getIms() {
        return ims;
    }

    public AceConfiguration setIms(String ims) {
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
}
