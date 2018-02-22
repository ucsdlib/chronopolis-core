package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.models.Replication;

import java.io.IOException;

/**
 * Jackson serializer for Replication entity -> Replication model
 * <p>
 * Created by shake on 12/16/16.
 */
public class ReplicationSerializer extends JsonSerializer<org.chronopolis.rest.entities.Replication> {
    @Override
    public void serialize(org.chronopolis.rest.entities.Replication replication,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        Replication model = new Replication();
        model.setId(replication.getId())
                .setBag(BagSerializer.fromEntity(replication.getBag()))
                .setBagLink(replication.getBagLink())
                .setCreatedAt(replication.getCreatedAt())
                .setUpdatedAt(replication.getUpdatedAt())
                .setNode(replication.getNode().getUsername())
                .setProtocol(replication.getProtocol())
                .setReceivedTagFixity(replication.getReceivedTagFixity())
                .setReceivedTokenFixity(replication.getReceivedTokenFixity())
                .setStatus(replication.getStatus())
                .setTokenLink(replication.getTokenLink());
        jsonGenerator.writeObject(model);
    }
}
