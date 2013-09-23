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
import org.apache.log4j.Logger;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.ReplicationQueue;
import org.chronopolis.replicate.util.URIUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {
    private static final Logger log = Logger.getLogger(CollectionInitProcessor.class);

    private ChronProducer producer;
    private ReplicationProperties props; 

    public CollectionInitProcessor(ChronProducer producer, ReplicationProperties props) {
        this.producer = producer;
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
    public void process(ChronMessage2 chronMessage) {
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            System.out.println("Error");
            return;
        }

        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        Path manifest;
        Path bagPath = Paths.get(props.getStage(), msg.getDepositor());
        Path collPath = Paths.get(bagPath.toString(), msg.getCollection());

        try { 
            log.info("Downloading manifest " + msg.getTokenStore());
            manifest = ReplicationQueue.getImmediateFile(msg.getTokenStore(), bagPath);
        } catch (IOException ex) {
            System.out.println("I/O Error in grabbing tokens");
            log.error("Error downloading manifest \n{}", ex);
            return;
        }

        TokenStoreReader reader;
        try {
            reader = new TokenStoreReader(Files.newInputStream(manifest, 
                                          StandardOpenOption.READ), 
                                          "UTF-8");
            // Will be
            // baseURL + depositor (group) + collection + file
            String url = "http://localhost/bags/"+msg.getCollection()+"/";
            while ( reader.hasNext()) {
                TokenStoreEntry entry = reader.next();
                for ( String identifier : entry.getIdentifiers() ) {
                    log.debug("Downloading " + identifier);
                    //Path download = Paths.get(collPath.toString(), identifier);
                    ReplicationQueue.getFileAsync(url, 
                                                  msg.getCollection(), 
                                                  msg.getDepositor(), 
                                                  identifier);
                }
            }
        } catch (IOException ex) {
            log.error("IO Exception while reading token store \n{}", ex);
            return;
        }

        try {
            // Build and POST our collection
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
            coll.put("digestAlgorithm", "SHA-256");
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
            // 1: Unhardcode
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
        ChronMessage2 response = MessageFactory.DefaultCollectionInitCompleteMessage();
        producer.send(response, chronMessage.getReturnKey());
    }
    
}
