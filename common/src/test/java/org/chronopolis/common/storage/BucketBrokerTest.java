package org.chronopolis.common.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

public class BucketBrokerTest {

    private final String BUCKET_0 = "test-0";
    private final String BUCKET_1 = "test-1";
    private final String BUCKET_2 = "test-2";

    private final String ALLOCATE = "ALLOCATE";
    private final String SEARCH = "test-depositor/test-operation";
    private final String SEARCH_DNE = "test-depositor/test-dne";

    private BucketBroker broker;
    private Set<Bucket> buckets;

    @Before
    public void setup() throws URISyntaxException {
        URI bucketPath = ClassLoader.getSystemClassLoader().getResource("buckets").toURI();
        Path root = Paths.get(bucketPath);

        Posix posix0 = new Posix().setId(0L)
                .setWarn(0.01)
                .setPath(root.resolve(BUCKET_0).toString());
        Posix posix1 = new Posix().setId(0L)
                .setWarn(0.01)
                .setPath(root.resolve(BUCKET_1).toString());
        Posix posix2 = new Posix().setId(0L)
                .setWarn(0.01)
                .setPath(root.resolve(BUCKET_2).toString());

        PreservationProperties properties = new PreservationProperties()
                .setPosix(ImmutableList.of(posix0, posix1, posix2));

        broker = BucketBroker.fromProperties(properties);
        buckets = broker.buckets();
    }

    @Test
    public void allocateSpaceForOperation() {
        final StorageOperation operation = new DirectoryStorageOperation(Paths.get(ALLOCATE))
                .setSize(1L)
                .setLink(ALLOCATE)
                .setIdentifier(ALLOCATE)
                .setType(OperationType.RSYNC);

        Optional<Bucket> bucket = broker.allocateSpaceForOperation(operation);
        Assert.assertTrue(bucket.isPresent());

        // check buckets to make sure only one allocated space
        Bucket container = bucket.get();
        Sets.SetView<Bucket> difference = Sets.difference(buckets, ImmutableSet.of(container));
        Assert.assertEquals(2, difference.size());

        // Someone give me a good variable name pls
        difference.forEach(empty -> Assert.assertFalse(empty.contains(operation)));
    }

    @Test
    public void allocateSpaceForOperationNotSupported() {
        StorageOperation operation = new DirectoryStorageOperation(Paths.get(ALLOCATE))
                .setSize(1L)
                .setLink(ALLOCATE)
                .setIdentifier(ALLOCATE)
                .setType(OperationType.NOP);

        Optional<Bucket> bucket = broker.allocateSpaceForOperation(operation);
        Assert.assertFalse(bucket.isPresent());
    }

    @Test
    public void findBucketForOperation() {
        StorageOperation operation = new DirectoryStorageOperation(Paths.get(SEARCH))
                .setSize(1L)
                .setLink(SEARCH)
                .setIdentifier(SEARCH)
                .setType(OperationType.RSYNC);

        Optional<Bucket> bucketForOperation = broker.findBucketForOperation(operation);
        Assert.assertTrue(bucketForOperation.isPresent());
    }

    @Test
    public void findBucketForOperationNotExists() {
        StorageOperation operation = new DirectoryStorageOperation(Paths.get(SEARCH_DNE))
                .setSize(1L)
                .setLink(SEARCH_DNE)
                .setIdentifier(SEARCH_DNE)
                .setType(OperationType.RSYNC);

        Optional<Bucket> bucketForOperation = broker.findBucketForOperation(operation);
        Assert.assertFalse(bucketForOperation.isPresent());
    }

}