package org.chronopolis.rest.kot.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.kot.models.AceToken
import org.chronopolis.rest.kot.entities.AceToken as AceTokenEntity

/**
 * Serializer to return an [AceToken] model from an [AceTokenEntity] entity
 *
 * Uses the UTC-0 zone for the create date's ZonedDateTime
 *
 * @author shake
 */
class AceTokenSerializer : JsonSerializer<AceTokenEntity>() {
    override fun serialize(token: AceTokenEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(token.model())
    }
}