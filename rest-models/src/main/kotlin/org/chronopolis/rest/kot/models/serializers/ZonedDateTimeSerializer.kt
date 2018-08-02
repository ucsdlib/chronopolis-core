package org.chronopolis.rest.kot.models.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

class ZonedDateTimeSerializer : JsonSerializer<ZonedDateTime>() {
    override fun serialize(dateTime: ZonedDateTime,
                           jsonGenerator: JsonGenerator,
                           serializerProvider: SerializerProvider) {
        jsonGenerator.writeString(ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC).format(dateTime))
    }
}

