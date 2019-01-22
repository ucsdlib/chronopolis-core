package org.chronopolis.rest.entities.serializers

import org.chronopolis.rest.entities.BagFile
import org.chronopolis.rest.entities.DataFile
import org.chronopolis.rest.entities.TokenStore
import org.chronopolis.rest.entities.repair.Ace
import org.chronopolis.rest.entities.repair.Rsync
import org.chronopolis.rest.entities.repair.Strategy
import org.chronopolis.rest.models.*
import org.chronopolis.rest.models.enums.FixityAlgorithm
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.chronopolis.rest.entities.AceToken as AceTokenEntity
import org.chronopolis.rest.entities.Bag as BagEntity
import org.chronopolis.rest.entities.Replication as ReplicationEntity
import org.chronopolis.rest.entities.depositor.Depositor as DepositorEntity
import org.chronopolis.rest.entities.depositor.DepositorContact as ContactEntity
import org.chronopolis.rest.entities.repair.Repair as RepairEntity
import org.chronopolis.rest.entities.storage.Fixity as FixityEntity
import org.chronopolis.rest.entities.storage.StagingStorage as StagingStorageEntity
import org.chronopolis.rest.entities.storage.StorageRegion as StorageRegionEntity

fun AceTokenEntity.model(): AceToken {
    return AceToken(
            this.id,
            this.bag.id,
            this.round,
            this.proof,
            this.imsHost,
            this.file.filename,
            this.algorithm,
            this.imsService,
            ZonedDateTime.ofInstant(this.createDate.toInstant(), ZoneOffset.UTC))
}

fun BagEntity.model(): Bag {
    return Bag(
            id = this.id,
            size = this.size,
            totalFiles = this.totalFiles,
            name = this.name,
            creator = this.creator,
            depositor = this.depositor.namespace,
            replicatingNodes = this.getReplicatingNodes(),
            status = this.status,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            bagStorage = this.storage
                    .filter {
                        when (it.file) {
                            is BagFile -> true
                            else -> false
                        }
                    }
                    .firstOrNull { it.active }
                    ?.model(),
            tokenStorage = this.storage
                    .filter {
                        when (it.file) {
                            is TokenStore -> true
                            else -> false
                        }
                    }
                    .firstOrNull { it.active }
                    ?.model()

    )
}

// todo: it's really dumb to go String -> FixityAlgorithm -> String
fun FixityEntity.model(): Fixity {
    return Fixity(this.value, FixityAlgorithm.fromString(this.algorithm), this.createdAt)
}

fun DataFile.model(): File {
    return File(
            this.id,
            this.filename,
            this.size,
            this.fixities.map { it.model() }.toSet(),
            this.bag.id,
            this.createdAt,
            this.updatedAt
    )
}

fun ContactEntity.model(): DepositorContact {
    return DepositorContact(this.contactName, this.contactEmail, this.contactPhone)
}

fun DepositorEntity.model(): Depositor {
    return Depositor(
            id = this.id,
            namespace = this.namespace,
            // hibernate is setting these to null maybe? whatever is going on, safety first
            sourceOrganization = this.sourceOrganization?: "",
            organizationAddress = this.organizationAddress?: "",
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            replicatingNodes = this.nodeDistributions.map { it.username }.toSet(),
            contacts = this.contacts.map { it.model() }.toSet()
    )
}

fun RepairEntity.model(): Repair {
    return Repair(
            id = this.id,
            to = this.to.username,
            collection = this.bag.name,
            depositor = this.bag.depositor.namespace,
            requester = this.requester,
            status = this.status,
            audit = this.audit,
            cleaned = this.cleaned,
            replaced = this.replaced,
            validated = this.validated,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            files = this.files.map { it.path },

            type = this.type,
            from = this.from?.username,
            credentials = this.strategy?.model()
    )
}

fun ReplicationEntity.model(): Replication {
    return Replication(
            id = this.id,
            bag = this.bag.model(),
            bagLink = this.bagLink,
            tokenLink = this.tokenLink,
            node = this.node.username,
            protocol = this.protocol,
            receivedTagFixity = this.receivedTagFixity ?: "",
            receivedTokenFixity = this.receivedTokenFixity ?: "",
            status = this.status,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
    )
}

fun StagingStorageEntity.model(): StagingStorage {
    return StagingStorage(
            active = this.active,
            path = this.path,
            region = this.region.id,
            size = this.size,
            totalFiles = this.totalFiles,
            fixities = this.file.fixities.map {
                Fixity(it.value, FixityAlgorithm.fromString(it.algorithm), it.createdAt)
            }.toSet()
    )
}

fun StorageRegionEntity.model(): StorageRegion {
    val storageId = this.id
    return StorageRegion(
            id = storageId,
            capacity = this.capacity,
            node = this.node.username,
            note = this.note,
            dataType = this.dataType,
            storageType = this.storageType,
            replicationConfig = this.replicationConfig.let {
                // need to test username nullability
                // I thought let would allow us to avoid elvis...
                ReplicationConfig(storageId,
                        it.path,
                        it.server,
                        it.username ?: "chronopolis")
            }
    )
}

// todo: Move to another... class
fun FulfillmentStrategy.toEntity(): Strategy = when (this) {
    is AceStrategy -> Ace(this.apiKey, this.url)
    is RsyncStrategy -> Rsync(this.link)
}
