package org.chronopolis.rest.support;

import org.chronopolis.rest.entities.Replication;

/**
 * Utils for replication model <-> replication entity relationship
 *
 * Created by shake on 12/16/16.
 */
public class ReplicationConverter {

    public org.chronopolis.rest.models.Replication toReplicationModel(Replication replication) {
        org.chronopolis.rest.models.Replication model = new org.chronopolis.rest.models.Replication();
        model.setId(replication.getId())
                .setBag(BagConverter.toBagModel(replication.getBag()))
                .setBagLink(replication.getBagLink())
                .setCreatedAt(replication.getCreatedAt())
                .setUpdatedAt(replication.getUpdatedAt())
                .setNode(replication.getNode().getUsername())
                .setProtocol(replication.getProtocol())
                .setReceivedTagFixity(replication.getReceivedTagFixity())
                .setReceivedTokenFixity(replication.getReceivedTokenFixity())
                .setStatus(replication.getStatus())
                .setTokenLink(replication.getTokenLink());
        return model;
    }
}
