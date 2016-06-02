package org.chronopolis.rest.support;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * ZonedDateTime serializer into an ISO-8601 string (YYYY-MM-ddTHH:mm:ssZ)
 *
 * Created by shake on 3/22/16.
 */
public class ZonedDateTimeSerializer implements JsonSerializer<ZonedDateTime> {
    @Override
    public JsonElement serialize(ZonedDateTime zonedDateTime, Type type, JsonSerializationContext jsonSerializationContext) {
        DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
        return new JsonPrimitive(fmt.format(zonedDateTime.truncatedTo(ChronoUnit.SECONDS)));
    }
}
