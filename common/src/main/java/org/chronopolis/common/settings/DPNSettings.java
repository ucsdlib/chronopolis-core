package org.chronopolis.common.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 9/30/14.
 */
@Component
public class DPNSettings {

    @Value("${dpn.web.host:localhost}")
    private String dpnWebHost;

    @Value("${dpn.web.port:8080}")
    private Integer dpnWebPort;

    @Value("${dpn.web.path:}")
    private String dpnWebPath;

    @Value("${dpn.web.user:user}")
    private String dpnUser;

    @Value("${dpn.web.password:password}")
    private String dpnPassword;

    public String getDpnWebHost() {
        return dpnWebHost;
    }

    public void setDpnWebHost(final String dpnWebHost) {
        this.dpnWebHost = dpnWebHost;
    }

    public Integer getDpnWebPort() {
        return dpnWebPort;
    }

    public void setDpnWebPort(final Integer dpnWebPort) {
        this.dpnWebPort = dpnWebPort;
    }

    public String getDpnWebPath() {
        return dpnWebPath;
    }

    public void setDpnWebPath(final String dpnWebPath) {
        this.dpnWebPath = dpnWebPath;
    }

    public String getDpnUser() {
        return dpnUser;
    }

    public void setDpnUser(final String dpnUser) {
        this.dpnUser = dpnUser;
    }

    public String getDpnPassword() {
        return dpnPassword;
    }

    public void setDpnPassword(final String dpnPassword) {
        this.dpnPassword = dpnPassword;
    }
}
