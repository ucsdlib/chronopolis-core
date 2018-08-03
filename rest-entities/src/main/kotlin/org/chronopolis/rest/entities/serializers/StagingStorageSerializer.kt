package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.models.StagingStorage
import org.chronopolis.rest.entities.storage.StagingStorage as StagingStorageEntity

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