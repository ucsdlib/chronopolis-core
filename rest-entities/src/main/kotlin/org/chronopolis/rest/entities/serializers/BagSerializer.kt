package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.entities.Bag as BagEntity

/**
 * Serializer to return a [Bag] model from an [BagEntity]
 *
 * @author shake
 */
class BagSerializer : JsonSerializer<BagEntity>() {
    override fun serialize(bag: BagEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(bag.model())
    }
}