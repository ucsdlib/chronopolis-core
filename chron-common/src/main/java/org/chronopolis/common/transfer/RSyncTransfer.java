/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.common.transfer;

import java.io.IOException;
import org.chronopolis.common.exception.RSyncException;

/**
 * TODO: If the rsync cannot connect it will simply hang. 
 *       We need some sort of prevention against that.
 *
 * @author shake
 */
public class RSyncTransfer {

    public int getFile(String user, String host, String remote, String local) throws IOException, InterruptedException {
        // Taken from http://stackoverflow.com/questions/1246255/any-good-rsync-library-for-java
        // Need to test/modify command 
        // Currently uses passwordless SSH keys to login to sword
        String[] cmd = new String[]{"rsync", "-r", user + "@" + host + ":" + remote, local};
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = pb.start();
        int val = p.waitFor();
        if (val != 0) {
            throw new RSyncException("Exception during RSync; return code = " + val);
        }
        return val;
    }
    
}
