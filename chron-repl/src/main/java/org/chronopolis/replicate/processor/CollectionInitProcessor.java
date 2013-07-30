/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import edu.umiacs.ace.token.TokenStoreEntry;
import edu.umiacs.ace.token.TokenStoreReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationProperties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

    private ChronProducer producer;
    private ReplicationProperties props; 

    public CollectionInitProcessor(ChronProducer producer, ReplicationProperties props) {
        this.producer = producer;
        this.props = props;
    }

    private void doPost(String url, JSONObject json) throws IOException,
                        UnsupportedEncodingException {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost(url);
        HttpHost host = new HttpHost(props.getAceFqdn(), props.getAcePort());
        client.getCredentialsProvider().setCredentials(
                new AuthScope(host.getHostName(), 
                              host.getPort()), 
                new UsernamePasswordCredentials(props.getAceUser(), 
                                                props.getAcePass())
        );
        StringEntity entity = new StringEntity(json.toString(), 
                                               ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        client.execute(host, post);
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
            reader = new TokenStoreReader(Files.newInputStream(manifest, 
                                          StandardOpenOption.READ), 
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

        try {
            JSONObject auditVals = new JSONObject();
            auditVals.put("key", "audit.tokens");
            auditVals.put("value", "true");
            JSONObject proxyVals = new JSONObject();
            proxyVals.put("key", "proxy.data");
            proxyVals.put("value", "false");
            JSONObject settings = new JSONObject().put("entry", 
                                                   new JSONArray().put(auditVals)
                                                                  .put(proxyVals));
            JSONObject obj = new JSONObject();
            obj.put("digestAlgorithm", "SHA-256");
            obj.put("settings", settings);
            obj.put("directory", collPath.toString());
            obj.put("name", msg.getCollection());
            obj.put("group", msg.getDepositor());
            obj.put("storage", "local");
            
        } catch (JSONException ex) {
            java.util.logging.Logger.getLogger(CollectionInitProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // doPost(url, obj);

        // Because I'm bad at reading - Collection Init Complete Message
        System.out.println("Sending response");
        ChronMessage2 response = MessageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getReturnKey());
    }
    
}
