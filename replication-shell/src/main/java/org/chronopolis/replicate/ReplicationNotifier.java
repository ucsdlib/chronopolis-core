package org.chronopolis.replicate;

import org.chronopolis.messaging.collection.CollectionInitMessage;

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

    public ReplicationNotifier(CollectionInitMessage message) {
        this.message = message;
        this.success = true;
    }

    @Override
    public String getNotificationBody() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter textBody = new PrintWriter(stringWriter, true);
        textBody.println("Message received from: " + message.getOrigin());
        textBody.println(message.toString());
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

}
