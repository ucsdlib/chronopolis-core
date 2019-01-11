package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.repair.Repair as RepairEntity

/**
 * Serializer to return a [Repair] model from an [RepairEntity] entity
 *
 * @author shake
 */
class RepairSerializer : JsonSerializer<RepairEntity>() {
    override fun serialize(repair: RepairEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(repair.model())
    }
}