package org.chronopolis.rest.kot.models.serializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import org.chronopolis.rest.kot.models.AceStrategy
import org.chronopolis.rest.kot.models.FulfillmentStrategy
import org.chronopolis.rest.kot.models.RsyncStrategy
import org.chronopolis.rest.kot.models.enums.FulfillmentType

class FulfillmentStrategyDeserializer : JsonDeserializer<FulfillmentStrategy>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FulfillmentStrategy {
        val node: JsonNode = p.codec.readTree(p)
        val type = node.get("type").asText()
        when (FulfillmentType.valueOf(type)) {
            FulfillmentType.ACE -> return AceStrategy(FulfillmentType.ACE,
                    node.get("apiKey").asText(),
                    node.get("url").asText())
            FulfillmentType.NODE_TO_NODE -> return RsyncStrategy(FulfillmentType.NODE_TO_NODE,
                    node.get("link").asText())
            FulfillmentType.INGEST -> return RsyncStrategy(FulfillmentType.INGEST,
                    node.get("link").asText())
        }
    }
}