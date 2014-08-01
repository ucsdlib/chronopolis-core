package org.chronopolis.intake.model;

/**
 * A snapshot request from Duracloud
 *
 * Created by shake on 3/6/14.
 */
public class DuracloudRequest {
    private String snapshotID;
    private String collectionName;
    private String depositor;

    public String getSnapshotID() {
        return snapshotID;
    }

    public void setSnapshotID(String snapshotID) {
        this.snapshotID = snapshotID;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(String depositor) {
        this.depositor = depositor;
    }
}
