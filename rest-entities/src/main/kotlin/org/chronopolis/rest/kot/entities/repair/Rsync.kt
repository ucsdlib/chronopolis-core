package org.chronopolis.rest.kot.entities.repair

import org.chronopolis.rest.kot.models.FulfillmentStrategy
import org.chronopolis.rest.kot.models.RsyncStrategy
import org.chronopolis.rest.kot.models.enums.FulfillmentType
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("RSYNC")
class Rsync(var link: String) : Strategy() {
    override fun model(): FulfillmentStrategy {
        return RsyncStrategy(FulfillmentType.NODE_TO_NODE, link)
    }
}