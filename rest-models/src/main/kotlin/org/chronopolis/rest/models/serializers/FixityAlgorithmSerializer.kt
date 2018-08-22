package org.chronopolis.rest.models.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.models.enums.FixityAlgorithm

/**
 * Serializer for a [FixityAlgorithm]
 *
 * @author shake
 */
class FixityAlgorithmSerializer : JsonSerializer<FixityAlgorithm>() {
    override fun serialize(value: FixityAlgorithm,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeString(value.canonical)
    }
}

