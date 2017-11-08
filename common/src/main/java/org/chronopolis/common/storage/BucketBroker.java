package org.chronopolis.common.storage;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

/**
 * Class to determine the best bucket to place an operation in
 *
 * @author shake
 */
public class BucketBroker {

    private List<Bucket> buckets;

    public static BucketBroker fromProperties(PreservationProperties preservationProperties) {
        BucketBroker broker = new BucketBroker();
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

    private Stream<Bucket> weighBuckets(StorageOperation operation) {
        return buckets.stream()
                .sorted(comparing(bucket -> weight(bucket, operation), reverseOrder()));
    }

    private Long weight(Bucket bucket, StorageOperation operation) {
        String hashString = bucket.hashCode() + operation.getIdentifier();
        return Hashing.sha256()
                .hashString(hashString, Charset.defaultCharset())
                .asLong();
    }

    public BucketBroker setBuckets(List<Bucket> buckets) {
        this.buckets = buckets;
        return this;
    }

    public BucketBroker addBucket(Bucket bucket) {
        this.buckets.add(bucket);
        return this;
    }

    private class Weight {
        private final int bucket;
        private final HashCode hashCode;
        private final StorageOperation operation;

        private Weight(int bucket, StorageOperation operation) {
            this.bucket = bucket;
            this.operation = operation;

            // ummm not terrible I guess but not great either
            this.hashCode = Hashing.sha256().newHasher()
                    .putInt(bucket)
                    .putString(operation.getIdentifier(), Charset.defaultCharset())
                    .hash();
        }

        public int getBucket() {
            return bucket;
        }

        public HashCode getHashCode() {
            return hashCode;
        }

        public StorageOperation getOperation() {
            return operation;
        }
    }
}
