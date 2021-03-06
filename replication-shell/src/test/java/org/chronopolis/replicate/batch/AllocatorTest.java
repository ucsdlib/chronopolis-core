package org.chronopolis.replicate.batch;

import com.google.common.collect.ImmutableSet;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.BucketBroker;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.OperationType;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.storage.PosixBucket;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.chronopolis.test.support.CallWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.time.ZonedDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for various allocation possibilities
 *
 * todo: mkdir fail... not sure how
 *
 * @author shake
 */
public class AllocatorTest {

    private final ReplicationStatus EXPECTED_SUCCESS = ReplicationStatus.STARTED;
    private final ReplicationStatus EXPECTED_FAILURE = ReplicationStatus.FAILURE;
    private final String ALLOCATE_PARENT = "test-allocate";
    private final String ALLOCATE_BAG = "test-bag";
    private final String ALLOCATE_OVERFLOW = "test-overflow";
    private final String ALLOCATE_TS = "test-token-store";

    private Path root;
    private Bucket bucket;
    private Allocator allocator;
    private BucketBroker broker;
    private Replication replication;

    private ServiceGenerator generator;
    @Mock private ReplicationService replications;

    @Before
    public void setup() throws URISyntaxException, IOException {
        replications = Mockito.mock(ReplicationService.class);
        URL resources = ClassLoader.getSystemClassLoader().getResource("");
        root = Paths.get(resources.toURI()).resolve("bags");
        Posix posix = new Posix()
                .setWarn(0.01)
                .setId(1L)
                .setPath(root.toString());
        bucket = new PosixBucket(posix);

        broker = BucketBroker.forBucket(bucket);
        generator = new ReplGenerator(replications);

        Bag bag = new Bag(1L, 1L, 1L, null, null, now(), now(), "ALLOCATOR-TEST", "allocator-test",
                "test-depositor", BagStatus.REPLICATING, ImmutableSet.of());
        replication = new Replication(1L, now(), now(), ReplicationStatus.PENDING,
                "bag-link", "token-link", "protocol", "", "", "test-node", bag);
    }

    @Test
    public void allocate() {
        Path bagPath = Paths.get(ALLOCATE_PARENT).resolve(ALLOCATE_BAG);
        Path tokenPath = Paths.get(ALLOCATE_PARENT).resolve(ALLOCATE_TS);
        StorageOperation op1 = new DirectoryStorageOperation(bagPath)
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);
        StorageOperation op2 = new SingleFileOperation(tokenPath)
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);

        when(replications.updateStatus(eq(1L), any(ReplicationStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(replication));

        allocator = new Allocator(broker, op1, op2, replication, generator);
        ReplicationStatus status = allocator.get();
        Assert.assertEquals(EXPECTED_SUCCESS, status);

        Path parent = root.resolve(ALLOCATE_PARENT);
        Path child = parent.resolve(ALLOCATE_BAG);
        Assert.assertTrue(parent.toFile().exists());
        Assert.assertTrue(parent.toFile().isDirectory());
        Assert.assertTrue(child.toFile().exists());
        Assert.assertTrue(child.toFile().isDirectory());
    }

    @Test
    public void allocateExists() {
        Path bagPath = Paths.get(ALLOCATE_BAG);
        Path tokenPath = Paths.get(ALLOCATE_BAG).resolve(ALLOCATE_TS);
        StorageOperation op1 = new DirectoryStorageOperation(bagPath)
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);
        StorageOperation op2 = new SingleFileOperation(tokenPath)
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);

        when(replications.updateStatus(eq(1L), any(ReplicationStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(replication));
        allocator = new Allocator(broker, op1, op2, replication, generator);
        ReplicationStatus status = allocator.get();
        Assert.assertEquals(EXPECTED_SUCCESS, status);
    }

    @Test(expected = IllegalArgumentException.class)
    public void allocateFail() {
        StorageOperation op1 = new DirectoryStorageOperation(Paths.get(ALLOCATE_PARENT).resolve(ALLOCATE_OVERFLOW))
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(Long.MAX_VALUE);
        StorageOperation op2 = new SingleFileOperation(Paths.get(ALLOCATE_PARENT).resolve(ALLOCATE_TS))
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(Long.MAX_VALUE);

        allocator = new Allocator(broker, op1, op2, replication, generator);
        ReplicationStatus status = allocator.get();
        Assert.assertEquals(EXPECTED_FAILURE, status);
    }

}