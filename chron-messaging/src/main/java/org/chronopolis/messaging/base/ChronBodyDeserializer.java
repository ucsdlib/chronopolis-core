package org.chronopolis.messaging.base;

import org.chronopolis.messaging.MessageType;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

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

        type = node.get("type").asText();
        JsonNode jsonBody = node.get("body");
        Iterator<String> fieldNames = jsonBody.getFieldNames();
        while ( fieldNames.hasNext() ) {
            String key = fieldNames.next();
            Object val = jsonBody.get(key);
            body.put(key, val);
        }

        MessageType messageType = MessageType.decode(type);
        chronBody = new ChronBody(messageType);
        chronBody.setBody(body);

        return chronBody;
    }
}
