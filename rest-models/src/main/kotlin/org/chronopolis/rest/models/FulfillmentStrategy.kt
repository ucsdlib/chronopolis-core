package org.chronopolis.rest.models

import org.chronopolis.rest.models.enums.FulfillmentType

/**
 * The types of Fulfillments possible
 *
 * Note: We might need some more fancy boy modeling school annotations in order to have polymorphic
 * serialization and deserialization
 *
 */
sealed class FulfillmentStrategy(val type: FulfillmentType)

data class AceStrategy(val apiKey: String,
                       val url: String) : FulfillmentStrategy(FulfillmentType.ACE)

data class RsyncStrategy(val link: String): FulfillmentStrategy(FulfillmentType.NODE_TO_NODE)
