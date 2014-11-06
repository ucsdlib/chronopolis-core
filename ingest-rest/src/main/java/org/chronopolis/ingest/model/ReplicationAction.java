package org.chronopolis.ingest.model;

/**
 * Created by shake on 11/5/14.
 */
public class ReplicationAction {

    String actionID;
    String replicatingNode;
    String bagID;
    String expectedTagFixity;
    String expectedTokenFixity;
    String error;
    String receivedTagFixity;
    String receivedTokenFixtiy;
    ReplicationStatus status;

}
