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
        }

        ChronMessage2 response = MessageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getChronHeader().getReturnKey());
        
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

        // Break out to new class 
        try {
            // Make HTTP Connection
            URL url = new URL(site);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());

            FileOutputStream fos = new FileOutputStream(filename);
            FileChannel fc = fos.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(blockSize);

            // Write file and update digest
            while ( rbc.read(buf) > 0 ) {
                // Do we want to throw an exception if write < 0?
                byte[] out = buf.array();
                int write = fc.write(buf);
                md.update(out);
                // buf.clear(); // I believe read takes care of this, will test later
            }
            fc.close();

            // Check digests
            // byte[] calculatedDigest = md.digest()
            // convert to String
            // compare
            // return 1 if false



        } catch (IOException ex) {
            LOG.fatal(ex);
        }
        */
    }
    
}
