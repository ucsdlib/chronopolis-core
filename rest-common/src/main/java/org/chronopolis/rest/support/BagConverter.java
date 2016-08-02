package org.chronopolis.rest.support;

import org.chronopolis.rest.models.Bag;

/**
 *
 * Created by shake on 8/1/16.
 */
public class BagConverter {

    public static org.chronopolis.rest.models.Bag toBagModel(org.chronopolis.rest.entities.Bag be) {
        org.chronopolis.rest.models.Bag bm = new Bag();
        bm.setCreatedAt(be.getCreatedAt())
                .setId(be.getId())
                .setUpdatedAt(be.getUpdatedAt())
                .setCreator(be.getCreator())
                .setFixityAlgorithm(be.getFixityAlgorithm())
                .setDepositor(be.getDepositor())
                .setLocation(be.getLocation())
                .setName(be.getName())
                .setReplicatingNodes(be.getReplicatingNodes())
                .setSize(be.getSize())
                .setStatus(be.getStatus())
                .setTotalFiles(be.getTotalFiles())
                .setTokenLocation(be.getTokenLocation());
        return bm;
    }

    public static org.chronopolis.rest.entities.Bag toBagEntity(org.chronopolis.rest.models.Bag bm) {
        org.chronopolis.rest.entities.Bag be = new org.chronopolis.rest.entities.Bag(bm.getName(), bm.getDepositor());
        be.setTokenLocation(bm.getTokenLocation());
        be.setCreator(bm.getCreator());
        be.setLocation(bm.getLocation());
        be.setFixityAlgorithm(bm.getFixityAlgorithm());
        be.setSize(bm.getSize());
        be.setStatus(bm.getStatus());
        be.setTotalFiles(bm.getTotalFiles());
        return be;
    }

}
