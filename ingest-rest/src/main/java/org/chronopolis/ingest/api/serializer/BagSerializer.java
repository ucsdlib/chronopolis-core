package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.storage.Fixity;
import org.chronopolis.rest.models.storage.StagingStorageModel;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Jackson serializer for Bag entity -> Bag model
 * <p>
 * Created by shake on 12/16/16.
 */
public class BagSerializer extends JsonSerializer<org.chronopolis.rest.entities.Bag> {
    @Override
    public void serialize(org.chronopolis.rest.entities.Bag bag,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(fromEntity(bag));
    }

    /**
     * Helper in the event we have other serializers which need to create a Bag
     *
     * @param bag the db entity
     * @return the converted model, ready for use with the API
     */
    protected static Bag fromEntity(org.chronopolis.rest.entities.Bag bag) {
        Bag model = new Bag();
        model.setCreatedAt(bag.getCreatedAt())
                .setBagStorage(toStorageModel(bag.getBagStorage()))
                .setTokenStorage(toStorageModel(bag.getTokenStorage()))
                .setId(bag.getId())
                .setSize(bag.getSize())
                .setTotalFiles(bag.getTotalFiles())
                .setUpdatedAt(bag.getUpdatedAt())
                .setCreator(bag.getCreator())
                .setDepositor(bag.getDepositor().getNamespace())
                .setName(bag.getName())
                .setReplicatingNodes(bag.getReplicatingNodes())
                .setRequiredReplications(bag.getRequiredReplications())
                .setStatus(bag.getStatus());
        return model;
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
