/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.CredentialRequestInterceptor;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.ReplicationQueue;
import org.chronopolis.replicate.util.URIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TODO: How to reply to collection init message if there is an error
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {
    private static final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

    private static final String TOKEN_DOWNLOAD = "TokenStore-Download";
    private static final String BAG_DOWNLOAD = "Bag-Download";
    private static final String ACE_REGISTER_COLLECTION = "Ace-Register-Collection";
    private static final String ACE_REGISTER_TOKENS = "Ace-Register-Tokens";
    private static final String INCOMPLETE = "Incomplete";

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final ReplicationProperties props;
    private final MailUtil mailUtil;
    private final AceService aceService;

    private HashMap<String, String> completionMap;
    private AtomicBoolean callbackComplete = new AtomicBoolean(false);

    public CollectionInitProcessor(ChronProducer producer,
                                   MessageFactory messageFactory,
                                   ReplicationProperties props,
                                   MailUtil mailUtil) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.props = props;
        this.mailUtil = mailUtil;

        // Set up our map of tasks and their progress for more info in mail messages
        completionMap = new HashMap<>();
        completionMap.put(TOKEN_DOWNLOAD, INCOMPLETE);
        completionMap.put(BAG_DOWNLOAD, INCOMPLETE);
        completionMap.put(ACE_REGISTER_COLLECTION, INCOMPLETE);
        completionMap.put(ACE_REGISTER_TOKENS, INCOMPLETE);

        // This might be better done through the dependency injection framework
        String endpoint = URIUtil.buildAceUri(props.getAceFqdn(),
                props.getAcePort(),
                props.getAcePath()).toString();

        CredentialRequestInterceptor interceptor = new CredentialRequestInterceptor(
                props.getAceUser(),
                props.getAcePass());

        RestAdapter restAdapter = new RestAdapter.Builder()
                                                 .setEndpoint(endpoint)
                                                 .setRequestInterceptor(interceptor)
                                                 .build();
        aceService = restAdapter.create(AceService.class);
    }



    // TODO: Reply if there is an error with the collection (ie: already registered in ace), or ack
    // TODO: Fix the flow of this so that we don't return on each failure...
    // that way we send mail and return in one spot instead of 4
    @Override
    public void process(ChronMessage chronMessage) {
        // TODO: Replace these with the values from the properties
        boolean checkCollection = false;
        boolean register = true;

        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            log.error("Incorrect Message Type");
            return;
        }

        log.trace("Received collection init message");
        SimpleMailMessage smm;
        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        Path manifest;
        Path bagPath = Paths.get(props.getStage(), msg.getDepositor());
        Path collPath = Paths.get(bagPath.toString(), msg.getCollection());
        String fixityAlg = msg.getFixityAlgorithm();
        String protocol = msg.getProtocol();
        String collection = msg.getCollection();
        String group = msg.getDepositor();
        int auditPeriod = msg.getAuditPeriod();

        //noinspection ConstantConditions (temporarily false while testing)
        if (checkCollection) {
            GsonCollection inAce = aceService.getCollectionByName(msg.getCollection(), msg.getDepositor());
            if (inAce != null) {
                log.error("Already registered collection '{}'", msg.getCollection());
            }
        }

        try { 
            log.info("Downloading Token Store" + msg.getTokenStore());
            manifest = ReplicationQueue.getFileImmediate(msg.getTokenStore(),
                                                         Paths.get(props.getStage()),
                                                         protocol);
            completionMap.put(TOKEN_DOWNLOAD, "Successfully downloaded from " + msg.getTokenStore());
            log.info("Finished downloading manifest");
        } catch (IOException ex) {
            log.error("Error downloading manifest \n{}", ex);
            smm = createErrorMail(ex, msg);
            mailUtil.send(smm);
            return;
        } catch (FileTransferException e) {
            log.error("File transfer exception {}", e);
            smm = createErrorMail(e, msg);
            mailUtil.send(smm);
            return;
        }

        FileTransfer transfer;
        String location = msg.getBagLocation();
        // TODO: We'll probably end up just using the entire location for the
        // rsync command instead of splitting it in the future
        String[] parts = location.split("@", 2);
        if (protocol.equalsIgnoreCase("https")) {
            transfer = new HttpsTransfer();
        } else {
            String user = parts[0];
            transfer = new RSyncTransfer(user);
        }

        try {
            transfer.getFile(parts[1], bagPath);
            log.info("Finished downloading bag");
            completionMap.put(BAG_DOWNLOAD, "Successfully downloaded from " + msg.getBagLocation());
        } catch (FileTransferException ex) {
            log.error("Error replicating bag");
            smm = createErrorMail(ex, msg);
            mailUtil.send(smm);
            return;
        }

        try {
            if (register) {
                setAceTokenStore(collection, group, collPath, manifest, fixityAlg, auditPeriod);
                while (!callbackComplete.get()) {
                }
            }
        } catch (IOException ex) {
            log.error("IO Error", ex);
            smm = createErrorMail(ex, msg);
            mailUtil.send(smm);
            return;
        }
        
        // Because I'm bad at reading - Collection Init Complete Message
        log.info("Sending response");
        ChronMessage response = messageFactory.collectionInitCompleteMessage(msg.getCorrelationId());
        producer.send(response, chronMessage.getReturnKey());

        smm = createSuccess(msg);
        mailUtil.send(smm);
    }

    // Function for handling ACE registration

    private void setAceTokenStore(String collection,
                                  String group,
                                  Path collPath,
                                  Path manifest,
                                  String fixityAlg,
                                  int auditPeriod) throws IOException {
        log.trace("Building ACE json");
        GsonCollection aceGson = new GsonCollection.Builder()
                .name(collection)
                .digestAlgorithm(fixityAlg)
                .directory(collPath.toString())
                .group(group)
                .storage("local")
                .auditPeriod(String.valueOf(auditPeriod))
                .auditTokens("true")
                .proxyData("false")
                .build();

        // TODO: This will throw a RetrofitError if the collection is already registered,
        // we need a callback to mitigate this
        log.debug("POSTing {}", aceGson.toJson());
        Map<String, Integer> idMap = aceService.addCollection(aceGson);
        completionMap.put(ACE_REGISTER_COLLECTION, "Successfully registered");

        long id = idMap.get("id");

        Callback tsCallback = new Callback() {
            @Override
            public void success(final Object o, final Response response) {
                log.info("Successfully posted token store");
                completionMap.put(ACE_REGISTER_TOKENS, "Successfully registered with response "
                        + response.getStatus());
                callbackComplete.getAndSet(true);
            }

            @Override
            public void failure(final RetrofitError retrofitError) {
                log.error("Error posting token store {} {}",
                        retrofitError.getResponse().getStatus(),
                        retrofitError.getBody());
                completionMap.put(ACE_REGISTER_TOKENS, "Failed to register tokens:\n"
                        + retrofitError.getBody());
                callbackComplete.getAndSet(true);
            }
        };

        aceService.loadTokenStore(id, new TypedFile("ASCII Text", manifest.toFile()), tsCallback);
    }

    // Mail Helpers

    private SimpleMailMessage createSuccess(CollectionInitMessage msg) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mailUtil.getSmtpTo());
        message.setFrom(props.getNodeName() + "-replicate@" + mailUtil.getSmtpFrom());
        message.setSubject("[" + props.getNodeName() + "] Successful replication of " + msg.getCollection());

        StringWriter stringWriter = new StringWriter();
        PrintWriter textBody = new PrintWriter(stringWriter, true);
        textBody.println("Message received from: " + msg.getOrigin());
        textBody.println(msg.toString());
        textBody.println("\n\nSteps completed:");
        for (Map.Entry entry : completionMap.entrySet()) {
            textBody.println(entry.getKey() + ": " + entry.getValue());
        }
        message.setText(stringWriter.toString());

        return message;
    }

    private SimpleMailMessage createErrorMail(Exception ex, CollectionInitMessage msg) {
        StringBuilder exception = new StringBuilder();
        exception.append(ex.getMessage()).append("\n");
        for (StackTraceElement element : ex.getStackTrace()) {
            exception.append(element.toString()).append("\n");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mailUtil.getSmtpTo());
        message.setFrom(props.getNodeName()+"-replicate@" + mailUtil.getSmtpFrom());
        message.setSubject("[" + props.getNodeName() + "] Error in CollectionInit");
        StringWriter stringWriter = new StringWriter();
        PrintWriter textBody = new PrintWriter(stringWriter, true);
        textBody.println("Message received from: " + msg.getOrigin());
        textBody.println(msg.toString());
        textBody.println("\n\nSteps completed:");
        for (Map.Entry entry : completionMap.entrySet()) {
            textBody.print(entry.getKey() + ": " + entry.getValue());
        }
        textBody.println("\n\nError: \n" + exception.toString());
        message.setText(stringWriter.toString());

        return message;
    }

}
