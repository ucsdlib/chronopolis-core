/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.common.transfer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;

/**
 * TODO: Stop requests that are sent w/ http
 *       Move other code over and what not
 *       
 *
 * @author shake
 */
public class HttpsTransfer extends FileTransfer {
    /**
     *
     * @param response
     */
    public int getFile(HttpServletResponse response) { 
        return 0;
        /*
         *         // Break out to new class 
        try {
            // Make HTTP Connection
            URL url = new URL(site);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());

            FileOutputStream fos = new FileOutputStream(filename);
            FileChannel fc = fos.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(blockSize);

            // Write file and update digest
            while ( rbc.read(buf) > 0 ) {
                // Do we want to throw an exception if write < 0?
                byte[] out = buf.array();
                int write = fc.write(buf);
                md.update(out);
                // buf.clear(); // I believe read takes care of this, will test later
            }
            fc.close();

            // Check digests
            // byte[] calculatedDigest = md.digest()
            // convert to String
            // compare
            // return 1 if false



        } catch (IOException ex) {
            LOG.fatal(ex);
        }
         */
    }
}
