package org.chronopolis.replicate.batch.rsync

import com.google.common.collect.ImmutableList
import org.chronopolis.common.exception.FileTransferException
import org.chronopolis.common.storage.Bucket
import org.chronopolis.common.storage.StorageOperation
import org.chronopolis.replicate.ReplicationProperties
import org.chronopolis.replicate.batch.transfer.Transfer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier

/**
 * Get a rsync file transfer which includes the '--files-from' argument and passes the give from
 * path to read from
 *
 * @param from the 'from' [Path] containing a list of files to transfer
 * @param bucket the [Bucket] to transfer data in to
 * @param operation the [StorageOperation]
 * @param properties the configuration properties
 * @author shake
 */
class FilesFromRsync(val from: Path,
                     val bucket: Bucket,
                     val operation: StorageOperation,
                     val properties: ReplicationProperties) : Supplier<Result<Path>>, Transfer {

    override fun get(): Result<Path> {
        val ffArg = "--files-from"
        val id = operation.identifier
        val arguments: MutableList<String> = properties.rsync.arguments
        val fullArgs = ImmutableList.Builder<String>()
                .add(ffArg, from.toString())
                .addAll(arguments)
                .build()
        val transfer = bucket.transfer(operation, fullArgs)

        log.debug("[{}] Able to create file transfer? {}", id, transfer.isPresent)
        return transfer.map {
            try {
                it.get()
                log(id, it.output)
                Files.deleteIfExists(from)
                Result.Success(from)
            } catch (e: FileTransferException) {
                log(id, it.errors)
                Result.Error<Path>(e)
            } catch (e: IOException) {
                log.error("[{}] Unable to remove file list", operation.identifier)
                Result.Error<Path>(e)
            }
        }.orElse(Result.Error(IOException("Unable to create file transfer for $from")))
    }

    companion object {
        val log : Logger = LoggerFactory.getLogger(FilesFromRsync::class.java)
    }

}