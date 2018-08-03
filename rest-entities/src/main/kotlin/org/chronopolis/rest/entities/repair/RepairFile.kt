package org.chronopolis.rest.entities.repair

import org.chronopolis.rest.entities.PersistableEntity
import javax.persistence.Entity
import javax.persistence.ManyToOne

@Entity
class RepairFile(
        @ManyToOne
        var repair: Repair = Repair(),

        var path: String = ""
) : PersistableEntity()