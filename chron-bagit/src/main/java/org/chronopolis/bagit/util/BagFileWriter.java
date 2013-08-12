/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Just to write files like bagit.txt, baginfo.txt, etc
 *
 * @author shake
 */
public class BagFileWriter {
    private static final Logger log = 
            LoggerFactory.getLogger(BagFileWriter.class);
    
    public static void write(Path file,
                             List<TagMetaElement> elements,
                             OpenOption opt) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(file,
                                                            Charset.forName("UTF-8"),
                                                            opt);
            int index = 0;
            for ( TagMetaElement element : elements) {
                writer.write(element.toString());
                if ( ++index < elements.size()) {
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            log.error("Error writing to {} : {}", file, ex);
        }
        
    }
    
}
