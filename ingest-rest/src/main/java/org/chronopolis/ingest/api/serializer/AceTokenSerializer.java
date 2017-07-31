package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.models.AceTokenModel;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Serializer to return an {@link AceTokenModel} from an {@link AceToken} entity
 * Use a UTC zone for the create ZonedDateTime
 *
 * @author shake
 */
public class AceTokenSerializer extends JsonSerializer<AceToken> {
    @Override
    public void serialize(AceToken value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        AceTokenModel model = new AceTokenModel();
        model.setId(value.getId())
             .setBagId(value.getBag().getId())
             .setRound(value.getRound())
             .setProof(value.getProof())
             .setAlgorithm(value.getAlgorithm())
             .setFilename(value.getFilename())
             .setImsService(value.getImsService())
             .setCreateDate(ZonedDateTime.ofInstant(value.getCreateDate().toInstant(), ZoneOffset.UTC));
        gen.writeObject(model);
    }
}
