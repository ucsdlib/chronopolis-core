package org.chronopolis.messaging.base;

import org.chronopolis.messaging.MessageType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Created by shake on 2/4/14.
 */
public class ChronBodyDeserializer extends JsonDeserializer<ChronBody> {

    @Override
    public ChronBody deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        ChronBody chronBody;
        Map<String, Object> body = new HashMap<>();
        String type = null;
        JsonNode node = jp.getCodec().readTree(jp);

        // Only worry about creating the proper enum from the json, let
        // jackson handle the rest
        type = node.get("type").asText();
        JsonNode jsonBody = node.get("body");
        ObjectMapper mapper = new ObjectMapper();
        body = mapper.readValue(jsonBody, Map.class);

        MessageType messageType = MessageType.decode(type);
        chronBody = new ChronBody(messageType);
        chronBody.setBody(body);

        return chronBody;
    }
}
