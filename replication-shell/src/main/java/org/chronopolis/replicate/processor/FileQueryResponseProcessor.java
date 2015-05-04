/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.file.FileQueryResponseMessage;

/**
 *
 * @author shake
 */
@Deprecated
public class FileQueryResponseProcessor implements ChronProcessor {
    private ChronProducer producer;

    public FileQueryResponseProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(ChronMessage chronMessage) {
        if (!(chronMessage instanceof FileQueryResponseMessage)) {
            // Error out
        } 
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
