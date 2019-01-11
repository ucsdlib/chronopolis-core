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
    val bag: StagingStorage? = storage["BAG"]?.let {
        StagingStorage(it.active, it.totalFiles, it.region, it.totalFiles, it.path, setOf())
    }
    val token: StagingStorage? = storage["TOKEN_STORE"]?.let {
        StagingStorage(it.active, it.totalFiles, it.region, it.totalFiles, it.path, setOf())
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
            bagStorage = bag,
            tokenStorage = token
    )
}