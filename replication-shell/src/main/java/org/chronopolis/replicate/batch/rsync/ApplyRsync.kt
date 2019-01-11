package org.chronopolis.replicate.batch.rsync

import org.chronopolis.common.storage.Bucket
import org.chronopolis.common.storage.StorageOperation
import org.chronopolis.replicate.ReplicationProperties
import org.chronopolis.rest.api.ReplicationService
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.function.Function

/**
 * Simplify control flow in the [TransferFactory]
 *
 * @author shake
 */
class ApplyRsync(val bucket: Bucket,
                 val operation: StorageOperation,
                 val replications: ReplicationService,
                 val properties: ReplicationProperties,
                 private val ioPool: ThreadPoolExecutor
) : Function<Result<List<Path>>, List<Result<Path>>> {

    override fun apply(result: Result<List<Path>>): List<Result<Path>> {
        return when (result) {
            is Result.Success ->
                result.data.map { FilesFromRsync(it, bucket, operation, properties) }
                        .map { CompletableFuture.supplyAsync(it, ioPool) }
                        .map(CompletableFuture<Result<Path>>::join)
            is Result.Error -> listOf(Result.Error(result.exception))
        }
    }

}

