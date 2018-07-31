package org.chronopolis.rest.kot.entities

import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
open class PersistableEntity(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        var id: Long = 0
)
