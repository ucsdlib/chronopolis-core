/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
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
import org.chronopolis.ingest.config.IngestSettings;
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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
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
    private final IngestSettings settings;
    private final MessageFactory messageFactory;
    private final DatabaseManager manager;
    private final MailUtil mailUtil;
    private static final String TAR_TYPE = "application/x-tar";


    public PackageReadyProcessor(ChronProducer producer,
                                 IngestSettings settings,
                                 MessageFactory messageFactory,
                                 DatabaseManager manager,
                                 MailUtil mailUtil) {
        this.producer = producer;
        this.settings = settings;
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
        Path toBag = Paths.get(settings.getBagStage(), location);
        try {
            String mimeType = Files.probeContentType(toBag);
            if (mimeType != null && mimeType.equals(TAR_TYPE)) {
                toBag = untar(toBag, depositor);
            }
        } catch (IOException e) {
            log.error("Error probing mime type for bag", e);
            throw new RuntimeException(e);
        }


        Path tokenStage = Paths.get(settings.getTokenStage());
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

        String user = settings.getExternalUser();
        String server = settings.getStorageServer();

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
            for (String node : settings.getChronNodes()) {
                createReplicationFlowItem(node, depositor, packageName, correlationId);
            }

            producer.send(response, RoutingKey.REPLICATE_BROADCAST.asRoute());

            SimpleMailMessage message = mailUtil.createMessage(settings.getNode(),
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

    /**
     * Given a path to a tar file, explode the tar and return the top-level directory
     *
     * @param toBag
     * @return the path of the top-level directory
     * @throws IOException
     */
    private Path untar(final Path toBag, String depositor) throws IOException {
        // Set up our tar stream and channel
        TarArchiveInputStream tais = new TarArchiveInputStream(Files.newInputStream(toBag));
        TarArchiveEntry entry = tais.getNextTarEntry();
        ReadableByteChannel inChannel = Channels.newChannel(tais);

        // Get our root path (just the staging area), and create an updated bag path
        Path root = Paths.get(settings.getBagStage(), depositor);
        Path bag = root.resolve(entry.getName());

        while (entry != null) {
            Path entryPath = root.resolve(entry.getName());

            if (entry.isDirectory()) {
                log.trace("Creating directory {}", entry.getName());
                Files.createDirectories(entryPath);
            } else {
                log.trace("Creating file {}", entry.getName());

                entryPath.getParent().toFile().mkdirs();

                // In case files are greater than 2^32 bytes, we need to use a
                // RandomAccessFile and FileChannel to write them
                RandomAccessFile file = new RandomAccessFile(entryPath.toFile(), "rw");
                FileChannel out = file.getChannel();

                // The TarArchiveInputStream automatically updates its offset as
                // it is read, so we don't need to worry about it
                out.transferFrom(inChannel, 0, entry.getSize());
                out.close();
            }

            entry = tais.getNextTarEntry();
        }

        // Because we aren't always certain the first element of the tar file
        // is the root directory, we need to resolve it from the bag variable
        // TODO: There might be a cleaner way to do this
        return root.resolve(bag.getName(root.getNameCount()));
    }

    private void createReplicationFlowItem(String node,
                                           String depositor,
                                           String collection,
                                           String correlationId) {
        ReplicationFlow flow = manager
                .getReplicationFlowTable()
                .findByDepositorAndCollectionAndNode(depositor, collection, node);
        // If we haven't seen this before, set up the flow item
        if (flow == null) {
            flow = new ReplicationFlow();
            flow.setCollection(collection);
            flow.setDepositor(depositor);
            flow.setNode(node);
            flow.setCurrentState(ReplicationState.INIT);
        }
        // Else check to see if we are retrying a replication
        else {
            ReplicationState state = flow.getCurrentState();
            if (state == ReplicationState.FAILED) {
                state = ReplicationState.RETRY;
                flow.setCurrentState(state);
            }
        }

        // Update the correlationId no matter hwhat
        // (so we can find the flow item in the other processors)
        flow.setCorrelationId(correlationId);

        manager.getReplicationFlowTable().save(flow);
    }

}
