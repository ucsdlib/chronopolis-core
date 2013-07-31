/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit.util;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Let's calculate the oxum in this class
 *
 * @author shake
 */
public class PayloadOxum {
    private long octetCount;
    private long numFiles;

    public PayloadOxum() {
        this.octetCount = 0;
        this.numFiles = 0;
    }

    /**
     * @return the octetCount
     */
    public long getOctetCount() {
        return octetCount;
    }

    /**
     * @param octetCount the octetCount to set
     */
    public void setOctetCount(long byteSize) {
        this.octetCount = byteSize;
    }

    /**
     * @return the numFiles
     */
    public long getNumFiles() {
        return numFiles;
    }

    /**
     * @param numFiles the numFiles to set
     */
    public void setNumFiles(long numFiles) {
        this.numFiles = numFiles;
    }

    // Todo: Traverse all dirs
    public void calculateOxum(Path directory) throws IOException {
        List<DirectoryStream<Path>> dirs = new ArrayList<>();
        DirectoryStream<Path> dir = Files.newDirectoryStream(directory);
        for ( Path p : dir ) {
            if ( p.toFile().isFile() ) {
                octetCount += p.toFile().length();
                ++numFiles;
            }
        }

    }

    @Override
    public String toString() {
        return octetCount+"."+numFiles;
    }
    
}
