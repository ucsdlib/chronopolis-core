package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.models.DepositorContactModel;
import org.chronopolis.rest.models.DepositorModel;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Create a DepositorModel from a Depositor entity
 *
 * @author shake
 */
public class DepositorSerializer extends JsonSerializer<Depositor> {
    @Override
    public void serialize(Depositor depositor, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        Set<DepositorContactModel> contacts = depositor.getContacts().stream()
                .map(contact -> new DepositorContactModel(contact.getContactName(),
                        contact.getContactEmail(),
                        contact.getContactPhone()))
                .collect(Collectors.toSet());
        DepositorModel model = new DepositorModel(depositor.getId(),
                depositor.getNamespace(),
                depositor.getSourceOrganization(),
                depositor.getOrganizationAddress(),
                depositor.getCreatedAt(),
                depositor.getUpdatedAt(),
                contacts);
        gen.writeObject(model);
    }
}
