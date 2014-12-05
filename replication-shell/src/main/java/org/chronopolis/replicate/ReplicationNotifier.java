package org.chronopolis.replicate;

import org.chronopolis.common.digest.Digest;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.rest.models.Replication;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by shake on 10/29/14.
 */
public class ReplicationNotifier implements Notifier {

    private final CollectionInitMessage message;
    private boolean success;
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
        this.success = true;
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
        empty.setDepositor("");
        empty.setCollection("");
        empty.setProtocol("");
        empty.setAuditPeriod(-1);
        empty.setFixityAlgorithm(Digest.SHA_256);
        this.message = empty;
        this.origin = "restful interface";
        this.messageText = replication.toString();
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
