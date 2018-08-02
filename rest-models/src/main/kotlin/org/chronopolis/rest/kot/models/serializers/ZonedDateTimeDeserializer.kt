package org.chronopolis.rest.kot.models.serializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME

class ZonedDateTimeDeserializer : JsonDeserializer<ZonedDateTime>() {

    override fun deserialize(jsonParser: JsonParser,
                             deserializationContext: DeserializationContext): ZonedDateTime {
        val text = jsonParser.text
        return ZonedDateTime.from(ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC).parse(text))
    }
}

