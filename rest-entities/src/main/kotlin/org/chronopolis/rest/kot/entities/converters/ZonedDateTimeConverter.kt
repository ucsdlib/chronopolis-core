package org.chronopolis.rest.kot.entities.converters

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter
class ZonedDateTimeConverter : AttributeConverter<ZonedDateTime, Timestamp> {

    override fun convertToDatabaseColumn(dateTime: ZonedDateTime?): Timestamp {
        if (dateTime != null) {
            return Timestamp.valueOf(dateTime.toLocalDateTime())
        }

        // not sure if this is a good idea or not
        return Timestamp.valueOf(LocalDateTime.now())
    }

    override fun convertToEntityAttribute(timestamp: Timestamp?): ZonedDateTime {
        if (timestamp != null) {
            return timestamp.toLocalDateTime().atZone(ZoneOffset.UTC)
        }

        return ZonedDateTime.now()
    }
}