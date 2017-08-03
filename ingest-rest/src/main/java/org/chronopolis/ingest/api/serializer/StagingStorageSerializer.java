package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.storage.Fixity;
import org.chronopolis.rest.models.storage.StagingStorageModel;

import java.io.IOException;

/**
 * Serializer so we don't expose the Storage entity
 *
 * Created by shake on 7/11/17.
 */
public class StagingStorageSerializer extends JsonSerializer<StagingStorage> {
    @Override
    public void serialize(StagingStorage entity, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        StagingStorageModel model = new StagingStorageModel();
        model.setActive(entity.isActive());
        model.setPath(entity.getPath());
        model.setRegion(entity.getRegion().getId());
        model.setSize(entity.getSize());
        model.setTotalFiles(entity.getTotalFiles());
        entity.getFixities().stream()
                .map(f -> new Fixity(f.getAlgorithm(), f.getValue(), f.getCreatedAt()))
                .forEach(model::addFixity);
        gen.writeObject(model);
    }
}
