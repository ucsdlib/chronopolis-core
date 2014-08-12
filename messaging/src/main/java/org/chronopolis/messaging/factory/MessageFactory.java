/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.factory;

import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.collection.CollectionRestoreCompleteMessage;
import org.chronopolis.messaging.collection.CollectionRestoreLocationMessage;
import org.chronopolis.messaging.collection.CollectionRestoreReplyMessage;
import org.chronopolis.messaging.collection.CollectionRestoreRequestMessage;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.messaging.pkg.PackageReadyReplyMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * TODO: Move to JodaTime
 * TODO: Remove default methods
 * TODO: If this becomes too big, we could make multiple builders for each message flow
 *
 * @author shake
 */
public class MessageFactory {

    private final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssz");

    private final ChronopolisSettings settings;

    public MessageFactory(ChronopolisSettings settings) {
        this.settings = settings;
    }

    private void setHeaders(ChronMessage msg) {
        setHeaders(msg, UUID.randomUUID().toString());
    }

    private void setHeaders(ChronMessage msg, String correlationId) {
        msg.setDate(dateFormat.format(new Date()));
        msg.setReturnKey(settings.getInboundKey());
        msg.setOrigin(settings.getNode());
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

    public CollectionRestoreRequestMessage collectionRestoreRequestMessage(String collection,
                                                                           String depositor,
                                                                           String correlationId) {
        CollectionRestoreRequestMessage msg = new CollectionRestoreRequestMessage();
        setHeaders(msg, correlationId);
        msg.setCollection(collection);
        msg.setDepositor(depositor);
        return msg;
    }

    public CollectionRestoreCompleteMessage collectionRestoreCompleteMessage(Indicator messageAtt,
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

    public CollectionRestoreReplyMessage collectionRestoreReplyMessage(Indicator messageAtt,
                                                                       String correlationId) {
        CollectionRestoreReplyMessage msg = new CollectionRestoreReplyMessage();
        setHeaders(msg, correlationId);
        msg.setMessageAtt(messageAtt);
        return msg;
    }

    public CollectionRestoreLocationMessage collectionRestoreLocationMessage(String protocol,
                                                                             String location,
                                                                             Indicator messageAtt,
                                                                             String correlationId) {
        CollectionRestoreLocationMessage msg = new CollectionRestoreLocationMessage();
        setHeaders(msg, correlationId);
        msg.setMessageAtt(messageAtt);
        if (Indicator.ACK.equals(messageAtt)) {
            msg.setProtocol(protocol);
            msg.setRestoreLocation(location);
        }
        return msg;
    }



    public PackageIngestCompleteMessage DefaultPackageIngestCompleteMessage() {
        PackageIngestCompleteMessage msg = new PackageIngestCompleteMessage();
        setHeaders(msg);
        return msg;
    }

}
