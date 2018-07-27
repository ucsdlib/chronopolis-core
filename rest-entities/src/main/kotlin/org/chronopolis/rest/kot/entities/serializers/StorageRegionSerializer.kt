package org.chronopolis.rest.kot.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.kot.entities.storage.StorageRegion as StorageRegionEntity

/**
 * Serializer to return a [StorageRegion] model from a [StorageRegionEntity] entity
 *
 * @author shake
 */
class StorageRegionSerializer : JsonSerializer<StorageRegionEntity>() {
    override fun serialize(region: StorageRegionEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(region.model())
    }
}