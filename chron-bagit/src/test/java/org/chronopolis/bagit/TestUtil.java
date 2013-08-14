/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import org.chronopolis.bagit.util.TagMetaElement;

/**
 *
 * @author shake
 */
public class TestUtil {

    /* To avoid writing a file to disk, we just write to a pipe in memory and 
     * read from it
     * 
     */
    public static BufferedReader createReader(TagMetaElement... elements) 
            throws IOException {
        PipedReader pr = new PipedReader();
        PipedWriter pw = new PipedWriter();
        pw.connect(pr);
        try (BufferedWriter writer = new BufferedWriter(pw)) {
            int i = 0;
            for ( TagMetaElement e : elements ) {
                writer.write(e.toString());
                if ( ++i < elements.length) {
                    writer.newLine();
                }
            }
        }
        //pr.connect(pw);
        BufferedReader reader = new BufferedReader(pr);
        return reader;
    }
    
}
