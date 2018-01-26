package org.chronopolis.replicate.batch;

import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.BucketBroker;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.OperationType;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.storage.PosixBucket;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
    @Mock
    private ReplicationService replications;

    @Before
    public void setup() throws URISyntaxException, IOException {
        replications = Mockito.mock(ReplicationService.class);
        URL resources = ClassLoader.getSystemClassLoader().getResource("");
        root = Paths.get(resources.toURI()).resolve("bags");
        Posix posix = new Posix()
                .setId(1L)
                .setPath(root.toString());
        bucket = new PosixBucket(posix);

        broker = BucketBroker.forBucket(bucket);
        generator = new ReplGenerator(replications);
        replication = new Replication()
                .setId(1L)
                .setBag(new Bag().setName("ALLOCATOR-TEST"));
    }

    @Test
    public void allocate() {
        StorageOperation op1 = new DirectoryStorageOperation(Paths.get(ALLOCATE_PARENT).resolve(ALLOCATE_BAG))
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);
        StorageOperation op2 = new SingleFileOperation(Paths.get(ALLOCATE_PARENT).resolve(ALLOCATE_TS))
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);

        when(replications.updateStatus(eq(1L), any(RStatusUpdate.class))).thenReturn(new CallWrapper<>(replication));

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
        // ???
        StorageOperation op1 = new DirectoryStorageOperation(Paths.get(ALLOCATE_BAG))
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);
        StorageOperation op2 = new SingleFileOperation(Paths.get(ALLOCATE_BAG).resolve(ALLOCATE_TS))
                .setType(OperationType.RSYNC)
                .setIdentifier("id")
                .setSize(1L);

        when(replications.updateStatus(eq(1L), any(RStatusUpdate.class))).thenReturn(new CallWrapper<>(replication));
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