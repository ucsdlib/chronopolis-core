/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;

/**
 *
 * @author shake
 */
public class CollectionInitCompleteProcessor implements ChronProcessor {

    private ChronProducer producer;

    public CollectionInitCompleteProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(ChronMessage2 chronMessage) {
        ChronMessage2 response = MessageFactory.DefaultPackageIngestCompleteMessage();

        producer.send(response, "collection.init.byah");
    }
    
}
