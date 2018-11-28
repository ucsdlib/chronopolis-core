package org.chronopolis.replicate.batch.rsync

import okhttp3.ResponseBody
import org.chronopolis.replicate.ReplicationProperties
import org.chronopolis.rest.api.FileService
import org.chronopolis.rest.models.Bag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import retrofit2.Call
import java.io.BufferedWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Supplier


/**
 * Download a file listing for a collection and save it into multiple chunked files.
 *
 * So at the moment we're using a [Result] class which provides us with a way to denote Success
 * and Failure (much like Try would). However, the fact that we're also using [CompletableFuture]s
 * means we _could_ let [RuntimeException]s bubble up and catch them with methods like whenComplete
 * and exceptionally. IDK lots to think about and paralysis probably isn't the best way to program.
 *
 * Let's justify the use of [Result] like this for now: it's a safe way to handle failure and does
 * not incur as much of a cost as throwing an Exception.
 *
 * @param bag the [Bag] whose file listing to download
 * @param api the [FileService] for interacting with the Ingest Server
 * @param properties the [ReplicationProperties] containing configuration information
 *
 * @author shake
 */
class ListingDownloader(val bag: Bag,
                        val api: FileService,
                        val properties: ReplicationProperties) : Supplier<Result<List<Path>>> {

    override fun get(): Result<List<Path>> {
        val path = Paths.get(properties.workDirectory, bag.depositor, bag.name)

        // Result -> Success(paths: List<Path>)
        //        -> Error(e: Exception)
        //   executeRequest (executeRequest)
        //   writeFiles (writeResponseBody)
        return try {
            Files.createDirectories(path)
            executeRequest(path)
        } catch (e: IOException) {
            log.error("Error creating directory, check work directory property", e)
            Result.Error(e)
        }
    }

    private fun executeRequest(path: Path): Result<List<Path>> {
        val call: Call<ResponseBody> = api.getFileList(bag.id)

        return try {
            val response = call.execute()
            if (response.isSuccessful && response.body() != null) {
                writeResponseBody(path, response.body()!!)
            } else {
                log.error("[{}] Unable to complete request to ingest api {} {}",
                        bag.name, response.code(), response.message())
                Result.Error(IOException("Unable to complete request successfully"))
            }
        } catch (e: Exception) {
            log.error("[{}] Error communicating with ingest api", bag.name, e)
            Result.Error(e)
        }
    }

    /**
     * Write a [ResponseBody] to a given output directory in chunks (currently just by max number of
     * lines defined by a [Bag]s totalFiles divided by 10)
     */
    private fun writeResponseBody(output: Path, body: ResponseBody): Result<List<Path>> {
        val max = Math.ceil(bag.totalFiles.div(10.0)).toInt()
        val writer = BodyWriter(output)

        return body.charStream()
                .buffered()
                .lineSequence()
                .chunked(max)
                .mapIndexed(writer::write)
                .asResult()
    }

    class BodyWriter(private val output: Path) {
        fun write(index: Int, list: List<String>): Result<Path> {
            val path = output.resolve("chunk-$index")
            log.debug("Writing chunk {}", path)
            return try {
                Files.newBufferedWriter(path).use { writer ->
                    list.forEach { writer.writeLn(it) }
                }
                Result.Success(path)
            } catch (e: IOException) {
                log.error("Error writing chunked listing {}", path, e)
                Result.Error(e)
            }
        }

        /**
         * Extension function to append a newline to each string that is written
         */
        private fun BufferedWriter.writeLn(str: String) {
            this.write(str)
            this.newLine()
        }
    }

    companion object {
        val log: Logger = LoggerFactory.getLogger(ListingDownloader::class.java)
    }
}

