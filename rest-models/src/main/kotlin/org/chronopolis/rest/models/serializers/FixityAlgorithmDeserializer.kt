package org.chronopolis.rest.models.serializers

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.chronopolis.rest.models.enums.FixityAlgorithm

/**
 * Deserializer for a [FixityAlgorithm]
 *
 * @author shake
 */
class FixityAlgorithmDeserializer : JsonDeserializer<FixityAlgorithm>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): FixityAlgorithm {
        return when (parser.text.toLowerCase()) {
            "sha256", "sha-256", "sha_256" -> FixityAlgorithm.SHA_256
            else -> FixityAlgorithm.UNSUPPORTED
        }
    }
}