package org.chronopolis.replicate.batch.rsync;

import com.google.common.collect.ImmutableSet;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.storage.PosixBucket;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.FixityUpdate;
import org.chronopolis.test.support.CallWrapper;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HasherTest {

    private final long id = 1L;
    private final String hash = "699caf4dc3dd8bd084f18174035a627b71f31cf5d07d5adbd722c45b874e7a78";

    private final URL resources = ClassLoader.getSystemClassLoader().getResource("");
    private final ReplicationService replications = mock(ReplicationService.class);

    private Bucket bucket;

    @Before
    public void setup() throws URISyntaxException, IOException {
        Path root = Paths.get(resources.toURI()).resolve("bags");
        Posix posix = new Posix()
                .setWarn(0.01)
                .setId(id)
                .setPath(root.toString());

        bucket = new PosixBucket(posix);
    }

    @Test
    public void success() {
        StorageOperation operation = operation("test-bag", "hash-kotlin-success");
        FixityUpdate fixityUpdate = new FixityUpdate(hash);
        Replication replication = replication();
        long replicationId = replication.getId();

        when(replications.updateTagManifestFixity(eq(replicationId), eq(fixityUpdate)))
                .thenReturn(new CallWrapper<>(replication));
        BagHasher hasher = new BagHasher(bucket, operation, replication, replications);
        hasher.accept(new Result.Success<>(Paths.get("success")));

        verify(replications, times(1)).updateTagManifestFixity(eq(replicationId), eq(fixityUpdate));
    }

    @Test
    public void hashError() {
        Result<Path> failure = new Result.Error<>(new IOException("failure"));
        StorageOperation operation = operation("test-error", "hash-kotlin-failure");
        Replication replication = replication();

        BagHasher hasher = new BagHasher(bucket, operation, replication, replications);
        hasher.accept(failure);

        verify(replications, never()).updateTagManifestFixity(eq(id), eq(new FixityUpdate(hash)));
    }

    @Test
    public void hashFailure() {
        StorageOperation operation = operation("test-dne", "hash-kotlin-failure");
        FixityUpdate fixityUpdate = new FixityUpdate(hash);
        Replication replication = replication();
        long replicationId = replication.getId();

        when(replications.updateTagManifestFixity(eq(replicationId), eq(fixityUpdate)))
                .thenReturn(new CallWrapper<>(replication));
        BagHasher hasher = new BagHasher(bucket, operation, replication, replications);
        hasher.accept(new Result.Success<>(Paths.get("dne")));

        verify(replications, times(0)).updateTagManifestFixity(eq(replicationId), eq(fixityUpdate));
    }

    private StorageOperation operation(String path, String identifier) {
        StorageOperation operation = new DirectoryStorageOperation(Paths.get(path));
        operation.setIdentifier(identifier);
        return operation;
    }

    private Replication replication() {
        Bag bag = new Bag(id, id, id, null, null, ZonedDateTime.now(), ZonedDateTime.now(),
                "test-bag", "hasher-test", "hasher-test", BagStatus.REPLICATING, ImmutableSet.of());
        return new Replication(id, ZonedDateTime.now(), ZonedDateTime.now(),
                ReplicationStatus.TRANSFERRED, "bag-link", "token-link", "rsync", "", "", "test-node",
                bag);
    }

}