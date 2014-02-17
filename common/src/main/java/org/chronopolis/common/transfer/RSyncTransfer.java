/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.common.transfer;

import java.io.IOException;
import java.nio.file.Path;

import org.chronopolis.common.exception.RSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: If the rsync cannot connect it will simply hang. 
 *       We need some sort of prevention against that.
 *       Note: Can make a ScheduledFuture w/ a timeout
 *
 * @author shake
 */
public class RSyncTransfer implements FileTransfer {
    private final Logger log = LoggerFactory.getLogger(RSyncTransfer.class);
    String user;
    String password;

    public RSyncTransfer(String user) {
      this.user = user;
    }

    @Override
    public Path getFile(String uri, Path local) {
        // Taken from http://stackoverflow.com/questions/1246255/any-good-rsync-library-for-java
        // Need to test/modify command 
        // Currently uses passwordless SSH keys to login to sword
        log.debug(local.toString());
        String[] cmd = new String[]{"rsync", "-az", user + "@" + uri, local.toString()};
        String[] parts = uri.split(":", 2);
        String[] pathList = parts[1].split("/");
        ProcessBuilder pb = new ProcessBuilder(cmd);
        Process p = null;
        try {
            log.info("Executing '{}' '{}' '{}' '{}'", cmd);
            p = pb.start();
            p.waitFor();
        } catch (IOException e) {
            log.error("IO Exception in rsync ", e);
        } catch (InterruptedException e) {
            log.error("rsync was interrupted", e);
        }

        Path dir = local.resolve(pathList[pathList.length-1]);
        return dir;
    }
    
}
