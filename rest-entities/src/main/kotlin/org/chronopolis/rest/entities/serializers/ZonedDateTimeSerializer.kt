package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

/**
 * Simple serializer for writing [ZonedDateTime] as an ISO-8601 string at UTC-0
 *
 * @author shake
 */
@Deprecated("already exists")
class ZonedDateTimeSerializer : JsonSerializer<ZonedDateTime>() {
    override fun serialize(value: ZonedDateTime,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeString(ISO_OFFSET_DATE_TIME.withZone(UTC).format(value))
    }
}