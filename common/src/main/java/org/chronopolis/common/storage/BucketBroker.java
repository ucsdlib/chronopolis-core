package org.chronopolis.common.storage;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

/**
 * Class to determine the best bucket to place an operation in
 * <p>
 * Should this be an interface?
 *
 * @author shake
 */
public class BucketBroker {

    private static final Logger log = LoggerFactory.getLogger(BucketBroker.class);

    private final List<Bucket> buckets;

    private BucketBroker() {
        this.buckets = new CopyOnWriteArrayList<>();
    }

    /**
     * Getter only for testing so that we can introspect on the buckets managed by
     * the broker
     *
     * @return the managed buckets
     */
    @VisibleForTesting
    protected Set<Bucket> buckets() {
        return ImmutableSet.copyOf(buckets);
    }

    /**
     * Create a BucketBroker from the configuration used for defining the
     * Preservation Storage
     *
     * @param preservationProperties the configuration for preservation storage systems
     * @return a BucketBroker for managing StorageOperations over said preservation storage
     * @throws BeanCreationException if a Bucket can not be created for given configuration parameters
     */
    public static BucketBroker fromProperties(PreservationProperties preservationProperties) {
        BucketBroker broker = new BucketBroker();

        // I guess we'll need to do this for each type of storage
        // should probably be a private static method
        preservationProperties.getPosix().forEach(posix -> {
            try {
                broker.addBucket(new PosixBucket(posix));
            } catch (IOException e) {
                // Not sure if we should push this to the constructor or do it here
                log.error("[{}] Error creating Bucket", posix.getPath(), e);
                throw new BeanCreationException(e.getMessage());
            }
        });

        return broker;
    }

    /**
     * Return a bucket broker which manages only a single bucket. Useful for testing.
     *
     * @param bucket The bucket to manager
     * @return a new BucketBroker
     */
    public static BucketBroker forBucket(Bucket bucket) {
        BucketBroker broker = new BucketBroker();
        broker.addBucket(bucket);
        return broker;
    }

    /**
     * Get the first Bucket for which a StorageOperation can be written to
     *
     * @param operation the operation to allocate space for
     * @return the Bucket to write the operation to
     */
    public Optional<Bucket> allocateSpaceForOperation(StorageOperation operation) {
        return weighBuckets(operation)
                .filter(bucket -> bucket.allocate(operation))
                .findFirst();
    }

    /**
     * Find a Bucket which contains a StorageOperation in it
     *
     * @param operation the operation to look for
     * @return the Bucket, or an Empty Optional if it does not exist
     */
    public Optional<Bucket> findBucketForOperation(StorageOperation operation) {
        return weighBuckets(operation)
                .filter(bucket -> bucket.contains(operation))
                .findFirst();
    }

    /**
     * Return a weighted stream of Buckets for a given storage operation
     * todo: test this
     *
     * @param operation the operation we're looking for
     * @return a stream of buckets, weighted by how likely they are to contain the operation
     */
    private Stream<Bucket> weighBuckets(StorageOperation operation) {
        return buckets.stream()
                .sorted(comparing(bucket -> weight(bucket, operation), reverseOrder()));
    }

    /**
     * Retrieve the "weight" for a Bucket. I.e. given a StorageOperation, how likely
     * the bucket is to contain the Operation (not mapped on [0,1], however).
     *
     * @param bucket    the bucket to weight
     * @param operation the operation to check for
     * @return the weight
     */
    private Long weight(Bucket bucket, StorageOperation operation) {
        String hashString = bucket.hashCode() + operation.getIdentifier();
        return Hashing.sha256()
                .hashString(hashString, Charset.defaultCharset())
                .asLong();
    }

    /**
     * Add a bucket which can be used for various StorageOperations
     *
     * @param bucket the bucket to add
     * @return this BucketBroker
     */
    private BucketBroker addBucket(Bucket bucket) {
        this.buckets.add(bucket);
        return this;
    }

}
