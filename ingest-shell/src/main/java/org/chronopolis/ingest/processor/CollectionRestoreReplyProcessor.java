package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreReplyMessage;
import org.chronopolis.messaging.factory.MessageFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shake on 8/7/14.
 */
public class CollectionRestoreReplyProcessor implements ChronProcessor {

    private ChronopolisSettings settings;
    private ChronProducer producer;
    private MessageFactory messageFactory;

    // TODO: Replace with ingest settings
    private IngestProperties ingestProperties;

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreReplyMessage)) {
            throw new RuntimeException("Invalid message for "
                    + this.getClass().getName()
                    + ": "
                    + chronMessage.getClass().getName());
        }

        CollectionRestoreReplyMessage msg =
                (CollectionRestoreReplyMessage) chronMessage;

        Path base = Paths.get(ingestProperties.getStage());
        ChronMessage reply = null;
        // For now, we'll just pull from ourselves
        // In the future we'll want a way to actualyl choose
        if (msg.getOrigin().equals(settings.getNode())) {
            reply = messageFactory.collectionRestoreLocationMessage("rsync",
                    "location",
                    Indicator.ACK,
                    chronMessage.getCorrelationId());
        } else {
            reply = messageFactory.collectionRestoreLocationMessage(null,
                    null,
                    Indicator.NAK,
                    chronMessage.getCorrelationId()
            );
        }

        producer.send(reply, msg.getReturnKey());
    }
}
