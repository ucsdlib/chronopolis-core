package org.chronopolis.rest.kot.models

import org.chronopolis.rest.kot.models.enums.FulfillmentType

/**
 * The types of Fulfillments possible
 *
 * Note: We might need some more fancy boy modeling school annotations in order to have polymorphic
 * serialization and deserialization
 *
 */
sealed class FulfillmentStrategy(val type: FulfillmentType)

data class AceStrategy(val aceType: FulfillmentType,
                       val apiKey: String,
                       val url: String) : FulfillmentStrategy(aceType)

data class RsyncStrategy(val rsyncType: FulfillmentType,
                         val link: String): FulfillmentStrategy(rsyncType)
