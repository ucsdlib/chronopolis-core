package org.chronopolis.rest.models.serializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.chronopolis.rest.models.AceStrategy
import org.chronopolis.rest.models.FulfillmentStrategy
import org.chronopolis.rest.models.RsyncStrategy
import org.chronopolis.rest.models.enums.FulfillmentType

/**
 * Simple deserializer for [FulfillmentStrategy]. Reads the type and delegates to the sealed class.
 *
 * @author shake
 */
class FulfillmentStrategyDeserializer : JsonDeserializer<FulfillmentStrategy>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FulfillmentStrategy {
        val node: JsonNode = p.codec.readTree(p)
        val type = node.get("type").asText()
        return when (FulfillmentType.valueOf(type)) {
            FulfillmentType.ACE -> AceStrategy(node.get("apiKey").asText(),
                    node.get("url").asText())
            FulfillmentType.NODE_TO_NODE -> RsyncStrategy(node.get("link").asText())
            FulfillmentType.INGEST -> RsyncStrategy(node.get("link").asText())
        }
    }
}