package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.depositor.DepositorContact as DepositorContactEntity

/**
 * Serializer to return a [DepositorContact] model from an [DepositorContactEntity] entity
 *
 * @author shake
 */
class DepositorContactSerializer : JsonSerializer<DepositorContactEntity>() {
    override fun serialize(contact: DepositorContactEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(contact.model())
    }
}