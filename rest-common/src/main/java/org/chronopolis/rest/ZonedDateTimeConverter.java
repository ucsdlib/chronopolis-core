package org.chronopolis.rest;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Simple converter for using a ZonedDate
 *
 * Created by shake on 3/21/16.
 */
@Converter
public class ZonedDateTimeConverter implements AttributeConverter<ZonedDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(ZonedDateTime dateTime) {
        if (dateTime != null) {
            return Timestamp.valueOf(dateTime.toLocalDateTime());
        }

        return null;
    }

    @Override
    public ZonedDateTime convertToEntityAttribute(Timestamp timestamp) {
        if (timestamp != null) {
            return timestamp.toLocalDateTime().atZone(ZoneOffset.UTC);
        }

        return null;
    }
}
