/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.common.transfer.FileTransfer;

/**
 *
 * @author shake
 */
public class PackageReadyProcessor implements ChronProcessor {

    private ChronProducer producer;

    public PackageReadyProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    /* Once we've confirmed that a package is in our staging area we want to do
     * a few things:
     * 1 - Check if the package is a bag
     *   .5 - If not, create a bag
     * 2 - Create ACE Tokens 
     * 3 - Send out the collection init message
     * 
     */
    @Override
    public void process(ChronMessage2 chronMessage) {
        System.out.println("Processing " + chronMessage.getType().toString());
        if ( !(chronMessage instanceof PackageReadyMessage)) {
            // Error out
        }
        // Things to do:
        // 1: Validate message
        // 2: Grab bag
        // 3: validate and create token store
        
        // String protocol = getProtocol();
        //FileTransfer transferObj = null;
        
        /*
        if (protocol.equals("rsync")) {
            transferObj = new RSyncTransfer();
        } else if (protocol.equals("https")) {
            transferObj = new HttpsTransfer();
        } else {
            // Unsupported protocol
        }
        */
        
        // Should end up being the location for a download
        //String tokenStore = "https://chron-monitor.umiacs.umd.edu/tokenStore001";
        // Send message
        ChronMessage2 msg = MessageFactory.DefaultCollectionInitMessage();

        // Sending the next message will be done in the ingest consumer?
        // CollectionInitMessage collectionInitRequest = new CollectionInitMessage();
        // collectionInitRequest.setAuditPeriod("somedefinedperiod");
        // collectionInitRequest.setCollection(getPackageName());
        // collectionInitRequest.setDepositor(getDepositor());
        // collectionInitRequest.setTokenStore(tokenStore);


        // Hold the routing key here temporarily
        // Will be from the properties soon
        msg.setReturnKey("collection.ingest.umiacs");
        producer.send(msg, "collection.init.broadcast");
    }

}
