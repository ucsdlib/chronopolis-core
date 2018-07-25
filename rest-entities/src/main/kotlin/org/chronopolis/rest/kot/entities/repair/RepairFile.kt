package org.chronopolis.rest.kot.entities.repair

import org.chronopolis.rest.kot.entities.PersistableEntity
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class RepairFile(
        @get:ManyToOne
        var repair: Repair = Repair(),

        var path: String = ""
) : PersistableEntity()