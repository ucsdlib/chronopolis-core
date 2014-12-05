package org.chronopolis.replicate;

import org.chronopolis.common.digest.Digest;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.rest.models.Replication;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by shake on 10/29/14.
 */
public class ReplicationNotifier implements Notifier {

    private final CollectionInitMessage message;
    private boolean success = true;
    private String aceStep;
    private String bagStep;
    private String tokenStep;
    private String rsyncStats;
    private String origin;
    private String messageText;

    private String calculatedTagDigest;
    private String calculatedTokenDigest;


    public ReplicationNotifier(CollectionInitMessage message) {
        this.origin = message.getOrigin();
        this.messageText = message.toString();
        this.message = message;
        this.calculatedTagDigest = message.getTagManifestDigest();
        this.calculatedTokenDigest = message.getTokenStoreDigest();
    }

    public ReplicationNotifier(Replication replication) {
        // temporary while the messaging is still part of the codebase
        CollectionInitMessage empty = new CollectionInitMessage();
        // Headers
        empty.setCorrelationId("");
        empty.setOrigin("");
        empty.setReturnKey("");
        empty.setDate("");

        // Body
        empty.setTokenStoreDigest("");
        empty.setTokenStore("");
        empty.setBagTagManifestDigest("");
        empty.setBagLocation("");
        empty.setDepositor(replication.getBag().getDepositor());
        empty.setCollection(replication.getBag().getName());
        empty.setProtocol("");
        empty.setAuditPeriod(-1);
        empty.setFixityAlgorithm(Digest.SHA_256);
        this.message = empty;

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.origin = "restful interface";
        try {
            this.messageText = mapper.writeValueAsString(replication.getBag());
        } catch (IOException e) {
            this.messageText = "Error writing replication object to json";
        }
    }

    @Override
    public String getNotificationBody() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter textBody = new PrintWriter(stringWriter, true);
        textBody.println("Message received from: " + origin);
        textBody.println(messageText);
        textBody.println();
        textBody.println();
        textBody.println("Step status:");
        textBody.println("Token Download - " + tokenStep);
        textBody.println("Bag Download - " + bagStep);
        textBody.println("ACE Register/Audit - " + aceStep);
        textBody.println("Transfer Stats:");
        textBody.println(rsyncStats);
        return stringWriter.toString();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(final boolean success) {
        this.success = success;
    }

    public String getAceStep() {
        return aceStep;
    }

    public void setAceStep(final String aceStep) {
        this.aceStep = aceStep;
    }

    public String getBagStep() {
        return bagStep;
    }

    public void setBagStep(final String bagStep) {
        this.bagStep = bagStep;
    }

    public String getTokenStep() {
        return tokenStep;
    }

    public void setTokenStep(final String tokenStep) {
        this.tokenStep = tokenStep;
    }

    public String getRsyncStats() {
        return rsyncStats;
    }

    public void setRsyncStats(final String rsyncStats) {
        this.rsyncStats = rsyncStats;
    }

    public CollectionInitMessage getMessage() {
        return message;
    }

    public String getCalculatedTagDigest() {
        return calculatedTagDigest;
    }

    public String getCalculatedTokenDigest() {
        return calculatedTokenDigest;
    }

    public void setCalculatedTokenDigest(final String calculatedTokenDigest) {
        this.calculatedTokenDigest = calculatedTokenDigest;
    }

    public void setCalculatedTagDigest(final String calculatedTagDigest) {
        this.calculatedTagDigest = calculatedTagDigest;
    }
}
