package org.chronopolis.rest.kot.entities.serializers

import org.chronopolis.rest.kot.entities.repair.Ace
import org.chronopolis.rest.kot.entities.repair.Rsync
import org.chronopolis.rest.kot.entities.repair.Strategy
import org.chronopolis.rest.kot.models.AceStrategy
import org.chronopolis.rest.kot.models.AceToken
import org.chronopolis.rest.kot.models.Bag
import org.chronopolis.rest.kot.models.Depositor
import org.chronopolis.rest.kot.models.DepositorContact
import org.chronopolis.rest.kot.models.Fixity
import org.chronopolis.rest.kot.models.FulfillmentStrategy
import org.chronopolis.rest.kot.models.Repair
import org.chronopolis.rest.kot.models.Replication
import org.chronopolis.rest.kot.models.ReplicationConfig
import org.chronopolis.rest.kot.models.RsyncStrategy
import org.chronopolis.rest.kot.models.StagingStorage
import org.chronopolis.rest.kot.models.StorageRegion
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.chronopolis.rest.kot.entities.AceToken as AceTokenEntity
import org.chronopolis.rest.kot.entities.Bag as BagEntity
import org.chronopolis.rest.kot.entities.Replication as ReplicationEntity
import org.chronopolis.rest.kot.entities.depositor.Depositor as DepositorEntity
import org.chronopolis.rest.kot.entities.depositor.DepositorContact as ContactEntity
import org.chronopolis.rest.kot.entities.repair.Repair as RepairEntity
import org.chronopolis.rest.kot.entities.storage.StagingStorage as StagingStorageEntity
import org.chronopolis.rest.kot.entities.storage.StorageRegion as StorageRegionEntity

fun AceTokenEntity.model(): AceToken {
    return AceToken(
            this.id ?: -1,
            this.bag.id ?: -1,
            this.round,
            this.proof,
            this.imsHost,
            this.filename,
            this.algorithm,
            this.imsService,
            ZonedDateTime.ofInstant(this.createDate.toInstant(), ZoneOffset.UTC))
}

fun BagEntity.model(): Bag {
    return Bag(
            id = this.id ?: -1,
            size = this.size,
            totalFiles = this.totalFiles,
            name = this.name,
            creator = this.creator,
            depositor = this.depositor.namespace,
            replicatingNodes = this.getReplicatingNodes(),
            status = this.status,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            bagStorage = this.bagStorage
                    .firstOrNull { it.active }
                    ?.model(),
            tokenStorage = this.tokenStorage
                    .firstOrNull { it.active }
                    ?.model()

    )
}

fun ContactEntity.model(): DepositorContact {
    return DepositorContact(this.contactName, this.contactEmail, this.contactPhone)
}

fun DepositorEntity.model(): Depositor {
    return Depositor(
            id = this.id ?: -1,
            namespace = this.namespace,
            sourceOrganization = this.sourceOrganization,
            organizationAddress = this.organizationAddress,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt,
            replicatingNodes = this.nodeDistributions.map { it.node.username }.toSet(),
            contacts = this.contacts.map { it.model() }.toSet()
    )
}

fun RepairEntity.model(): Repair {
    return Repair(
            id = this.id ?: -1,
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
            id = this.id ?: -1,
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
            region = this.region.id ?: -1,
            size = this.size,
            totalFiles = this.totalFiles,
            fixities = this.fixities
                    .map { Fixity(it.value, it.algorithm, it.createdAt) }
                    .toSet()
    )
}

fun StorageRegionEntity.model(): StorageRegion {
    val storageId = this.id ?: -1
    return StorageRegion(
            id = storageId,
            capacity = this.capacity,
            node = this.node.username,
            note = this.note,
            dataType = this.dataType,
            storageType = this.storageType,
            replicationConfig = this.replicationConfig.let {
                // need to test username nullability
                ReplicationConfig(storageId, it.path, it.server, it.username ?: "chronopolis")
            }
    )
}

// todo: Move to another... class
fun FulfillmentStrategy.toEntity(): Strategy = when (this) {
    is AceStrategy -> Ace(this.apiKey, this.url)
    is RsyncStrategy -> Rsync(this.link)
}
