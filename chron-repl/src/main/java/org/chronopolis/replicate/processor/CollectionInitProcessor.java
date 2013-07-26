/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import edu.umiacs.ace.token.TokenStoreEntry;
import edu.umiacs.ace.token.TokenStoreReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.props.GenericProperties;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

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
        System.out.println("Processing message");
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            System.out.println("Error");
            return;
        }

        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        HttpsTransfer xfer = new HttpsTransfer();
        Path manifest = null;
        Path bagPath = Paths.get(props.getStage(), msg.getDepositor());
        Path collPath = Paths.get(bagPath.toString(), msg.getCollection());

        System.out.println("Starting transfer");
        try { 
            manifest = xfer.getFile(msg.getTokenStore(), bagPath);
        } catch (IOException ex) {
            System.out.println("I/O Error in grabbing tokens");
            log.error("Error downloading manifest \n{}", ex);
            return;
        }

        TokenStoreReader reader;
        System.out.println("Starting reader");
        try {
            reader = new TokenStoreReader(Files.newInputStream(manifest, StandardOpenOption.READ), 
                                          "UTF-8");
            // Will be
            // base + depositor + collection
            String url = "http://localhost/bags/"+msg.getCollection()+"/";
            while ( reader.hasNext()) {
                TokenStoreEntry entry = reader.next();
                for ( String identifier : entry.getIdentifiers() ) {
                    System.out.println("Downloading: " + identifier);
                    Path download = Paths.get(collPath.toString(), identifier);
                    xfer.getFile(url+identifier, download);
                }
            }
        } catch (IOException ex) {
            log.error("IO Exception while reading token store \n{}", ex);
            return;
        }

        // Because I'm bad at reading - Collection Init Complete Message
        System.out.println("Sending response");
        ChronMessage2 response = MessageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getReturnKey());
    }
    
}
