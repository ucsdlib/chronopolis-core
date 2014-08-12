package org.chronopolis.replicate.config;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 8/12/14.
 */
@Component
public class ReplicationSettings extends ChronopolisSettings {

    @Value("${ace.fqdn:localhost}")
    private String aceFQDN;

    @Value("${ace.path:ace-am}")
    private String acePath;

    @Value("${ace.user:admin}")
    private String aceUser;

    @Value("${ace.password:admin}")
    private String acePassword;

    @Value("${ace.port:8080}")
    private Integer acePort;

    public String getAceFQDN() {
        return aceFQDN;
    }

    public void setAceFQDN(final String aceFQDN) {
        this.aceFQDN = aceFQDN;
    }

    public String getAcePath() {
        return acePath;
    }

    public void setAcePath(final String acePath) {
        this.acePath = acePath;
    }

    public String getAceUser() {
        return aceUser;
    }

    public void setAceUser(final String aceUser) {
        this.aceUser = aceUser;
    }

    public String getAcePassword() {
        return acePassword;
    }

    public void setAcePassword(final String acePassword) {
        this.acePassword = acePassword;
    }

    public Integer getAcePort() {
        return acePort;
    }

    public void setAcePort(final Integer acePort) {
        this.acePort = acePort;
    }
}
