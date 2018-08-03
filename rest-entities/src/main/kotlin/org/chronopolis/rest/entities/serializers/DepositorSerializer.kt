package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.depositor.Depositor as DepositorEntity

/**
 * Serializer to return a [Depositor] model from an [DepositorEntity] entity
 *
 * @author shake
 */
class DepositorSerializer : JsonSerializer<DepositorEntity>() {
    override fun serialize(depositor: DepositorEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(depositor.model())
    }
}