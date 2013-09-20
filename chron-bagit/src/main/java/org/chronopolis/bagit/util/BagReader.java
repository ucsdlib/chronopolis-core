/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class BagReader extends BufferedReader {
    private Pattern fullRegex;
    private final Logger log = LoggerFactory.getLogger(BagReader.class);
    
    public BagReader(Reader r) {
        super(r);
        this.fullRegex = Pattern.compile("^[A-Za-z\\-]*:");
    }
    
    public TagMetaElement readNextElement() {
        String line;
        TagMetaElement<String> payload;
        try {
            line = readLine();
            mark(79);
            StringBuilder fullElement = new StringBuilder(line);
            String extra;

            // Read ahead until we reach the next element
            while ( (extra = readLine()) != null) {
                Matcher m = fullRegex.matcher(extra);
                if ( m.find()) {
                    break;
                } else {
                    fullElement.append(" ");
                    fullElement.append(extra.trim());
                    // Update the mark
                    mark(79);
                }
            }
            payload = TagMetaElement.ParseBagMetaElement(fullElement.toString());
            reset();
        } catch (IOException ex) {
            log.error("{}", ex);
            return null;
        }
        
        return payload;
    }
}
