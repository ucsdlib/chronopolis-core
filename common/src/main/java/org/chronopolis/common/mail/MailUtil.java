package org.chronopolis.common.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Created by shake on 4/29/14.
 */
public class MailUtil {

    public static void sendMail(String smtpHost, SimpleMailMessage message) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtpHost);
        sender.send(message);
    }

}
