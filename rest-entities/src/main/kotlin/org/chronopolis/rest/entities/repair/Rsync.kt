package org.chronopolis.rest.entities.repair

import org.chronopolis.rest.models.FulfillmentStrategy
import org.chronopolis.rest.models.RsyncStrategy
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("RSYNC")
class Rsync(var link: String = "") : Strategy() {
    override fun model(): FulfillmentStrategy {
        return RsyncStrategy(link)
    }
}