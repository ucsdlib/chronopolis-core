package org.chronopolis.rest.kot.entities.listeners

import org.chronopolis.rest.kot.entities.UpdatableEntity
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.persistence.PrePersist
import javax.persistence.PreUpdate

/**
 * EntityListener which updates the created_at on the initial save,
 * and updated_at on all others
 *
 * @author shake
 */
class UpdatableEntityListener {
    @PrePersist
    fun createTimeStamps(ue: UpdatableEntity) {
        println("HEY A PREPERSIST HERE")
        ue.createdAt = ZonedDateTime.now(ZoneOffset.UTC)
        ue.updatedAt = ZonedDateTime.now(ZoneOffset.UTC)
    }

    @PreUpdate
    fun updateTimeStamps(ue: UpdatableEntity) {
        println("HEY A UPDATING HERE")
        ue.updatedAt = ZonedDateTime.now(ZoneOffset.UTC)
    }
}