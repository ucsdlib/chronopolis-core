/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
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

    public CollectionInitProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    public void process(ChronMessage2 chronMessage) {
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            return;
        }

        // Because I'm bad at reading - Collection Init Complete Message
        ChronMessage2 response = MessageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getReturnKey());
        
        /*
         * This is from the old FileConsumer class
         * I'm saving it here for a short time until I get this sorted out
         * Actually it should go in the HttpDownload class... oh well maybe later 
         * 
         * 
        
       // Register collection with ACE
       // POST obj to localhost:8080/ace-am/rest/collection

       // Load token store (File xfer? Yea probably the best)
        
        String depositor = (String) obj.get("depositor");
        String site = (String) obj.get("url");

        // Maybe full path instead
        String filename = (String) obj.get("filename");
        String digestType = (String) obj.get("digest-type");
        String digest = (String) obj.get("digest");

        System.out.println("Pulling file: " + filename);
        System.out.println("Digest to check against: " + digest);
        System.out.println("Digest method: " + digestType);
        System.out.println("Depositor: " + depositor);
        System.out.println("URL: " + site);


        MessageDigest md = null;// MessageDigest.getInstance(digestType);

        // Stop here while we don't have a server to pull from
        if (null != obj) {
            System.out.println("Returning 0");
            return 0;
        }

        */
    }
    
}
