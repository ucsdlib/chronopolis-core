package org.chronopolis.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Created by shake on 4/29/14.
 */
public class MailUtil {
    private static final Logger log = LoggerFactory.getLogger(MailUtil.class);

    private String smtpHost;
    private String smtpTo;
    private String smtpFrom;

    public MailUtil() {
        smtpHost = "localhost.localdomain";
        smtpFrom = "localhost";
        smtpTo = "shake@umiacs.umd.edu";
    }


    public static void sendMail(String smtpHost, SimpleMailMessage message) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpHost);
        sender.send(message);
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(final String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getSmtpTo() {
        return smtpTo;
    }

    public void setSmtpTo(final String smtpTo) {
        this.smtpTo = smtpTo;
    }

    public String getSmtpFrom() {
        return smtpFrom;
    }

    public void setSmtpFrom(final String smtpFrom) {
        this.smtpFrom = smtpFrom;
    }

    public void send(final SimpleMailMessage message) {
        log.info("Sending mail to {}", message.getTo());
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpHost);
        sender.send(message);
    }
}
