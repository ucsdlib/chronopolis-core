package org.chronopolis.common.settings;

import org.chronopolis.common.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * Created by shake on 8/14/14.
 */
@Component
@Deprecated
public class AceSettings {
    private final Logger log = LoggerFactory.getLogger(AceSettings.class);

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

    @Value("${ace.am.validate:false}")
    Boolean amValidate;


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

    @PostConstruct
    @SuppressWarnings("UnusedDeclaration")
    public void checkConnection() {
        StringBuilder sb = URIUtil.buildAceUri(amHost,
                amPort,
                amPath);
        if (amValidate) {
            try {
                URL url = new URL(sb.toString());
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                if (connection.getResponseCode() != 200) {
                    log.error("Could not connect to ACE instance, check your "
                            + "properties against your tomcat deployment");
                    throw new BeanCreationException("Could not connect to ACE");
                }
            } catch (IOException e) {
                log.error("Could not create URL connection to "
                        + amHost
                        + ". Ensure your tomcat server is running.");
                throw new BeanCreationException("Could not connect to ACE");
            }
        }
    }


    public Boolean getAmValidate() {
        return amValidate;
    }

    public void setAmValidate(final Boolean amValidate) {
        this.amValidate = amValidate;
    }
}
