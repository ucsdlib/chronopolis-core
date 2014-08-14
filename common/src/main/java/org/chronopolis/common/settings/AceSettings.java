package org.chronopolis.common.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 8/14/14.
 */
@Component
public class AceSettings {

    @Value("${ace.ims.host:ims.umiacs.umd.edu}")
    String imsHost;

    @Value("${ace.am.host:localhost}")
    String amHost;

    @Value("${ace.am.path:ace-am}")
    String amPath;

    @Value("${ace.am.port:8080}")
    Integer amPort;

    @Value("${ace.am.user:admin}")
    String amUser;

    @Value("${ace.am.password:admin}")
    String amPassword;


    public String getImsHost() {
        return imsHost;
    }

    public void setImsHost(final String imsHost) {
        this.imsHost = imsHost;
    }

    public String getAmHost() {
        return amHost;
    }

    public void setAmHost(final String amHost) {
        this.amHost = amHost;
    }

    public String getAmPath() {
        return amPath;
    }

    public void setAmPath(final String amPath) {
        this.amPath = amPath;
    }

    public Integer getAmPort() {
        return amPort;
    }

    public void setAmPort(final Integer amPort) {
        this.amPort = amPort;
    }

    public String getAmUser() {
        return amUser;
    }

    public void setAmUser(final String amUser) {
        this.amUser = amUser;
    }

    public String getAmPassword() {
        return amPassword;
    }

    public void setAmPassword(final String amPassword) {
        this.amPassword = amPassword;
    }
}
