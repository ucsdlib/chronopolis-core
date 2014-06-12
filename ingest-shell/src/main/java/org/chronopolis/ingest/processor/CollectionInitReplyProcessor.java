package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;

/**
 * Created by shake on 6/12/14.
 */
public class CollectionInitReplyProcessor implements ChronProcessor {

    private final IngestProperties properties;
    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final DatabaseManager manager;

    public CollectionInitReplyProcessor(final IngestProperties properties,
                                        final ChronProducer producer,
                                        final MessageFactory messageFactory,
                                        final DatabaseManager manager) {
        this.properties = properties;
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.manager = manager;
    }

    @Override
    public void process(final ChronMessage chronMessage) {

    }
}
