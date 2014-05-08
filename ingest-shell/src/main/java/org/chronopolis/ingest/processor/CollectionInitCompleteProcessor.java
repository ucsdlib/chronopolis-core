/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.ingest.IngestDB;
import org.chronopolis.db.model.CollectionIngest;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.exception.InvalidMessageException;
import org.chronopolis.messaging.factory.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;


/**
 *
 * @author shake
 */
public class CollectionInitCompleteProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionInitCompleteProcessor.class);

    private final IngestProperties properties;
    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final DatabaseManager manager;
    private final MailUtil mailUtil;

    public CollectionInitCompleteProcessor(ChronProducer producer,
                                           IngestProperties properties,
                                           MessageFactory messageFactory,
                                           DatabaseManager manager,
                                           MailUtil mailUtil) {
        this.producer = producer;
        this.properties = properties;
        this.messageFactory = messageFactory;
        this.manager = manager;
        this.mailUtil = mailUtil;
    }

    @Override
    public void process(ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionInitCompleteMessage)) {
            // Error out
            log.error("Invalid message type");
            throw new InvalidMessageException("Expected message of type CollectionInitComplete"
                    + " but received " + chronMessage.getClass().getName());
        }
        ChronMessage response = messageFactory.DefaultPackageIngestCompleteMessage();

        sendCompletionRecord((CollectionInitCompleteMessage) chronMessage);

        IngestDB db = manager.getIngestDatabase();
        CollectionIngest ci = db.findByCorrelationId(chronMessage.getCorrelationId());
        log.info("Retrieved item correlation {} and toDpn value of {}",
                ci.getCorrelationId(), ci.getToDpn());
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
        msg.setTo(mailUtil.getSmtpTo());
        msg.setFrom(properties.getNodeName() + "-ingest@" + mailUtil.getSmtpFrom());
        msg.setSubject("Ingestion of " + message.getCorrelationId() + " complete");
    }

}
