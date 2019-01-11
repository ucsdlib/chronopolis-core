package org.chronopolis.rest.entities

import org.chronopolis.rest.entities.converters.ZonedDateTimeConverter
import java.time.ZonedDateTime
import javax.persistence.Convert
import javax.persistence.MappedSuperclass
import javax.persistence.PreUpdate

@MappedSuperclass
open class UpdatableEntity(
        @Convert(converter = ZonedDateTimeConverter::class)
        var createdAt: ZonedDateTime = ZonedDateTime.now(),

        @Convert(converter = ZonedDateTimeConverter::class)
        var updatedAt: ZonedDateTime = ZonedDateTime.now()
) : PersistableEntity() {

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}

