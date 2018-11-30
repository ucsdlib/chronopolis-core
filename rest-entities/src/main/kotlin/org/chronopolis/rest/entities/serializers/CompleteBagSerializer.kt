package org.chronopolis.rest.entities.serializers

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import org.chronopolis.rest.entities.projections.CompleteBag
import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.StagingStorage


class CompleteBagSerializer : JsonSerializer<CompleteBag>() {
    override fun serialize(bag: CompleteBag, gen: JsonGenerator, privider: SerializerProvider) {
        gen.writeObject(bag.toModel())
    }

}

fun CompleteBag.toModel(): Bag {
    var bag: StagingStorage? = null
    var token: StagingStorage? = null

    storage.forEach {
        if (it.type == "BAG") {
            bag = StagingStorage(it.active, it.totalFiles, it.region, it.totalFiles, it.path, setOf())
        } else {
            token = StagingStorage(it.active, it.totalFiles, it.region, it.totalFiles, it.path, setOf())
        }
    }

    return Bag(
            id = id,
            size = size,
            name = name,
            status = status,
            creator = creator,
            depositor = depositor,
            createdAt = createdAt,
            updatedAt = updatedAt,
            totalFiles = totalFiles,
            replicatingNodes = replicatingNodes,
            // ... lol
            bagStorage = bag,
            tokenStorage = token
    )
}