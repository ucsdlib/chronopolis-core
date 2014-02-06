package org.chronopolis.common.digest;

/**
 * Created by shake on 2/6/14.
 */
public enum Digest {

    SHA_256("SHA-256", "sha256"),
    MD5("MD5", "md5");


    private final String name;
    private final String bagitIdentifier;

    Digest(String name, String bagitIdentifier) {
        this.name = name;
        this.bagitIdentifier = bagitIdentifier;

    }

    public static Digest fromString(String algorithm) {
        if ( algorithm.equalsIgnoreCase(SHA_256.name) ||
             algorithm.equalsIgnoreCase(SHA_256.bagitIdentifier)) {
            return SHA_256;
        } else if (algorithm.equalsIgnoreCase(MD5.name)) {
            return MD5;
        } else {
            throw new IllegalArgumentException("Invalid algorithm! " + algorithm);
        }
    }

    public String getName() {
        return name;
    }

    public String getBagitIdentifier() {
        return bagitIdentifier;
    }
}
