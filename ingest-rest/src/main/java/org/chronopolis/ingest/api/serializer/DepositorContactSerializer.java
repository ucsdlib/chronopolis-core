package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.models.DepositorContactModel;

import java.io.IOException;

/**
 * Serialize a DepositorContact entity to DepositorContactModel
 *
 * @author shake
 */
public class DepositorContactSerializer extends JsonSerializer<DepositorContact> {
    @Override
    public void serialize(DepositorContact contact,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        DepositorContactModel model = new DepositorContactModel(contact.getContactName(),
                contact.getContactEmail(),
                contact.getContactPhone());
        gen.writeObject(model);
    }
}
