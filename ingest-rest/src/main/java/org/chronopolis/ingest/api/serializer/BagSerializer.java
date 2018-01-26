package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.support.BagConverter;

import java.io.IOException;

/**
 * Jackson serializer for Bag entity -> Bag model
 *
 * Created by shake on 12/16/16.
 */
public class BagSerializer extends JsonSerializer<Bag> {
    @Override
    public void serialize(Bag bag, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(BagConverter.toBagModel(bag));
    }
}
