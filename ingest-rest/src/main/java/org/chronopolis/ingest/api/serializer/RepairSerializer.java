package org.chronopolis.ingest.api.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.entities.RepairFile;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * JsonSerializer for writing our Repair model from our api
 *
 * Created by shake on 1/25/17.
 */
public class RepairSerializer extends JsonSerializer<Repair> {
    @Override
    public void serialize(Repair repair, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
        org.chronopolis.rest.models.repair.Repair model = new org.chronopolis.rest.models.repair.Repair();
        model.setId(repair.getId());
        model.setCollection(repair.getBag().getName());
        model.setDepositor(repair.getBag().getDepositor());
        model.setFiles(repair.getFiles().stream().map(RepairFile::getPath).collect(Collectors.toList()));
        model.setRequester(repair.getRequester());
        model.setStatus(repair.getStatus());
        model.setCreatedAt(repair.getCreatedAt());
        model.setUpdatedAt(repair.getUpdatedAt());
        jsonGenerator.writeObject(model);
    }
}
