/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit.util;

import java.math.BigInteger;

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
}
