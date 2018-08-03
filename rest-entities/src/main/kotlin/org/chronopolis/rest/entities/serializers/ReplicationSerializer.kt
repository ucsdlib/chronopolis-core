package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.Replication as ReplicationEntity

/**
 * Serializer to return a [Replication] model from a [ReplicationEntity] entity
 *
 * @author shake
 */
class ReplicationSerializer : JsonSerializer<ReplicationEntity>() {
    override fun serialize(replication: ReplicationEntity,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(replication.model())
    }
}