package org.chronopolis.rest.entities.repair

import org.chronopolis.rest.models.AceStrategy
import org.chronopolis.rest.models.FulfillmentStrategy
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("ACE")
class Ace(var apiKey: String = "", var url: String = "") : Strategy() {
    override fun model(): FulfillmentStrategy {
        return AceStrategy(apiKey, url)
    }
}