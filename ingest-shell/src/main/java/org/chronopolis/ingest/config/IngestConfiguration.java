package org.chronopolis.ingest.config;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.restore.CollectionRestore;
import org.chronopolis.common.restore.LocalRestore;
import org.chronopolis.common.settings.AMQPSettings;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.common.RestoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    public DatabaseManager manager;

    @Autowired
    public RestoreRepository restoreRepository;

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
    public RabbitTemplate rabbitTemplate(AMQPSettings amqpSettings,
                                         CachingConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate();
        template.setExchange(amqpSettings.getExchange());
        template.setConnectionFactory(connectionFactory);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public CollectionRestore collectionRestore(IngestSettings settings) {
        return new LocalRestore(Paths.get(settings.getPreservation()),
                Paths.get(settings.getRestore()));
    }

}
