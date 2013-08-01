/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author shake
 */
public class DigestUtil {
    public static String byteToHex(byte[] bytes) {
        StringBuilder str = new StringBuilder(new BigInteger(1, bytes).toString(16));
        if ( str.length() < bytes.length*2) {
            for ( int i=0; i < bytes.length*2-str.length(); i++) {
                str.insert(0, "0");
            }
        }
        return str.toString();
    }
    
    public static byte[] doDigest(Path path, MessageDigest md) throws FileNotFoundException, 
                                                                      IOException {
        FileInputStream fis = new FileInputStream(path.toFile());
        try (DigestInputStream dis = new DigestInputStream(fis, md)) {
            dis.on(true);
            int bufferSize = 1048576; // should move into some type of settings
            byte []buf = new byte[bufferSize];
            while ( dis.read(buf) != -1) {
                // spin
            }
        }
        return md.digest();
    }

    // TODO: Maybe there's a cleaner way to do this? Whatever at least it works
    // and is sort of atomic.
    public static byte[] convertToSHA256(Path path, MessageDigest md, String validDigest ) 
            throws FileNotFoundException, IOException, NoSuchAlgorithmException {

        if ( validDigest == null || md == null || path == null ) {
            throw new RuntimeException("NO NULL ARGUMENTS! BAD!");
        }
        
        BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ));
        MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");
        try (DigestInputStream dis = new DigestInputStream(bis, md); 
             DigestInputStream nis = new DigestInputStream(bis, shaDigest)) {
            byte [] buffer = new byte[1048567];
            while ( bis.available() > 0 ) { 
                bis.mark(buffer.length);
                dis.read(buffer);
                bis.reset();
                nis.read(buffer);
            }
        }

        String calculatedDigest = byteToHex(md.digest());
        if ( !calculatedDigest.equalsIgnoreCase(validDigest)) {
            return null;
        }
        
        return shaDigest.digest();
    }
    
}
