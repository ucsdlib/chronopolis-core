package org.chronopolis.replicate.batch.rsync;

import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.OperationType;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.storage.PosixBucket;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.ReplicationProperties;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class FilesFromRsyncTest {

    private final String linkName = "test-bag";
    private final String depositor = "chunked-test";
    private final String identifier = "chunked-rsync-test";

    private Path root;
    private Bucket bucket;
    private StorageOperation operation;
    private ReplicationProperties properties;

    @Before
    public void setup() throws URISyntaxException, IOException {
        URL resources = ClassLoader.getSystemClassLoader().getResource("");
        root = Paths.get(resources.toURI()).resolve("bags");
        Posix posix = new Posix()
                .setWarn(0.01)
                .setId(1L)
                .setPath(root.toString());
        bucket = new PosixBucket(posix);

        Path work = Paths.get(resources.toURI()).resolve("chunks");
        properties = new ReplicationProperties()
                .setWorkDirectory(work.toString());

    }

    // helpers for creating and cleaning allocated resources

    private ReplicationProperties allocateTmp(String test,
                                              String bag,
                                              String chunk) throws IOException {
        Path existing = Paths.get(properties.getWorkDirectory(), depositor, bag, chunk);
        Path temp = Files.createTempDirectory(test);
        Files.copy(existing, temp.resolve(chunk));
        return new ReplicationProperties()
                .setWorkDirectory(temp.toString());
    }

    private void cleanup(ReplicationProperties properties) {
        try (Stream<Path> paths = Files.walk(Paths.get(properties.getWorkDirectory()))) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {
        }
    }

    @Test
    public void success() throws IOException {
        String bag = "transfer-success";
        String chunkName = "chunk-kotlin";
        String testName = "files-from-success";

        ReplicationProperties properties = allocateTmp(testName, bag, chunkName);
        Path chunk = Paths.get(properties.getWorkDirectory(), chunkName);
        Path bagRoot = root.resolve(linkName);
        Path opRoot = Paths.get(depositor).resolve(bag);
        Path onFs = root.resolve(opRoot);
        Files.createDirectories(onFs);

        operation = new DirectoryStorageOperation(opRoot);
        operation.setSize(10L);
        operation.setIdentifier(identifier);
        operation.setType(OperationType.RSYNC);
        operation.setLink(bagRoot.toString());
        FilesFromRsync rsync = new FilesFromRsync(chunk, bucket, operation, properties);

        Result<Path> from = rsync.get();
        Assert.assertNotNull(from);
        Assert.assertTrue(from instanceof Result.Success);
        Result.Success<Path> success = (Result.Success<Path>) from;
        Path data = success.getData();
        Assert.assertNotNull(data);
        cleanup(properties);
    }

    @Test
    public void failureChunk() throws IOException {
        String bag = "transfer-fail-chunk";
        String chunkName = "chunk-dne";

        Path chunk = Paths.get(properties.getWorkDirectory(), depositor, bag, chunkName);
        Path bagRoot = root.resolve(linkName);
        Path opRoot = Paths.get(depositor).resolve(bag);
        Path onFs = root.resolve(opRoot);
        Files.createDirectories(onFs);

        operation = new DirectoryStorageOperation(opRoot);
        operation.setSize(10L);
        operation.setIdentifier(identifier);
        operation.setType(OperationType.RSYNC);
        operation.setLink(bagRoot.toString());
        FilesFromRsync rsync = new FilesFromRsync(chunk, bucket, operation, properties);

        Result<Path> from = rsync.get();
        Assert.assertNotNull(from);
        Assert.assertTrue(from instanceof Result.Error);
        Result.Error error = (Result.Error) from;
        Assert.assertTrue(error.getException() instanceof FileTransferException);
    }

    @Test
    public void failureNotAllocated() {
        String bag = "transfer-fail-not-allocated";
        String chunkName = "chunk-not-allocated";

        Path chunk = Paths.get(properties.getWorkDirectory(), depositor, bag, chunkName);
        Path bagRoot = root.resolve(linkName);
        Path opRoot = Paths.get(depositor).resolve(bag);

        operation = new DirectoryStorageOperation(opRoot);
        operation.setSize(10L);
        operation.setIdentifier(identifier);
        operation.setType(OperationType.RSYNC);
        operation.setLink(bagRoot.toString());
        FilesFromRsync rsync = new FilesFromRsync(chunk, bucket, operation, properties);

        Result<Path> from = rsync.get();
        Assert.assertNotNull(from);
        Assert.assertTrue(from instanceof Result.Error);
        Result.Error error = (Result.Error) from;
        Assert.assertTrue(error.getException() instanceof IOException);
    }

}