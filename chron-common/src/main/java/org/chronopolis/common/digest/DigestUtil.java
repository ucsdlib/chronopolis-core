package org.chronopolis.common.digest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by shake on 1/28/14.
 */
public class DigestUtil {
    private final Logger log = LoggerFactory.getLogger(DigestUtil.class);

    /**
     * Digest the file and return it in hex format
     *
     * @param file the file to digest
     * @param alg the algorithm to use (sha-256)
     * @return string representation of the digest
     */
    public static String digest(Path file, String alg) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException e) {
        }

        return null;
    }
}
