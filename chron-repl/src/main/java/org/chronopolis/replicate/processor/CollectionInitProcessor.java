/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.props.GenericProperties;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;

/**
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {

    private ChronProducer producer;
    private GenericProperties props;

    public CollectionInitProcessor(ChronProducer producer, GenericProperties props) {
        this.producer = producer;
        this.props = props;
    }

    // TODO: Register token store in to ACE
    // TODO: Download tokens from manifest
    // TODO: Stuff
    public void process(ChronMessage2 chronMessage) {
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            return;
        }

        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        HttpsTransfer xfer = new HttpsTransfer();
        try { 
            xfer.getFile(msg.getTokenStore(), Paths.get(props.getStage()));
        } catch (IOException ex) {
            Logger.getLogger(CollectionInitProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Because I'm bad at reading - Collection Init Complete Message
        ChronMessage2 response = MessageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getReturnKey());
    }
    
}
