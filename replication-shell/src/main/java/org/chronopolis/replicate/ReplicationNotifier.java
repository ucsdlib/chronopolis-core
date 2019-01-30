package org.chronopolis.replicate;

import org.chronopolis.rest.models.Replication;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * TODO: IMO this is kind of shitty and it would be nice to get rid of it, or at least change how we use it
 *
 * Created by shake on 10/29/14.
 */
public class ReplicationNotifier implements Notifier {

    private boolean success = true;
    private String aceStep;
    private String bagStep;
    private String tokenStep;
    private String rsyncStats;
    private final String origin;
    private String messageText;

    private String calculatedTagDigest;
    private String calculatedTokenDigest;
    private final String collection;

    public ReplicationNotifier(Replication replication) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.origin = "restful interface";
        this.collection = replication.getBag().getName();

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

    public String getCollection() {
        return collection;
    }
}
