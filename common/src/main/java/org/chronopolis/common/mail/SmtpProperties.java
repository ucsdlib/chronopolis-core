package org.chronopolis.common.mail;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author shake
 */
@ConfigurationProperties(prefix = "smtp")
public class SmtpProperties {

    private Boolean send = true;
    private String to = "chron-support-l@mailman.ucsd.edu";
    private String from = "localhost";
    private String host = "localhost.localdomain";

    public Boolean getSend() {
        return send;
    }

    public SmtpProperties setSend(Boolean send) {
        this.send = send;
        return this;
    }

    public String getTo() {
        return to;
    }

    public SmtpProperties setTo(String to) {
        this.to = to;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public SmtpProperties setFrom(String from) {
        this.from = from;
        return this;
    }

    public String getHost() {
        return host;
    }

    public SmtpProperties setHost(String host) {
        this.host = host;
        return this;
    }
}
