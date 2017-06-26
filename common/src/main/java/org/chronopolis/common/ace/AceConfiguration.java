package org.chronopolis.common.ace;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 *
 * Created by shake on 3/27/17.
 */
@ConfigurationProperties(prefix = "ace")
public class AceConfiguration {

    private String ims = "ims.umiacs.umd.edu";
    private String am = "http://localhost:8080/ace-am/";
    private String username = "user";
    private String password = "change-me";


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
}
