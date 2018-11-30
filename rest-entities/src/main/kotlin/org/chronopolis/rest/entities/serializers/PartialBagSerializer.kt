package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.projections.PartialBag
import org.chronopolis.rest.models.Bag

class PartialBagSerializer : JsonSerializer<PartialBag>() {
    override fun serialize(part: PartialBag, gen: JsonGenerator, provider: SerializerProvider?) {
        gen.writeObject(part.toModel())
    }
}

private fun PartialBag.toModel(): Bag {
    return Bag(
            id,
            size,
            totalFiles,
            null,
            null,
            createdAt,
            createdAt,
            name,
            creator,
            depositor,
            status,
            replicatingNodes
    )
}
