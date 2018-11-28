package org.chronopolis.replicate.batch.rsync

import com.google.common.hash.HashCode
import org.chronopolis.common.storage.Bucket
import org.chronopolis.common.storage.StorageOperation
import org.chronopolis.replicate.batch.callback.UpdateCallback
import org.chronopolis.rest.api.ReplicationService
import org.chronopolis.rest.models.Replication
import org.chronopolis.rest.models.update.FixityUpdate
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer


/**
 * A quick little class to hash a tagmanifest of a bag and update a replication with its value
 *
 * @param bucket The [Bucket] containing the replicated content
 * @param operation the [StorageOperation]
 * @param replication the [Replication]
 * @param replications the [ReplicationService] for communicating with the ingest api
 * @author shake
 */
class BagHasher(val bucket: Bucket,
                val operation: StorageOperation,
                val replication: Replication,
                val replications: ReplicationService) : Consumer<Result<Path>> {

    override fun accept(result: Result<Path>) {
        when (result) {
            is Result.Success -> run()
            else -> log.info("[{}] Skipping hash", operation.identifier)
        }
    }

    fun run() {
        log.debug("[{}] Running hasher", operation.identifier)
        // todo: get the filename from the ingest api models
        val hash = bucket.hash(operation, Paths.get("tagmanifest-sha256.txt"))
        hash.ifPresent(this::update)
    }

    fun update(hashCode: HashCode) {
        val cb = UpdateCallback()
        val hash = hashCode.toString()
        log.info("[{}] Calculated tagmanifest digest {}", operation.identifier, hash.toString())

        val call = replications.updateTagManifestFixity(replication.id, FixityUpdate(hash))
        call.enqueue(cb)
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(BagHasher::class.java)
    }
}