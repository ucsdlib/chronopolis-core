package org.chronopolis.common.digest;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 *
 * Created by shake on 2/6/14.
 */
public enum Digest {

    SHA_256("SHA-256", "sha256", Hashing.sha256()),
    MD5("MD5", "md5", Hashing.md5());


    private final String name;
    private final String bagitIdentifier;
    private final HashFunction function;

    Digest(final String name, final String bagitIdentifier, HashFunction function) {
        this.name = name;
        this.bagitIdentifier = bagitIdentifier;
        this.function = function;
    }

    public static Digest fromString(final String algorithm) {
        if (algorithm.equalsIgnoreCase(SHA_256.name)
            || algorithm.equalsIgnoreCase(SHA_256.bagitIdentifier)) {
            return SHA_256;
        } else if (algorithm.equalsIgnoreCase(MD5.name)) {
            return MD5;
        } else {
            throw new IllegalArgumentException("Invalid algorithm! " + algorithm);
        }
    }

    /**
     * Get the name of the message digest, used for
     * {@link java.security.MessageDigest#getInstance(String)}
     *
     * @return the digest name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the name of the message digest, formatted for the bagit spec
     *
     * @return the digest name
     */
    public String getBagitIdentifier() {
        return bagitIdentifier;
    }

    /**
     * Get the HashFunction for the Digest
     *
     * @return the HashFunction
     */
    public HashFunction getFunction() {
        return function;
    }
}
