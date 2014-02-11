/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;

/**
 *
 * @author shake
 */
public class CollectionInitCompleteProcessor implements ChronProcessor {

    private ChronProducer producer;
    private MessageFactory messageFactory;

    public CollectionInitCompleteProcessor(ChronProducer producer, MessageFactory messageFactory) {
        this.producer = producer;
        this.messageFactory = messageFactory;
    }

    @Override
    public void process(ChronMessage chronMessage) {
        ChronMessage response = messageFactory.DefaultPackageIngestCompleteMessage();

        // Once again, hold the routing key temporarily
        producer.send(response, "package.intake.umiacs");
    }
    
}
