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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * TODO: How to reply to collection init message if there is an error
 *
 * @author shake
 */
public class CollectionInitProcessor implements ChronProcessor {
    private static final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final ReplicationProperties props;
    private final MailUtil mailUtil;
    private final AceService aceService;

    public CollectionInitProcessor(ChronProducer producer,
                                   MessageFactory messageFactory,
                                   ReplicationProperties props,
                                   MailUtil mailUtil) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.props = props;
        this.mailUtil = mailUtil;

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


    private void setAceTokenStore(String collection,
                                  String group,
                                  Path collPath,
                                  Path manifest,
                                  String fixityAlg,
                                  int auditPeriod) throws IOException {
        log.trace("Building ACE json");
        GsonCollection aceGson = new GsonCollection();
        aceGson.setDigestAlgorithm(fixityAlg);
        aceGson.setDirectory(collPath.toString());
        aceGson.setName(collection);
        aceGson.setGroup(group);
        aceGson.setStorage("local");
        aceGson.setAuditPeriod(String.valueOf(auditPeriod));
        aceGson.setAuditTokens("true");
        aceGson.setProxyData("false");

        // With retrofit
        // TODO: This will throw a RetrofitError if the collection is already registered,
        // we need a callback to mitigate this
        Map<String, Integer> idMap = aceService.addCollection(aceGson);

        long id = idMap.get("id");

        Callback tsCallback = new Callback() {
            @Override
            public void success(final Object o, final Response response) {
                log.info("Successfully posted token store");
            }

            @Override
            public void failure(final RetrofitError retrofitError) {
                log.error("Error posting token store {} {}",
                        retrofitError.getResponse().getStatus(),
                        retrofitError.getBody());
            }
        };

        // The last retrofit
        aceService.loadTokenStore(id, new TypedFile("ASCII Text", manifest.toFile()), tsCallback);
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
            log.info("Downloading manifest " + msg.getTokenStore());
            manifest = ReplicationQueue.getFileImmediate(msg.getTokenStore(),
                                                         Paths.get(props.getStage()),
                                                         protocol);
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
        } catch (FileTransferException ex) {
            log.error("Error replicating bag");
            smm = createErrorMail(ex, msg);
            mailUtil.send(smm);
            return;
        }

        try {
            if (register) {
                setAceTokenStore(collection, group, collPath, manifest, fixityAlg, auditPeriod);
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


    // Mail Helpers

    private SimpleMailMessage createSuccess(CollectionInitMessage msg) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(mailUtil.getSmtpTo());
        message.setFrom(props.getNodeName() + "-replicate@" + mailUtil.getSmtpFrom());
        message.setSubject("Successful replication of " + msg.getCollection());
        message.setText(msg.toString());

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
        message.setSubject("Error in CollectionInit");
        message.setText("Message: \n" + msg.toString() + "\n\nError: \n" + exception.toString());

        return message;
    }

}
