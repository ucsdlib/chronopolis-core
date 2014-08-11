package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.settings.ChronopolisSettings;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shake on 8/7/14.
 */
public class CollectionRestoreLocationProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionRestoreLocationProcessor.class);

    private final ChronopolisSettings settings;
    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final RestoreRepository restoreRepository;

    public CollectionRestoreLocationProcessor(final ChronopolisSettings settings,
                                              final ChronProducer producer,
                                              final MessageFactory messageFactory,
                                              final RestoreRepository restoreRepository) {
        this.settings = settings;
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.restoreRepository = restoreRepository;
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
        if (Indicator.ACK.name().equals(msg.getMessageAtt())) {
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

        RestoreRequest restore =
                restoreRepository.findByCorrelationId(correlationId);
        // This implies that ACE and the Replication Shell share the same
        // pathname for the chronopolis preservation area - this may not
        // always be the case so...
        // TODO: Relativize the restore path against our preservation mount
        Path local = Paths.get(restore.getDirectory());
        Indicator att = Indicator.ACK;

        try {
            transfer.put(local, location);
        } catch (FileTransferException e) {
            log.error("Error restoring collection", e);
            att = Indicator.NAK;
        }

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
        restoreRepository.delete(restore);
    }

}
