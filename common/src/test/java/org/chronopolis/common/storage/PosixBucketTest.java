package org.chronopolis.common.storage;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class PosixBucketTest {

    private final String TEST_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private final String TEST_FILE = "test-file";
    private final String TEST_DEPOSITOR = "test-depositor";
    private final String TEST_BUCKET_0 = "test-0";

    private final String FREE = "test-free";
    private final String HASH = "test-hash";
    private final String LINK = "test-link";
    private final String STREAM = "test-stream";
    private final String REFRESH = "test-refresh";
    private final String STORAGE = "test-storage";
    private final String CONTAINS = "test-contains";
    private final String TRANSFER = "test-transfer";
    private final String WRITEABLE = "test-writeable";
    private final String ALLOCATION = "test-allocation";

    private Path dir;
    private PosixBucket bucket;

    @Before
    public void setup() throws IOException, URISyntaxException {
        URI buckets = ClassLoader.getSystemClassLoader().getResource("buckets").toURI();

        dir = Paths.get(buckets).resolve(TEST_BUCKET_0);
        Posix posix = new Posix()
                .setPath(dir.toString());
        bucket = new PosixBucket(posix);
    }

    @Test
    public void allocate() throws Exception {
        StorageOperation operation = new StorageOperation()
                .setSize(0L)
                .setLink(LINK)
                .setIdentifier(ALLOCATION)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(ALLOCATION));
        Assert.assertTrue("Operation is writeable and supported", bucket.allocate(operation));
        Assert.assertTrue("Operation is writeable and supported (resubmission)", bucket.allocate(operation));
    }

    @Test
    public void allocateNotWriteable() {
        StorageOperation operation = new StorageOperation()
                .setLink(LINK)
                .setSize(Long.MAX_VALUE) // ehhh
                .setIdentifier(ALLOCATION)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(ALLOCATION));
        Assert.assertFalse("Operation is not writeable and supported", bucket.allocate(operation));
    }

    @Test
    public void allocateNotSupported() {
        StorageOperation operation = new StorageOperation()
                .setSize(0L)
                .setLink(LINK)
                .setIdentifier(ALLOCATION)
                .setType(OperationType.NOP)
                .setPath(Paths.get(ALLOCATION));
        Assert.assertFalse("Operation is writeable and not supported", bucket.allocate(operation));
    }

    @Test
    public void contains() throws Exception {
        StorageOperation operation = new StorageOperation()
                .setSize(0L)
                .setLink(LINK)
                .setIdentifier(CONTAINS)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(CONTAINS));
        bucket.allocate(operation);
        Assert.assertTrue("Operation exists in bucket's pending operations", bucket.contains(operation));
    }

    public void containsOnDisk() {
        // todo
    }

    @Test
    public void notContains() {
        StorageOperation operation = new StorageOperation()
                .setSize(0L)
                .setLink(LINK)
                .setIdentifier(CONTAINS)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(CONTAINS));
        Assert.assertFalse("Operation does not exist", bucket.contains(operation));
    }

    @Test
    public void writeable() throws Exception {
        StorageOperation operation = new StorageOperation()
                .setSize(1L)
                .setLink(LINK)
                .setIdentifier(WRITEABLE)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(WRITEABLE));

        Assert.assertTrue("Operation is writeable", bucket.writeable(operation));
    }

    @Test
    public void writeableNoSpace() {
        StorageOperation operation = new StorageOperation()
                .setLink(LINK)
                .setSize(Long.MAX_VALUE)
                .setIdentifier(WRITEABLE)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(WRITEABLE));

        Assert.assertFalse("Operation is not writeable", bucket.writeable(operation));
    }

    @Test
    public void writeableHighPercent() {
        // do a bit of math to get an operation which will use a large amount of the remaining space
        // limit = 0.12 * total
        // max =  usable - limit
        Double limit = dir.toFile().getTotalSpace() * 0.12;
        Double size = dir.toFile().getUsableSpace() - limit;
        StorageOperation operation = new StorageOperation()
                .setLink(LINK)
                .setSize(size.longValue())
                .setIdentifier(WRITEABLE)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(WRITEABLE));

        Assert.assertTrue("Operation is writeable", bucket.writeable(operation));
    }

    @Test
    public void transfer() throws Exception {
        StorageOperation op = new StorageOperation()
                .setSize(1L)
                .setLink(LINK)
                .setIdentifier(TRANSFER)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(TRANSFER));

        bucket.allocate(op);
        Optional<FileTransfer> transfer = bucket.transfer(op);
        Assert.assertTrue("transfer is not empty", transfer.isPresent());
        // use map/ifPresent instead?
        Assert.assertEquals(RSyncTransfer.class, transfer.get().getClass());
    }

    @Test
    public void transferNotInPending() {
        StorageOperation op = new StorageOperation()
                .setSize(1L)
                .setLink(LINK)
                .setIdentifier(TRANSFER)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(TRANSFER));

        Optional<FileTransfer> transfer = bucket.transfer(op);
        Assert.assertFalse("transfer is empty", transfer.isPresent());
    }

    @Test
    public void hash() throws Exception {
        StorageOperation operation = new StorageOperation()
                .setSize(1L)
                .setLink(LINK)
                .setIdentifier(HASH)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(TEST_DEPOSITOR));

        bucket.allocate(operation);
        Optional<HashCode> hash = bucket.hash(operation, Paths.get(TEST_FILE));
        Assert.assertTrue("Optional is not empty", hash.isPresent());
        Assert.assertEquals(TEST_HASH, hash.get().toString());
    }

    @Test
    public void hashFileDNE() throws Exception {
        StorageOperation operation = new StorageOperation()
                .setSize(1L)
                .setLink(LINK)
                .setIdentifier(HASH)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(TEST_DEPOSITOR));

        bucket.allocate(operation);
        Optional<HashCode> hash = bucket.hash(operation, Paths.get("some-fake-file"));
        Assert.assertFalse("Optional is empty", hash.isPresent());
    }


    @Test
    public void stream() throws Exception {
         StorageOperation operation = new StorageOperation()
                .setSize(1L)
                .setLink(LINK)
                .setIdentifier(STREAM)
                .setType(OperationType.RSYNC)
                .setPath(Paths.get(TEST_DEPOSITOR));

        bucket.allocate(operation);
        Optional<ByteSource> source = bucket.stream(operation, Paths.get(TEST_FILE));

        Assert.assertTrue("Optional is not empty", source.isPresent());
        ByteSource bytes = source.get();
        Assert.assertTrue("Source is empty", bytes.isEmpty());
    }

    @Test
    public void fillAceStorage() throws Exception {
        StorageOperation op = new StorageOperation()
                .setSize(0L)
                .setLink(LINK)
                .setIdentifier(STORAGE)
                .setPath(Paths.get(STORAGE))
                .setType(OperationType.RSYNC);

        GsonCollection.Builder bldr = new GsonCollection.Builder();
        bldr = bucket.fillAceStorage(op, bldr);
        GsonCollection collection = bldr.build();

        final Path finalDirectory = dir.resolve(STORAGE);

        Assert.assertEquals("local", collection.getStorage());
        Assert.assertEquals(finalDirectory.toString(), collection.getDirectory());
    }

    @Test
    public void free() throws Exception {
        StorageOperation op = new StorageOperation()
                .setSize(0L)
                .setLink(LINK)
                .setIdentifier(FREE)
                .setPath(Paths.get(FREE))
                .setType(OperationType.RSYNC);

        Assert.assertTrue(bucket.allocate(op));
        bucket.free(op);
        Assert.assertFalse("Operation has been removed from the bucket", bucket.contains(op));
    }

}