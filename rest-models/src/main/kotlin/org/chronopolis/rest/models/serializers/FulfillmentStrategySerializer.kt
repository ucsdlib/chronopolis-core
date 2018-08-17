package org.chronopolis.rest.models.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.models.AceStrategy
import org.chronopolis.rest.models.FulfillmentStrategy
import org.chronopolis.rest.models.RsyncStrategy

/**
 * Simple serializer for a [FulfillmentStrategy] so that all json is written
 *
 * @author shake
 */
class FulfillmentStrategySerializer : JsonSerializer<FulfillmentStrategy>() {
    override fun serialize(value: FulfillmentStrategy,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeStringField("type", value.type.toString())
        when (value) {
            is AceStrategy -> {
                gen.writeStringField("apiKey", value.apiKey)
                gen.writeStringField("url", value.url)
            }
            is RsyncStrategy -> gen.writeStringField("link", value.link)
        }
        gen.writeEndObject()
    }
}