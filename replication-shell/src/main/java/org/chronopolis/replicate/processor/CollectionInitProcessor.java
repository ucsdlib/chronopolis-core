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
import java.nio.file.spi.FileSystemProvider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.ReplicationQueue;
import org.chronopolis.replicate.util.URIUtil;
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
    private static final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

    private ChronProducer producer;
    private MessageFactory messageFactory;
    private ReplicationProperties props;

    public CollectionInitProcessor(ChronProducer producer,
                                   MessageFactory messageFactory,
                                   ReplicationProperties props) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.props = props;
    }

    private HttpResponse executeRequest(HttpRequest req) throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpHost host = new HttpHost(props.getAceFqdn(), props.getAcePort());

        client.getCredentialsProvider().setCredentials(
                new AuthScope(host.getHostName(),host.getPort()), 
                new UsernamePasswordCredentials(props.getAceUser(), 
                                                props.getAcePass()));

        
        return client.execute(host, req);
    }

    // Helper to POST to ACE
    private HttpResponse doPost(String url, 
                                HttpEntity entity) throws IOException,
                                                          UnsupportedEncodingException {
        log.info("Posting to " + url + " with " + entity.toString());
        HttpPost post = new HttpPost(url);
        post.setEntity(entity);
        return executeRequest(post);
    }

    // Helper to get the id of the newly created collection
    // Maybe I should have ACE return a json blob on a successful collection creation 
    private  int getCollectionId(String collection, 
                                 String group) throws IOException, 
                                                      JSONException {
        String uri = URIUtil.buildACECollectionGet(props.getAceFqdn(), 
                                                   props.getAcePort(), 
                                                   props.getAcePath(), 
                                                   collection, 
                                                   group);
        HttpGet get = new HttpGet(uri.toString());

        HttpResponse response = executeRequest(get);
        if ( response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Could not get collection!");
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

        // The JSON builder is kind of broken
        // luckily we only get a single line back from ACE
        String r = reader.readLine();
        JSONObject responseJson = new JSONObject(r);
        log.info(responseJson.toString());
        return responseJson.getInt("id");
    }

    // TODO: Register token store in to ACE
    // TODO: Stuff
    @Override
    public void process(ChronMessage chronMessage) {
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            System.out.println("Error");
            return;
        }

        log.trace("Received collection init message");
        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        Path manifest;
        Path bagPath = Paths.get(props.getStage(), msg.getDepositor());
        Path collPath = Paths.get(bagPath.toString(), msg.getCollection());
        String fixityAlg = msg.getFixityAlgorithm();
        String protocol = msg.getProtocol();

        try { 
            log.info("Downloading manifest " + msg.getTokenStore());
            manifest = ReplicationQueue.getFileImmediate(msg.getTokenStore(), Paths.get(props.getStage()), protocol);
        } catch (IOException ex) {
            log.error("Error downloading manifest \n{}", ex);
            return;
        }

        FileTransfer transfer;
        if ( protocol.equalsIgnoreCase("https")) {
            transfer = new HttpsTransfer();
        } else {
            transfer = new RSyncTransfer("shake");
        }

        transfer.getFile(msg.getBagLocation(), Paths.get(props.getStage()));
        /*
        TokenStoreReader reader;
        try {
            reader = new TokenStoreReader(Files.newInputStream(manifest, 
                                          StandardOpenOption.READ), 
                                          "UTF-8");
            // Will be
            // baseURL + depositor (group) + collection + file
            String url = msg.getTokenStore();
            while ( reader.hasNext()) {
                TokenStoreEntry entry = reader.next();
                for ( String identifier : entry.getIdentifiers() ) {
                    log.debug("Downloading " + identifier);
                    //Path download = Paths.get(collPath.toString(), identifier);
                    /*
                     * Let's get this to work with downloads first, then worry about
                     * multithreading later
                    ReplicationQueue.getFileAsync(url, 
                                                  msg.getCollection(), 
                                                  msg.getDepositor(), 
                                                  identifier,
                                                  protocol);
                                                  /
                }
            }
        } catch (IOException ex) {
            log.error("IO Exception while reading token store \n{}", ex);
            return;
        }
        */

        try {
            log.trace("Building ACE json");
            // Build and POST our collection
            // TODO: Functionize 
            JSONObject auditVals = new JSONObject();
            auditVals.put("key", "audit.tokens");
            auditVals.put("value", "true");
            JSONObject proxyVals = new JSONObject();
            proxyVals.put("key", "proxy.data");
            proxyVals.put("value", "false");
            JSONObject auditPeriod = new JSONObject();
            auditPeriod.put("key", "audit.period");
            auditPeriod.put("value", msg.getAuditPeriod());
            JSONObject settings = new JSONObject().put("entry", 
                                                   new JSONArray().put(auditVals)
                                                                  .put(proxyVals)
                                                                  .put(auditPeriod));

            JSONObject coll = new JSONObject();
            coll.put("digestAlgorithm", fixityAlg);
            coll.put("settings", settings);
            coll.put("directory", collPath.toString());
            coll.put("name", msg.getCollection());
            coll.put("group", msg.getDepositor());
            coll.put("storage", "local");
            StringEntity entity = new StringEntity(coll.toString(), 
                                                   ContentType.APPLICATION_JSON);
            String uri = URIUtil.buildACECollectionPost(props.getAceFqdn(), 
                                                        props.getAcePort(), 
                                                        props.getAcePath());
            HttpResponse req = doPost(uri, entity);
            // 2 things
            // 2: Log also
            log.info(req.getStatusLine().toString());
            if ( req.getStatusLine().getStatusCode() != 200 ) {
                throw new RuntimeException("Could not POST collection");
            }

            // Get the ID of the newly made collection
            int id = getCollectionId(msg.getCollection(), msg.getDepositor());

            // Now let's POST the token store
            FileBody body = new FileBody(manifest.toFile());
            MultipartEntity mpEntity = new MultipartEntity();
            mpEntity.addPart("tokenstore", body);
            uri = URIUtil.buildACETokenStorePost(props.getAceFqdn(), 
                                                 props.getAcePort(), 
                                                 props.getAcePath(), 
                                                 id);
            doPost(uri, mpEntity);
        } catch (JSONException ex) {
            log.error("Error creating json", ex);
        } catch (IOException ex) {
            log.error("IO Error", ex);
        }
        
        // Because I'm bad at reading - Collection Init Complete Message
        log.info("Sending response");
        ChronMessage response = messageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getReturnKey());
    }
    
}
