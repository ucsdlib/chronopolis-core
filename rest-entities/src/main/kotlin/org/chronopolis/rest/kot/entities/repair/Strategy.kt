package org.chronopolis.rest.kot.entities.repair

import org.chronopolis.rest.kot.entities.PersistableEntity
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.Inheritance
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Inheritance
@DiscriminatorColumn(name = "TYPE")
@Table(name = "strategy")
abstract class Strategy(
        @get:OneToOne(mappedBy = "strategy")
        var repair: Repair = Repair()
) : PersistableEntity()