package org.chronopolis.rest.kot.models

import org.chronopolis.rest.kot.models.enums.FulfillmentType

/**
 * The types of Fulfillments possible
 *
 * Note: We might need some more fancy boy modeling school annotations in order to have polymorphic
 * serialization and deserialization
 *
 */
sealed class FulfillmentStrategy

data class AceStrategy(val fulfillmentType: FulfillmentType,
                       val apiKey: String,
                       val url: String) : FulfillmentStrategy()

data class RsyncStrategy(val fulfillmentType: FulfillmentType,
                         val link: String): FulfillmentStrategy()
