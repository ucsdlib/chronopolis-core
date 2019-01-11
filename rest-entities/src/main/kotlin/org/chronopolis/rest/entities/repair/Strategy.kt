package org.chronopolis.rest.entities.repair

import org.chronopolis.rest.entities.PersistableEntity
import org.chronopolis.rest.models.FulfillmentStrategy
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.Inheritance
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Inheritance
@Table(name = "strategy")
@DiscriminatorColumn(name = "TYPE")
abstract class Strategy(
        @OneToOne(mappedBy = "strategy")
        var repair: Repair = Repair()
) : PersistableEntity() {
        abstract fun model(): FulfillmentStrategy
}