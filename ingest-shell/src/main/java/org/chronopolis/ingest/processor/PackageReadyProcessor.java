/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.ace.BagTokenizer;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.digest.DigestUtil;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.model.CollectionIngest;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.exception.InvalidMessageException;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.messaging.pkg.PackageReadyReplyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Processor for collections which are ready to be ingested into chronopolis
 * Creates ace tokens and relays the necessary information to the replicating nodes
 *
 *
 * @author shake
 */
public class PackageReadyProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageReadyProcessor.class);
    private final ChronProducer producer;
    private final IngestProperties props;
    private final MessageFactory messageFactory;
    private final DatabaseManager manager;
    private final MailUtil mailUtil;


    public PackageReadyProcessor(ChronProducer producer,
                                 IngestProperties props,
                                 MessageFactory messageFactory,
                                 DatabaseManager manager,
                                 MailUtil mailUtil) {
        this.producer = producer;
        this.props = props;
        this.messageFactory = messageFactory;
        this.manager = manager;
        this.mailUtil = mailUtil;
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
        boolean success = true;
        if (!(chronMessage instanceof PackageReadyMessage)) {
            // Error out
            log.error("Invalid message type");
            throw new InvalidMessageException("Expected message of type PackageReadyMessage "
                    + "but received " + chronMessage.getClass().getName());
        }

        BagTokenizer tokenizer;

        PackageReadyMessage msg = (PackageReadyMessage) chronMessage;

        String location = msg.getLocation();
        String packageName = msg.getPackageName();
        String fixityAlg = msg.getFixityAlgorithm();
        Digest fixity = Digest.fromString(msg.getFixityAlgorithm());
        String depositor = msg.getDepositor();
        Boolean toDpn = msg.toDpn();

        // Save some info about the object
        CollectionIngest ci = new CollectionIngest();
        ci.setCorrelationId(msg.getCorrelationId());
        ci.setToDpn(toDpn);
        manager.getIngestDatabase().save(ci);

        // Set up our paths
        Path toBag = Paths.get(props.getStage(), location);
        Path tokenStage = Paths.get(props.getTokenStage());

        // And create our tokens
        tokenizer = new BagTokenizer(toBag, tokenStage, fixityAlg);
        Path manifest = null;
        try {
            manifest = tokenizer.getAceManifestWithValidation();
        } catch (Exception e) {
            log.error("Error creating ace manifest {}", e);
            success = false;
        }

        // Create digests for replicate nodes to validate from
        String tagManifestDigest = tokenizer.getTagManifestDigest();
        String tokenDigest = DigestUtil.digest(manifest, fixity.getName());

        String user = props.getExternalUser();
        String server = props.getStorageServer();

        // Should end up being the location for a download
        StringBuilder tokenStore = new StringBuilder(user);
        tokenStore.append("@").append(server);
        tokenStore.append(":").append(manifest.toString());
        StringBuilder bagLocation = new StringBuilder(user);
        bagLocation.append("@").append(server);
        bagLocation.append(":").append(toBag.toString());


        Indicator replyInd;

        if (success) {
            replyInd = Indicator.ACK;

            // Start the replication
            // TODO: Choose the preferred protocol (maybe from properties?)
            CollectionInitMessage response = messageFactory.collectionInitMessage(
                    120,
                    packageName,
                    depositor,
                    "rsync",
                    tokenStore.toString(),
                    tokenDigest,
                    bagLocation.toString(),
                    tagManifestDigest,
                    fixity);

            producer.send(response, RoutingKey.REPLICATE_BROADCAST.asRoute());
        } else {
            replyInd = Indicator.NAK;
        }

        // Tell the intake service if we succeeded or not
        PackageReadyReplyMessage reply = messageFactory.packageReadyReplyMessage(
                packageName,
                replyInd,
                msg.getCorrelationId());

        producer.send(reply, msg.getReturnKey());

        sendPackageReadyNotification(msg);
    }

    private void sendPackageReadyNotification(PackageReadyMessage packageReadyMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(props.getNodeName() + "-ingest@" + mailUtil.getSmtpFrom());
        message.setTo(mailUtil.getSmtpTo());
        message.setSubject("[" + props.getNodeName() + "] Received new package");
        message.setText(packageReadyMessage.toString());
        mailUtil.send(message);
    }

}
