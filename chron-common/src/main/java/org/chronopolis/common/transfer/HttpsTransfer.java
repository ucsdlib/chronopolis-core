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
 *       Move other code over and what not
 *
 *
 * @author shake
 */
public class HttpsTransfer extends FileTransfer {
    private final Logger log = LoggerFactory.getLogger(HttpsTransfer.class);
    /**
     *
     * @param response
     */
    public Path getFile(String uri, Path stage) throws IOException {
        // Make HTTP Connection
        System.out.println("Setting up url");
        URL url = new URL(uri);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        System.out.println("Now the other stuff");
        Path output = Paths.get(stage.toString(),
                uri.substring(uri.lastIndexOf("/", uri.length())));
        Path parent = output.getParent();
        System.out.println(parent.toString());
        System.out.println(output.toString());
        parent.toFile().mkdirs();
        output.toFile().createNewFile();
        FileOutputStream fos = new FileOutputStream(output.toString());
        FileChannel fc = fos.getChannel();
        fc.transferFrom(rbc, 0, 1<<24);
        
        /*
         * // We may want to use the Files creation methods. will test performance later
         * OutputStream fos = Files.newOutputStream(output, CREATE);
         * 
         * // Also some digest stuff
         * ByteBuffer buf = ByteBuffer.allocate(blockSize);
         * 
         * // Write file and update digest
         * while ( rbc.read(buf) > 0 ) {
         * // Do we want to throw an exception if write < 0?
         * byte[] out = buf.array();
         * int write = fc.write(buf);
         * md.update(out);
         * // buf.clear(); // I believe read takes care of this, will test later
         * }
         * fc.close();
         */
        
        return output;
    }
}
