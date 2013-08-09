/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

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

    public void calculateOxum(Path directory) throws IOException {
        // Clear old oxum
        numFiles = 0;
        octetCount = 0;

        // Walk the file tree
        // and have the visitor increment our payload
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>(){ 
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if ( attrs.isRegularFile() ) {
                    ++numFiles;
                    octetCount += attrs.size();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    
    @Override
    public boolean equals(Object other) {
        if ( other == null ) {
            return false;
        }
        if ( !(other instanceof PayloadOxum)) {
            return false;
        }
        PayloadOxum ot = (PayloadOxum)other;

        if ( octetCount != ot.getOctetCount()) {
            return false;
        }
        if ( numFiles != ot.getNumFiles()) {
            return false;
        }


        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + (int) (this.octetCount ^ (this.octetCount >>> 32));
        hash = 53 * hash + (int) (this.numFiles ^ (this.numFiles >>> 32));
        return hash;
    }

    @Override
    public String toString() {
        return octetCount+"."+numFiles;
    }

    public void setFromString(String val) {
        if ( !val.contains(("."))) {
            throw new RuntimeException("Error parsing payload, please use the following format: octetCount.fileCount");
        }

        String[] payload = val.split(".");

        if ( payload.length != 2 ) {
            throw new RuntimeException("Too many values in the PayloadOxum");
        }

        octetCount = Long.parseLong(payload[0]);
        numFiles = Long.parseLong(payload[1]);
    }

    public BagMetaElement toBagMetaElement() {
        String payloadRE = "Payload-Oxum";
        BagMetaElement payloadOxum = new BagMetaElement(payloadRE, 
                                                        this.toString());
        return payloadOxum;
    }
    
}
