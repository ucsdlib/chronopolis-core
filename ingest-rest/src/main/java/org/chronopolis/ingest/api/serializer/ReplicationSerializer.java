package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.support.ReplicationConverter;

import java.io.IOException;

/**
 * Jackson serializer for Replication entity -> Replication model
 *
 * Created by shake on 12/16/16.
 */
public class ReplicationSerializer extends JsonSerializer<Replication> {
    @Override
    public void serialize(Replication replication,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        ReplicationConverter converter = new ReplicationConverter();
        jsonGenerator.writeObject(converter.toReplicationModel(replication));
    }
}
