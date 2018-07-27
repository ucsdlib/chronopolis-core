package org.chronopolis.rest.kot.entities.repair

import org.chronopolis.rest.kot.models.AceStrategy
import org.chronopolis.rest.kot.models.FulfillmentStrategy
import org.chronopolis.rest.kot.models.enums.FulfillmentType
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("ACE")
class Ace(var apiKey: String = "", var url: String = "") : Strategy() {
    override fun model(): FulfillmentStrategy {
        return AceStrategy(FulfillmentType.ACE, apiKey, url)
    }
}