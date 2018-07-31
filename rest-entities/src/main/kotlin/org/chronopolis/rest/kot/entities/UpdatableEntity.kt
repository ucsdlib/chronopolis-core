package org.chronopolis.rest.kot.entities

import org.chronopolis.rest.kot.entities.converters.ZonedDateTimeConverter
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

