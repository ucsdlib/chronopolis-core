package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.storage.ReplicationConfig;

import java.io.IOException;

/**
 * StorageRegion serializer so we don't expose the entity
 *
 * Created by shake on 7/11/17.
 */
public class StorageRegionSerializer extends JsonSerializer<StorageRegion> {
    @Override
    public void serialize(StorageRegion entity, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        ReplicationConfig config = new ReplicationConfig();
        config.setPath(entity.getReplicationConfig().getPath());
        config.setServer(entity.getReplicationConfig().getServer());
        config.setUsername(entity.getReplicationConfig().getUsername());
        config.setRegion(entity.getId());

        org.chronopolis.rest.models.storage.StorageRegion model = new org.chronopolis.rest.models.storage.StorageRegion();
        model.setId(entity.getId());
        model.setCapacity(entity.getCapacity());
        model.setNode(entity.getNode().getUsername());
        model.setStorageType(entity.getStorageType());
        model.setReplicationConfig(config);

        gen.writeObject(model);
    }
}
