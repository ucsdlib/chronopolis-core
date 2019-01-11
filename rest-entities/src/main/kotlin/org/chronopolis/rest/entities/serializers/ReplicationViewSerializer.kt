package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.projections.ReplicationView
import org.chronopolis.rest.models.Replication

class ReplicationViewSerializer : JsonSerializer<ReplicationView>() {
    override fun serialize(value: ReplicationView,
                           gen: JsonGenerator,
                           serializers: SerializerProvider) {
        gen.writeObject(value.toModel())
    }

}

private fun ReplicationView.toModel() : Replication {
    return Replication(
            id, createdAt, updatedAt, status, bagLink, tokenLink, protocol,
            receivedTagFixity ?: "",  // these should probably just be nullable
            receivedTokenFixity ?: "",
            node,
            bag.toModel()
    )
}
