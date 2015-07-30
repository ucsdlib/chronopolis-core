package org.chronopolis.common.dpn;

import org.joda.time.DateTime;

/**
 * Created by shake on 7/29/15.
 */
public class DPNReplication {

    String replicationId;
    String fromNode;
    String toNode;
    String uuid;
    String fixityAlgorithm;
    String fixityValue;
    String fixityNonce;
    boolean fixityAccept;
    boolean bagValid;
    String status;
    String protocol;
    String link;
    DateTime createdAt;
    DateTime updatedAt;

}
