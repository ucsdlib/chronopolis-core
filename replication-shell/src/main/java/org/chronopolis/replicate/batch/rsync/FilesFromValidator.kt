package org.chronopolis.replicate.batch.rsync

import org.chronopolis.common.storage.StorageOperation
import org.chronopolis.replicate.ReplicationProperties
import org.chronopolis.rest.models.Bag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Function

/**
 * Validate post-chunked rsync operations
 *
 * Currently in order to validate a successful set of operations:
 * * result must be nonempty
 * * all results must be [Result.Success]
 * * the work directory under work/bag.depositor/bag.name must exist
 * * the work directory under work/bag.depositor/bag.name must be empty
 * * the work directory under work/bag.depositor/bag.name must be able to be removed
 *
 * @param bag The [Bag] which should have been transferred
 * @param operation the [StorageOperation]
 * @param properties the configuration properties
 * @author shake
 */
class FilesFromValidator(
        val bag: Bag,
        val operation: StorageOperation,
        val properties: ReplicationProperties
) : Function<List<Result<Path>>, Result<Path>> {

    override fun apply(results: List<Result<Path>>): Result<Path> {
        val work = properties.workDirectory

        val chunkRoot = Paths.get(work, bag.depositor, bag.name)
        return try {
            if (allSuccess(results)
                    && Files.list(chunkRoot).use { 0L == it.count() }
                    && Files.deleteIfExists(chunkRoot)) {
                Result.Success(chunkRoot)
            } else {
                Result.Error(IOException("Work Directory still contains chunks: $chunkRoot"))
            }
        } catch (e: IOException) {
            log.error("[{}] Unable to cleanup work directory", operation.identifier)
            Result.Error(e)
        }
    }

    private fun allSuccess(result: List<Result<Path>>): Boolean {
        return result.isNotEmpty() && result.all {
            when (it) {
                is Result.Success -> true
                is Result.Error -> false
            }
        }
    }

    companion object {
        val log : Logger = LoggerFactory.getLogger(FilesFromValidator::class.java)
    }
}

