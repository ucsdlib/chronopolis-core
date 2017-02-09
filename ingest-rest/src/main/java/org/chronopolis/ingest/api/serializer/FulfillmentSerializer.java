package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.Fulfillment;

import java.io.IOException;

/**
 *
 * Created by shake on 1/27/17.
 */
public class FulfillmentSerializer extends JsonSerializer<Fulfillment> {
    @Override
    public void serialize(Fulfillment fulfillment, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        org.chronopolis.rest.models.repair.Fulfillment model = new org.chronopolis.rest.models.repair.Fulfillment();
        model.setId(fulfillment.getId());
        model.setStatus(fulfillment.getStatus());
        model.setCreatedAt(fulfillment.getCreatedAt());
        model.setUpdatedAt(fulfillment.getUpdatedAt());
        model.setRepair(fulfillment.getRepair().getId());
        model.setFrom(fulfillment.getFrom().getUsername());
        model.setTo(fulfillment.getRepair().getTo().getUsername());
        if (fulfillment.getStrategy() != null) {
            model.setType(fulfillment.getType()); // push to FulfillmentStrategy?
            model.setCredentials(fulfillment.getStrategy().createModel());
        }
        jsonGenerator.writeObject(model);
    }

}
