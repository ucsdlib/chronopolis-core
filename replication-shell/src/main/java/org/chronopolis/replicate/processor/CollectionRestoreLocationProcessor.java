package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.db.common.model.RestoreRequest;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreLocationMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shake on 8/7/14.
 */
public class CollectionRestoreLocationProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionRestoreLocationProcessor.class);

    private final ReplicationSettings settings;
    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final RestoreRepository restoreRepository;
    private final MailUtil mailUtil;

    public CollectionRestoreLocationProcessor(final ReplicationSettings settings,
                                              final ChronProducer producer,
                                              final MessageFactory messageFactory,
                                              final RestoreRepository restoreRepository, final MailUtil mailUtil) {
        this.settings = settings;
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.restoreRepository = restoreRepository;
        this.mailUtil = mailUtil;
    }

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreLocationMessage)) {
            throw new RuntimeException("Invalid message received for "
                    + this.getClass().getName());
        }

        CollectionRestoreLocationMessage msg =
                (CollectionRestoreLocationMessage) chronMessage;

        // If we were chosen, restore the collection
        if (Indicator.ACK.name().equalsIgnoreCase(msg.getMessageAtt())) {
            putAndNotify(msg);
        }

        // This is the end of the flow for repl nodes, so remove the DB object
        removeRequest(msg);
    }

    private void putAndNotify(CollectionRestoreLocationMessage msg) {
        String protocol = msg.getProtocol();
        String location = msg.getRestoreLocation();
        String correlationId = msg.getCorrelationId();
        FileTransfer transfer = null;
        if (protocol.equals("rsync")) {
            transfer = new RSyncTransfer("chrono");
        } else {
            transfer = new HttpsTransfer();
        }

        log.info("Finding restore request...");
        RestoreRequest restore =
                restoreRepository.findByCorrelationId(correlationId);
        // This implies that ACE and the Replication Shell share the same
        // pathname for the chronopolis preservation area - this may not
        // always be the case so...
        // TODO: Relativize the restore path against our preservation mount
        Path local = Paths.get(restore.getDirectory());
        Indicator att = Indicator.ACK;

        try {
            log.info("putting collection...");
            transfer.put(local, location);

            // Send mail for our success
            mailUtil.send(mailUtil.createMessage(
                    settings.getNode(),
                    "Successful Restoration",
                    "Successfully restored " + restore.getCollectionName()
            ));
        } catch (FileTransferException e) {
            log.error("Error restoring collection", e);
            att = Indicator.NAK;

            // Send mail for our failure
            mailUtil.send(mailUtil.createMessage(
                    settings.getNode(),
                    "Restoration Failed",
                    "Could not restore" + restore.getCollectionName()
                    + "\n" + e.getMessage()
            ));
        }

        log.info("Sending response...");
        // TODO: What location to actually send back?
        ChronMessage reply = messageFactory.collectionRestoreCompleteMessage(
                att,
                location,
                correlationId
        );

        producer.send(reply, msg.getReturnKey());
    }

    private void removeRequest(CollectionRestoreLocationMessage msg) {
        String correlationId = msg.getCorrelationId();
        // TODO: repo.deleteByCorrelationId
        RestoreRequest restore =
                restoreRepository.findByCorrelationId(correlationId);
        if (restore != null) {
            restoreRepository.delete(restore);
        }
    }

}
