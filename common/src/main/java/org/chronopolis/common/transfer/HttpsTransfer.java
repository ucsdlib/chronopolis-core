/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.common.transfer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Stop requests that are sent w/ http
 * TODO: Common interface for different transfer types? 
 *       
 *
 * @author shake
 */
public class HttpsTransfer {
    private final Logger log = LoggerFactory.getLogger(HttpsTransfer.class);
    /**
     *
     * @param response
     */
    public Path getFile(String uri, Path stage) throws IOException {
        // Make HTTP Connection
        URL url = new URL(uri);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        Path output = Paths.get(stage.toString(),
                                uri.substring(uri.lastIndexOf("/", uri.length())));
        Path parent = output.getParent();
        parent.toFile().mkdirs();
        output.toFile().createNewFile();
        FileOutputStream fos = new FileOutputStream(output.toString());
        FileChannel fc = fos.getChannel();
        fc.transferFrom(rbc, 0, Long.MAX_VALUE);
        
        return output;
    }
}
