package org.chronopolis.rest.support;

import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.storage.Fixity;
import org.chronopolis.rest.models.storage.StagingStorageModel;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by shake on 8/1/16.
 */
@Deprecated
public class BagConverter {

    /**
     * Convert a BagEntity to a BagModel
     *
     * @param be the BagEntity
     * @return a BagModel
     */
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
                .setDepositor(be.getDepositor().getNamespace())
                .setName(be.getName())
                .setReplicatingNodes(be.getReplicatingNodes())
                .setRequiredReplications(be.getRequiredReplications())
                .setStatus(be.getStatus());
        return bm;
    }

    private static StagingStorageModel toStorageModel(Set<StagingStorage> storage) {
        if (storage == null || storage.isEmpty()) {
            return null;
        }

        // this is normally not fetched... could cause issues... need to test from the api
        return storage.stream()
                .filter(StagingStorage::isActive)
                .findFirst()
                .map(ss -> new StagingStorageModel().setTotalFiles(ss.getTotalFiles())
                        .setSize(ss.getSize())
                        .setRegion(ss.getRegion().getId())
                        .setActive(ss.isActive())
                        .setPath(ss.getPath())
                        // the null kind of negates the safety we get from the Optional... but it's ignored by the serializer anyway
                        .setFixities(toFixityModel(ss.getFixities()))).orElse(null);
    }

    private static Set<Fixity> toFixityModel(Set<org.chronopolis.rest.entities.storage.Fixity> fixities) {
        return fixities.stream()
                .map(fixity -> new Fixity(fixity.getAlgorithm(), fixity.getValue(), fixity.getCreatedAt()))
                .collect(Collectors.toSet());
    }

}
