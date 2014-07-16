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
import org.chronopolis.db.model.ReplicationFlow;
import org.chronopolis.db.model.ReplicationState;
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
     * TODO: We can likely move most of the processing into other classes when we start to add on more logic
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

        CollectionIngest ci = manager.getIngestDatabase()
                                     .findByNameAndDepositor(packageName, depositor);
        if (ci == null) {
            ci = new CollectionIngest();
            ci.setTokensGenerated(false);
            ci.setName(packageName);
            ci.setDepositor(depositor);
        }

        // Set up our paths
        Path toBag = Paths.get(props.getStage(), location);
        Path tokenStage = Paths.get(props.getTokenStage());
        String tagManifestDigest; // = tokenizer.getTagManifestDigest();

        // And create our tokens
        Path manifest = null;
        if (ci.getTokensGenerated()) {
            log.info("Tokens already created for {}, skipping", packageName);
            manifest = tokenStage.resolve(depositor)
                                 .resolve(packageName + "-tokens");
            tagManifestDigest = ci.getTagDigest();
        } else {
            tokenizer = new BagTokenizer(toBag, tokenStage, fixityAlg, depositor);
            try {
                manifest = tokenizer.getAceManifestWithValidation();
                ci.setTokensGenerated(true);
            } catch (Exception e) {
                log.error("Error creating ace manifest {}", e);
                success = false;
            }
            tagManifestDigest = tokenizer.getTagManifestDigest();
        }

        // Create digests for replicate nodes to validate from
        String tokenDigest = DigestUtil.digest(manifest, fixity.getName());

        ci.setTagDigest(tagManifestDigest);
        ci.setTokenDigest(tokenDigest);

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

            String correlationId; // = response.getCorrelationId();

            // Figure out which correlationId to use (we reuse any old ones)
            if (ci.getCorrelationId() == null) {
                correlationId = response.getCorrelationId();
                ci.setCorrelationId(correlationId);
            } else {
                correlationId = ci.getCorrelationId();
                response.setCorrelationId(correlationId);
            }

            manager.getIngestDatabase().save(ci);

            // And create our flow items
            for (String node : props.getChronNodes()) {
                createReplicationFlowItem(node, depositor, packageName, correlationId);
            }

            producer.send(response, RoutingKey.REPLICATE_BROADCAST.asRoute());

            SimpleMailMessage message = mailUtil.createMessage(props.getNodeName(),
                    "Package Ready to Replicate",
                    msg.toString());
            mailUtil.send(message);
        } else {
            replyInd = Indicator.NAK;
        }

        // Tell the intake service if we succeeded or not
        PackageReadyReplyMessage reply = messageFactory.packageReadyReplyMessage(
                packageName,
                replyInd,
                msg.getCorrelationId());

        producer.send(reply, msg.getReturnKey());

    }

    private void createReplicationFlowItem(String node, String depositor, String collection, String correlationId) {
        ReplicationFlow flow = new ReplicationFlow();
        flow.setCollection(collection);
        flow.setDepositor(depositor);
        flow.setNode(node);
        flow.setCurrentState(ReplicationState.INIT);
        flow.setCorrelationId(correlationId);
        manager.getReplicationFlowTable().save(flow);
    }

}
