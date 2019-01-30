package org.chronopolis.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * TODO: MailProperties for from w/ a default of chronmail
 * TODO: Constructor using SMTPSettings
 *
 * Created by shake on 4/29/14.
 */
public class MailUtil {
    private final Logger log = LoggerFactory.getLogger(MailUtil.class);

    private String smtpHost;
    private String smtpTo;
    private String smtpFrom;
    private boolean smtpSend;

    public MailUtil() {
        smtpHost = "localhost.localdomain";
        smtpFrom = "localhost";
        smtpTo = "shake@umiacs.umd.edu";
        smtpSend = true;
    }

    /**
     * get the smtp host used to send from
     *
     * @return smtpHost
     */
    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(final String smtpHost) {
        if (smtpHost != null) {
            this.smtpHost = smtpHost;
        }
    }

    /**
     * Get the to field in an smtp message
     *
     * @return smtpTo
     */
    public String getSmtpTo() {
        return smtpTo;
    }

    public void setSmtpTo(final String smtpTo) {
        if (smtpTo != null) {
            this.smtpTo = smtpTo;
        }
    }

    /**
     * Get the from field in an smtp message
     *
     * @return smtpFrom
     */
    public String getSmtpFrom() {
        return smtpFrom;
    }

    public void setSmtpFrom(final String smtpFrom) {
        if (smtpFrom != null) {
            this.smtpFrom = smtpFrom;
        }
    }

    /**
     * Create a {@link org.springframework.mail.SimpleMailMessage}, using the
     * nodeName and subject as the message's subject, and the body as the
     * message's text
     *
     * @param nodeName The node sending the message
     * @param subject The subject of the message
     * @param body The content of the message
     * @return the created message
     */
    public SimpleMailMessage createMessage(String nodeName, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(smtpTo);
        message.setFrom("chronmail");
        message.setSubject("[" + nodeName + "] " + subject);
        message.setText(body);
        return message;
    }

    /**
     * Send a {@link org.springframework.mail.SimpleMailMessage}
     *
     * @param message The message to send
     */
    public void send(final SimpleMailMessage message) {
        if (smtpSend) {
            log.info("Sending mail to {}", new Object[]{message.getTo()});
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(smtpHost);
            sender.send(message);
        }
    }

    public void setSmtpSend(Boolean smtpSend) {
        this.smtpSend = smtpSend;
    }
}
