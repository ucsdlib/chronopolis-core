package org.chronopolis.common.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * Created by shake on 8/13/14.
 */
@Component
public class SMTPSettings {

    @Value("${smtp.from:localhost}")
    private String from;

    @Value("${smtp.to:chron-support-l@mailman.ucsd.edu}")
    private String to;

    @Value("${smtp.host:localhost.localdomain}")
    private String host;

    @Value("${smtp.send:true}")
    private Boolean send;

    public String getFrom() {
        return from;
    }

    public void setFrom(final String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(final String to) {
        this.to = to;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public Boolean getSend() {
        return send;
    }

    public void setSend(final Boolean send) {
        this.send = send;
    }
}
