/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.ace.BagTokenizer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for collections which are ready to be ingested into chronopolis
 * Creates ace tokens and relays the necessary information to the replicating nodes
 *
 * TODO: Send a confirmation to the intake service as well
 *
 * @author shake
 */
public class PackageReadyProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageReadyProcessor.class);
    private ChronProducer producer;
    private IngestProperties props;
    private MessageFactory messageFactory;


    public PackageReadyProcessor(ChronProducer producer, IngestProperties props, MessageFactory messageFactory) {
        this.producer = producer;
        this.props = props;
        this.messageFactory = messageFactory;
    }

    /* 
     * Once we've confirmed that a package is in our staging area we want to do
     * a few things:
     * 1 - Check manifests
     * 2 - Create ACE Tokens 
     * 3 - Send out the collection init message
     * 
     */
    @Override
    public void process(ChronMessage chronMessage) {
        System.out.println("Processing " + chronMessage.getType().toString());
        if ( !(chronMessage instanceof PackageReadyMessage)) {
            // Error out
            log.error("Invalid message type");
        }

        BagTokenizer tokenizer;

        PackageReadyMessage msg = (PackageReadyMessage) chronMessage;

        String location = msg.getLocation();
        String packageName = msg.getPackageName();
        String fixityAlg = msg.getFixityAlgorithm();
        String depositor = msg.getDepositor();
        Path toBag = Paths.get(props.getStage(), location);

        tokenizer = new BagTokenizer(toBag, fixityAlg);
        Path manifest = null;

        try {
            manifest = tokenizer.getAceManifestWithValidation();
        } catch (Exception e) {
            log.error("Error creating manifest " + e);
        }


        // Should end up being the location for a download
        // String tokenStore = "https://chron-monitor.umiacs.umd.edu/tokenStore001";

        // Send message
        StringBuilder tokenStore = new StringBuilder("http://localhost/tokens/");
        tokenStore.append(manifest.getFileName().toString());
        CollectionInitMessage response = messageFactory.collectionInitMessage(120,
                packageName,
                depositor,
                manifest.toString());

        // Hold the routing key here temporarily
        // Will be from the properties soon
        response.setReturnKey("collection.ingest.umiacs");
        producer.send(response, "collection.init.broadcast");
    }

}
