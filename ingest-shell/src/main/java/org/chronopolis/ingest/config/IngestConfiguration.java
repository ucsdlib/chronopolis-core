package org.chronopolis.ingest.config;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.restore.CollectionRestore;
import org.chronopolis.common.restore.LocalRestore;
import org.chronopolis.common.settings.SMTPSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.nio.file.Paths;

/**
 * ingest configuration
 * Created by shake on 4/10/14.
 */
@Configuration
@Import(IngestJPAConfiguration.class)
public class IngestConfiguration {

    private final Logger log = LoggerFactory.getLogger(IngestConfiguration.class);

    @Bean
    public MailUtil mailUtil(SMTPSettings smtpSettings) {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(smtpSettings.getFrom());
        mailUtil.setSmtpTo(smtpSettings.getTo());
        mailUtil.setSmtpHost(smtpSettings.getHost());
        mailUtil.setSmtpSend(smtpSettings.getSend());
        return mailUtil;
    }

    @Bean
    public CollectionRestore collectionRestore(IngestSettings settings) {
        return new LocalRestore(Paths.get(settings.getPreservation()),
                Paths.get(settings.getRestore()));
    }

}
