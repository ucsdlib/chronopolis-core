package org.chronopolis.rest.kot.entities

import java.time.ZonedDateTime
import javax.persistence.MappedSuperclass
import javax.persistence.PreUpdate

@MappedSuperclass
open class UpdatableEntity : PersistableEntity() {
    var createdAt: ZonedDateTime = ZonedDateTime.now()
    var updatedAt: ZonedDateTime = ZonedDateTime.now()

    @PreUpdate
    protected fun onUpdate() {
        updatedAt = ZonedDateTime.now()
    }
}

