package org.chronopolis.rest.kot.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.kot.models.StagingStorage
import org.chronopolis.rest.kot.entities.storage.StagingStorage as StagingStorageEntity

/**
 * Serializer to return a [StagingStorage] model from a [StagingStorageEntity]
 *
 * @author shake
 */
class StagingStorageSerializer : JsonSerializer<StagingStorageEntity>() {
    override fun serialize(stagingStorage: StagingStorageEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(stagingStorage.model())
    }
}