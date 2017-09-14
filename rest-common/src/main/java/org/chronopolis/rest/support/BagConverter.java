package org.chronopolis.rest.support;

import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.storage.Fixity;
import org.chronopolis.rest.models.storage.StagingStorageModel;

import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * Created by shake on 8/1/16.
 */
public class BagConverter {

    public static org.chronopolis.rest.models.Bag toBagModel(org.chronopolis.rest.entities.Bag be) {
        org.chronopolis.rest.models.Bag bm = new Bag();
        bm.setCreatedAt(be.getCreatedAt())
                .setBagStorage(toStorageModel(be.getBagStorage()))
                .setTokenStorage(toStorageModel(be.getTokenStorage()))
                .setId(be.getId())
                .setSize(be.getSize())
                .setTotalFiles(be.getTotalFiles())
                .setUpdatedAt(be.getUpdatedAt())
                .setCreator(be.getCreator())
                .setDepositor(be.getDepositor())
                .setName(be.getName())
                .setReplicatingNodes(be.getReplicatingNodes())
                .setRequiredReplications(be.getRequiredReplications())
                .setStatus(be.getStatus());
        return bm;
    }

    // todo Should we remove this??
    public static org.chronopolis.rest.entities.Bag toBagEntity(org.chronopolis.rest.models.Bag bm) {
        org.chronopolis.rest.entities.Bag be = new org.chronopolis.rest.entities.Bag(bm.getName(), bm.getDepositor());
        be.setCreator(bm.getCreator());
        be.setStatus(bm.getStatus());
        return be;
    }

    private static StagingStorageModel toStorageModel(StagingStorage storage) {
        if (storage == null) {
            return null;
        }

        return new StagingStorageModel().setTotalFiles(storage.getTotalFiles())
                .setSize(storage.getSize())
                .setRegion(storage.getRegion().getId())
                .setPath(storage.getPath())
                .setActive(storage.isActive())
                .setFixities(toFixityModel(storage.getFixities()));
    }

    private static Set<Fixity> toFixityModel(Set<org.chronopolis.rest.entities.storage.Fixity> fixities) {
        return fixities.stream()
                .map(fixity -> new Fixity(fixity.getAlgorithm(), fixity.getValue(), fixity.getCreatedAt()))
                .collect(Collectors.toSet());
    }

}
