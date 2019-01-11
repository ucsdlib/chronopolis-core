package org.chronopolis.replicate.batch.rsync

import org.chronopolis.common.storage.DirectoryStorageOperation
import org.chronopolis.replicate.ReplicationProperties
import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.enums.BagStatus
import org.junit.Assert
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.ZonedDateTime

class FilesFromValidatorTest {

    private val name = "files-from-bag"
    private val path = Paths.get(name)
    private val depositor = "files-from-validator-test"
    private val operation = DirectoryStorageOperation(path)

    private val bag = Bag(1L, 1L, 1L, null, null, ZonedDateTime.now(), ZonedDateTime.now(),
            name, depositor, depositor, BagStatus.REPLICATING, setOf())

    @Test
    fun success() {
        val identifier = "validator-success"
        val temp = Files.createTempDirectory(identifier)
        val workDir = temp.resolve(depositor).resolve(name)
        Files.createDirectories(workDir)

        val properties = ReplicationProperties().setWorkDirectory(temp.toString())
        operation.identifier = identifier

        val validator = FilesFromValidator(bag, operation, properties)

        val apply = validator.apply(listOf(Result.Success(path)))

        val isSuccess = when (apply) {
            is Result.Success -> true
            is Result.Error -> false
        }

        Assert.assertTrue(isSuccess)
        cleanup(properties)
    }

    @Test
    fun emptyList() {
        val identifier = "validator-empty"
        val temp = Files.createTempDirectory(identifier)
        val workDir = temp.resolve(depositor).resolve(name)
        Files.createDirectories(workDir)

        val properties = ReplicationProperties().setWorkDirectory(temp.toString())
        operation.identifier = identifier

        val validator = FilesFromValidator(bag, operation, properties)

        val apply = validator.apply(listOf())

        val isSuccess = when (apply) {
            is Result.Success -> false
            is Result.Error -> true
        }

        Assert.assertTrue(isSuccess)
        cleanup(properties)
    }

    @Test
    fun failureDirNotEmpty() {
        val identifier = "validator-not-empty"
        val temp = Files.createTempDirectory(identifier)
        val workDir = temp.resolve(depositor).resolve(name)
        Files.createDirectories(workDir)
        Files.createFile(workDir.resolve("chunk"))

        val properties = ReplicationProperties().setWorkDirectory(temp.toString())
        operation.identifier = identifier
        val validator = FilesFromValidator(bag, operation, properties)

        val apply = validator.apply(listOf())

        val isFailure = when (apply) {
            is Result.Success -> false
            is Result.Error -> true
        }

        Assert.assertTrue(isFailure)
        cleanup(properties)
    }

    @Test
    fun failureDirDoesNotExist() {
        val identifier = "validator-does-not-exist"
        val temp = Files.createTempDirectory(identifier)
        val workDir = temp.resolve(depositor)
        Files.createDirectories(workDir)
        Files.createFile(workDir.resolve("chunk"))

        val properties = ReplicationProperties().setWorkDirectory(temp.toString())
        operation.identifier = identifier
        val validator = FilesFromValidator(bag, operation, properties)

        val apply = validator.apply(listOf())

        val isFailure = when (apply) {
            is Result.Success -> false
            is Result.Error -> true
        }

        Assert.assertTrue(isFailure)
        cleanup(properties)
    }

    private fun cleanup(properties: ReplicationProperties) {
        Files.walk(Paths.get(properties.workDirectory)).use { stream ->
            stream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach { it.delete() }
        }
    }

}