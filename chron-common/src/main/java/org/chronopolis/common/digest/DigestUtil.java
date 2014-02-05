package org.chronopolis.common.digest;

import edu.umiacs.ace.util.HashValue;
import edu.umiacs.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by shake on 1/28/14.
 */
public class DigestUtil {
    private static final Logger log = LoggerFactory.getLogger(DigestUtil.class);

    /**
     * Digest the file and return it in hex format
     *
     * @param file the file to digest
     * @param alg the algorithm to use (sha-256)
     * @return string representation of the digest
     */
    public static String digest(Path file, String alg) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(alg);
            DigestInputStream dis = new DigestInputStream(Files.newInputStream(file), md);
            int bufferSize = 1046576;
            byte[] buf = new byte[bufferSize];
            while ( dis.read(buf) >= 0 ) { }
        } catch (NoSuchAlgorithmException e) {
            log.error("Error finding algorithm {}", alg, e);
        } catch (IOException e) {
            log.error("IO Error for {}", file, e);
        }

        assert md != null;
        return HashValue.asHexString(md.digest());
    }

}
