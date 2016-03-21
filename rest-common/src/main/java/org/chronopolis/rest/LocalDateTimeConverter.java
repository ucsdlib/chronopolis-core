package org.chronopolis.rest;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Simple converter for using a LocalDate
 *
 * Created by shake on 3/21/16.
 */
@Converter
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

    @Override
    public Timestamp convertToDatabaseColumn(LocalDateTime dateTime) {
        if (dateTime != null) {
            return Timestamp.valueOf(dateTime);
        }

        return null;
    }

    @Override
    public LocalDateTime convertToEntityAttribute(Timestamp timestamp) {
        if (timestamp != null) {
            return timestamp.toLocalDateTime();
        }

        return null;
    }
}
