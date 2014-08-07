/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.factory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.collection.CollectionRestoreCompleteMessage;
import org.chronopolis.messaging.collection.CollectionRestoreRequestMessage;
import org.chronopolis.messaging.file.FileQueryMessage;
import org.chronopolis.messaging.file.FileQueryResponseMessage;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusQueryMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusResponseMessage;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.messaging.pkg.PackageReadyReplyMessage;

/**
 * TODO: Order based on length of method names
 * TODO: Move to JodaTime
 * TODO: Remove default methods
 *
 * @author shake
 */
public class MessageFactory {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssz");
    private final GenericProperties properties;

    public MessageFactory(GenericProperties properties) {
        this.properties = properties;
    }

    // TODO: delete this method
    private void setHeaders(ChronMessage msg) {
        msg.setDate(dateFormat.format(new Date()));
        msg.setReturnKey(properties.getInboundKey());
        msg.setOrigin(properties.getNodeName());
        msg.setCorrelationId(UUID.randomUUID().toString());
    }

    private void setHeaders(ChronMessage msg, String correlationId) {
        msg.setDate(dateFormat.format(new Date()));
        msg.setReturnKey(properties.getInboundKey());
        msg.setOrigin(properties.getNodeName());
        msg.setCorrelationId(correlationId);
    }

    public CollectionInitMessage defaultCollectionInitMessage() {
        CollectionInitMessage msg = new CollectionInitMessage();
        msg.setAuditPeriod(90);
        msg.setCollection("default-collection");
        msg.setDepositor("default-depositor");
        msg.setTokenStore("https://localhost/tokenstore");
        setHeaders(msg);
        return msg;
    }

    public CollectionInitMessage collectionInitMessage(int auditPeriod,
                                                       String collection,
                                                       String depositor,
                                                       String protocol,
                                                       String tokenStore,
                                                       String tokenStoreDigest,
                                                       String bagLocation,
                                                       String tagManifestDigest,
                                                       Digest fixityAlgorithm) {
        CollectionInitMessage msg = new CollectionInitMessage();
        msg.setAuditPeriod(auditPeriod);
        msg.setCollection(collection);
        msg.setDepositor(depositor);
        msg.setProtocol(protocol);
        msg.setTokenStore(tokenStore);
        msg.setBagLocation(bagLocation);
        msg.setFixityAlgorithm(fixityAlgorithm);
        msg.setBagTagManifestDigest(tagManifestDigest);
        msg.setTokenStoreDigest(tokenStoreDigest);
        setHeaders(msg);
        return msg;
    }

    public CollectionInitCompleteMessage collectionInitCompleteMessage(String correlationId) {
        CollectionInitCompleteMessage msg = new CollectionInitCompleteMessage();
        setHeaders(msg, correlationId);
        return msg;
    }

    public CollectionInitCompleteMessage defaultCollectionInitCompleteMessage() {
        CollectionInitCompleteMessage msg = new CollectionInitCompleteMessage();
        setHeaders(msg);
        return msg;
    }

    public CollectionInitReplyMessage collectionInitReplyMessage(String correlationId,
                                                                 Indicator messageAtt,
                                                                 String depositor,
                                                                 String collection,
                                                                 List<String> failedItems) {
        CollectionInitReplyMessage msg = new CollectionInitReplyMessage();
        setHeaders(msg, correlationId);

        msg.setMessageAtt(messageAtt);
        msg.setCollection(collection);
        msg.setDepositor(depositor);

        if (messageAtt.equals(Indicator.NAK)) {
            msg.setFailedItems(failedItems);
        }

        return msg;
    }


    public FileQueryMessage defaultFileQueryMessage() {
        FileQueryMessage msg = new FileQueryMessage();
        setHeaders(msg);
        return msg;
    }

    public FileQueryResponseMessage defaultFileQueryResponseMessage() {
        FileQueryResponseMessage msg = new FileQueryResponseMessage();
        setHeaders(msg);
        return msg;
    }

    public PackageReadyMessage defaultPackageReadyMessage() {
        PackageReadyMessage msg = new PackageReadyMessage();
        msg.setDepositor("default-depositor");
        msg.setLocation("default-location");
        msg.setPackageName("default-package-name");
        msg.setFixityAlgorithm(Digest.fromString("SHA-256"));
        msg.setSize(1024);
        setHeaders(msg);
        return msg;
    }

    public PackageReadyMessage packageReadyMessage(String depositor,
                                                   Digest fixityAlg,
                                                   String location,
                                                   String packageName,
                                                   int size) {
        PackageReadyMessage msg = new PackageReadyMessage();
        msg.setDepositor(depositor);
        msg.setFixityAlgorithm(fixityAlg);
        msg.setLocation(location);
        msg.setPackageName(packageName);
        msg.setSize(size);
        setHeaders(msg);
        return msg;
    }

    public PackageReadyReplyMessage packageReadyReplyMessage(String packageName,
                                                             Indicator indicator,
                                                             String correlationId) {
        PackageReadyReplyMessage msg = new PackageReadyReplyMessage();
        msg.setMessageAtt(indicator);
        msg.setPackageName(packageName);
        setHeaders(msg, correlationId);
        return msg;
    }

    public CollectionRestoreRequestMessage collectionRestoreRequestMessage(String collection,
                                                                           String depositor) {
        CollectionRestoreRequestMessage msg = new CollectionRestoreRequestMessage();
        setHeaders(msg);
        msg.setCollection(collection);
        msg.setDepositor(depositor);
        return msg;
    }

    public CollectionRestoreCompleteMessage collectionRestoreReplyMessage(Indicator messageAtt,
                                                                       String location,
                                                                       String correlationId) {
        CollectionRestoreCompleteMessage msg = new CollectionRestoreCompleteMessage();
        setHeaders(msg, correlationId);
        msg.setMessageAtt(messageAtt);
        if (messageAtt.equals(Indicator.ACK)) {
            msg.setLocation(location);
        }
        return msg;
    }



    public PackageIngestCompleteMessage DefaultPackageIngestCompleteMessage() {
        PackageIngestCompleteMessage msg = new PackageIngestCompleteMessage();
        setHeaders(msg);
        return msg;
    }

    public PackageIngestStatusQueryMessage DefaultPackageIngestStatusQueryMessage() {
        PackageIngestStatusQueryMessage msg = new PackageIngestStatusQueryMessage();
        setHeaders(msg);
        return msg;
    }

    public PackageIngestStatusResponseMessage DefaultPackageIngestStatusResponseMessage() {
        PackageIngestStatusResponseMessage msg = new PackageIngestStatusResponseMessage();
        setHeaders(msg);
        return msg;
    }

}
