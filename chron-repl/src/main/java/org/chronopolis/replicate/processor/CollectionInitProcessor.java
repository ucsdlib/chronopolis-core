/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import edu.umiacs.ace.token.TokenStoreEntry;
import edu.umiacs.ace.token.TokenStoreReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.ReplicationQueue;
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

    // Helper to POST to ACE
    private HttpResponse doPost(String url, HttpEntity entity) throws IOException,
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

        post.setEntity(entity);
        return client.execute(host, post);
    }

    // Helper to get the id of the newly created collection
    // Maybe I should have ACE return a json blob on a successful collection creation 
    private  int getCollectionId(String collection, String group) throws IOException, 
                                                                         JSONException {
        DefaultHttpClient client = new DefaultHttpClient();
        StringBuilder uri = new StringBuilder("http://");
        uri.append(props.getAceFqdn()).append("/");
        uri.append(props.getAcePath()).append("/");
        uri.append("rest/collection/json/");
        uri.append(collection).append("/");
        uri.append(group).append("/");
        HttpGet get = new HttpGet(uri.toString());
        HttpHost host = new HttpHost(props.getAceFqdn(), props.getAcePort());
        client.getCredentialsProvider().setCredentials(
                new AuthScope(host.getHostName(), host.getPort()), 
                new UsernamePasswordCredentials(props.getAceUser(), 
                                                props.getAcePass()));
        HttpResponse response = client.execute(get);
        if ( response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Could not get collection!");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        JSONObject responseJson = new JSONObject(reader);
        return responseJson.getInt("id");
    }

    // TODO: Register token store in to ACE
    // TODO: Stuff
    public void process(ChronMessage2 chronMessage) {
        System.out.println("Processing message");
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            System.out.println("Error");
            return;
        }

        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        Path manifest;
        Path bagPath = Paths.get(props.getStage(), msg.getDepositor());
        Path collPath = Paths.get(bagPath.toString(), msg.getCollection());

        System.out.println("Starting transfer");
        try { 
            manifest = ReplicationQueue.getImmediateFile(msg.getTokenStore(), bagPath);
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
                    //xfer.getFile(url+identifier, download);
                }
            }
        } catch (IOException ex) {
            log.error("IO Exception while reading token store \n{}", ex);
            return;
        }

        // Do we want to force https?
        StringBuilder uri = new StringBuilder("http://");
        uri.append(props.getAceFqdn()).append("/");
        uri.append(props.getAcePath()).append("/");
        int mark = uri.length();
        // and the restful path now...
        uri.append("rest/collection/");
        
        try {
            // Build and POST our collection
            JSONObject auditVals = new JSONObject();
            auditVals.put("key", "audit.tokens");
            auditVals.put("value", "true");
            JSONObject proxyVals = new JSONObject();
            proxyVals.put("key", "proxy.data");
            proxyVals.put("value", "false");
            JSONObject settings = new JSONObject().put("entry", 
                                                   new JSONArray().put(auditVals)
                                                                  .put(proxyVals));
            JSONObject coll = new JSONObject();
            coll.put("digestAlgorithm", "SHA-256");
            coll.put("settings", settings);
            coll.put("directory", collPath.toString());
            coll.put("name", msg.getCollection());
            coll.put("group", msg.getDepositor());
            coll.put("storage", "local");
            StringEntity entity = new StringEntity(coll.toString(), 
                                                   ContentType.APPLICATION_JSON);

            HttpResponse req = doPost(uri.toString(), entity);
            // 2 things
            // 1: Unhardcode
            // 2: Log also
            if ( req.getStatusLine().getStatusCode() != 200 ) {
                throw new RuntimeException("Could not POST collection");
            }

            // Get the ID of the newly made collection
            int id = getCollectionId(msg.getCollection(), msg.getDepositor());

            // Now let's POST the token store
            FileEntity fileEntity = new FileEntity(manifest.toFile(), 
                                                   ContentType.MULTIPART_FORM_DATA);
            uri.delete(mark, uri.length());
            uri.append("rest/tokenstore/");
            uri.append(id);
            doPost(uri.toString(), fileEntity);
        } catch (JSONException ex) {
            java.util.logging.Logger.getLogger(CollectionInitProcessor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(CollectionInitProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // doPost(url, obj);

        // Because I'm bad at reading - Collection Init Complete Message
        System.out.println("Sending response");
        ChronMessage2 response = MessageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getReturnKey());
    }
    
}
