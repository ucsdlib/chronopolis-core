package org.chronopolis.rest.support;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;

import java.lang.reflect.Type;
import java.util.List;


public class PageDeserializer implements JsonDeserializer<PageImpl> {
    private final Logger log = LoggerFactory.getLogger(PageDeserializer.class);

    Type contentType;

    public PageDeserializer(Type contentType) {
        this.contentType = contentType;
    }

    /**
     * Deserialize a Page object with the minimal amount needed for us to operate on it
     *
     */
    public PageImpl deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = jsonElement.getAsJsonObject();
        JsonArray content = obj.getAsJsonArray("content");
        List contentList = context.deserialize(content, contentType);

        PageImpl page = new PageImpl(contentList);

        return page;
    }

}
