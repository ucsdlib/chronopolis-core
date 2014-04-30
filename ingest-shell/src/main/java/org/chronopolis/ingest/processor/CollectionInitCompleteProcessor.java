/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.model.CollectionIngest;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;


/**
 *
 * @author shake
 */
public class CollectionInitCompleteProcessor implements ChronProcessor {
    private static final Logger log = LoggerFactory.getLogger(CollectionInitCompleteProcessor.class);

    private final IngestProperties properties;
    private ChronProducer producer;
    private MessageFactory messageFactory;
    private DatabaseManager manager;
    private MailSender mailSender;

    public CollectionInitCompleteProcessor(ChronProducer producer, IngestProperties properties, MessageFactory messageFactory, DatabaseManager manager) {
        this.producer = producer;
        this.properties = properties;
        this.messageFactory = messageFactory;
        this.manager = manager;
        this.mailSender = new JavaMailSenderImpl();
    }

    @Override
    public void process(ChronMessage chronMessage) {
        ChronMessage response = messageFactory.DefaultPackageIngestCompleteMessage();

        sendCompletionRecord((CollectionInitCompleteMessage) chronMessage);

        CollectionIngest ci = manager.getIngestDatabase().findByCorrelationId(chronMessage.getCorrelationId());
        Boolean toDpn = (ci.getToDpn() && properties.pushToDpn());
        if (toDpn) {
            // Send replication-init-query
            log.debug("Sending {} to dpn", chronMessage.getCorrelationId());

        }

        // Once again, hold the routing key temporarily
        producer.send(response, "package.intake.umiacs");
    }

    public void sendCompletionRecord(CollectionInitCompleteMessage message) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo("shake@umiacs.umd.edu");
        msg.setFrom("chron-ingest@localhost");
        msg.setSubject("Ingestion of " + message.getCorrelationId() + " complete");
        mailSender.send(msg);
    }
    
}
