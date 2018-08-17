package org.chronopolis.rest.entities.storage

import org.chronopolis.rest.entities.PersistableEntity
import org.chronopolis.rest.entities.converters.ZonedDateTimeConverter
import java.time.ZonedDateTime
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class Fixity(
        @ManyToOne
        @JoinColumn(name = "storage_id", nullable = false)
        var storage: StagingStorage = StagingStorage(),

        @Convert(converter = ZonedDateTimeConverter::class)
        var createdAt: ZonedDateTime = ZonedDateTime.now(),

        var value: String = "",
        var algorithm: String = ""
) : PersistableEntity()