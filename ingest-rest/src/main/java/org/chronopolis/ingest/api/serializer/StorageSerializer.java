package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.storage.Storage;

import java.io.IOException;

/**
 * Serializer so we don't expose the Storage entity
 *
 * Created by shake on 7/11/17.
 */
public class StorageSerializer extends JsonSerializer<Storage> {
    @Override
    public void serialize(Storage entity, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        org.chronopolis.rest.models.storage.Storage model = new org.chronopolis.rest.models.storage.Storage();
        model.setActive(entity.isActive());
        model.setChecksum(entity.getChecksum());
        model.setPath(entity.getPath());
        model.setRegion(entity.getRegion().getId());
        model.setSize(entity.getSize());
        model.setTotalFiles(entity.getTotalFiles());
        gen.writeObject(model);
    }
}
