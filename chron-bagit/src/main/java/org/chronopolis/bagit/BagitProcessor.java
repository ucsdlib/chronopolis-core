/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.chronopolis.bagit.util.BagFileWriter;
import org.chronopolis.bagit.util.TagMetaElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate and create bagit.txt files
 *
 * @author shake
 */
public class BagitProcessor implements TagProcessor {
    private final Logger log = LoggerFactory.getLogger(BagitProcessor.class);
    
    // As defined in the bagit spec
    private final String bagitRE = "bagit.txt";
    private final String bagVersionRE = "BagIt-Version";
    private final String tagFileRE = "Tag-File-Character-Encoding";
    private final String currentBagVersion = "0.97";
    private final String tagFileEncoding = "UTF-8";

    // Everything else
    private Path bagitPath;
    private TagMetaElement<String> bagVersion;
    private TagMetaElement<String> tagEncoding;
    private int unknownTags;
    private boolean initialized;
    
    public BagitProcessor(Path bag) {
        this.unknownTags = 0;
        this.bagitPath = bag.resolve(bagitRE);
        this.initialized = false;
        init();
    }
    
    private boolean exists() {
        return bagitPath.toFile().exists();
    }
    
    /**
     * Set the bagit path and then initialize if it exists
     * 
     * @param bagit The path to the bagit.txt file
     */
    public void setBagitPath(Path bagit) {
        this.bagitPath = bagit;
        init();
    }
    
    private void init() {
        if (exists()) {
            // Reset in case we have already initialized
            unknownTags = 0;
            try (BufferedReader reader = Files.newBufferedReader(bagitPath, Charset.forName("UTF-8"))) {
                String line;
                while ( (line = reader.readLine()) != null ) {
                    TagMetaElement<String> element = TagMetaElement.ParseBagMetaElement(line);
                    
                    switch (element.getKey()) {
                        case bagVersionRE:
                            bagVersion = element;
                            break;
                        case tagFileRE:
                            tagEncoding = element;
                            break;
                        default:
                            // We should have some flag that says we had unknown
                            // value
                            unknownTags++;
                            break;
                    }
                }
                initialized = true; 
            }catch (IOException ex) {
                log.error("Error reading bagit.txt: {}", ex);
            }
        }
    }
    
    @Override
    public boolean valid() {
        boolean valid = exists();
        if ( bagVersion == null || 
             bagVersion.getValue() == null || 
             !bagVersion.getValue().equals(currentBagVersion) ) { 
            valid = false;
        }
        if ( tagEncoding == null || 
             tagEncoding.getValue() == null ||
             !tagEncoding.getValue().equals(tagFileEncoding)) { 
            valid = false;
        }
        if ( unknownTags > 0 ) {
            valid = false;
        }
        return valid;
    }
    
    /**
     * @return the bagVersion
     */
    public String getBagVersion() {
        return bagVersion.getValue();
    }
    
    /**
     * @return the tagFileEncoding
     */
    public String getTagFileEncoding() {
        return tagFileEncoding;
    }
    
    private void fullCreate() {
        List<TagMetaElement> elements = new ArrayList<>();
        if ( bagVersion == null ) {
            bagVersion = new TagMetaElement(bagVersionRE, 
                                            currentBagVersion, 
                                            false);
        }
        elements.add(bagVersion);
        elements.add(tagEncoding);
        BagFileWriter.write(bagitPath, elements, StandardOpenOption.CREATE);
    }
    
    private void partialCreate() {
        // Since UTF-8 is defined fr the Tag-File, this is the only thing we
        // need to check
        if ( bagVersion == null ){
            bagVersion = new TagMetaElement(bagVersionRE, 
                                            currentBagVersion, 
                                            false);
        }
        String version = bagVersionAsString();
        
        try {
            BufferedWriter writer = Files.newBufferedWriter(bagitPath,
                    Charset.forName(tagFileEncoding),
                    StandardOpenOption.WRITE);
            writer.write(version, 0, version.length());
        } catch (IOException ex) {
            log.error("Error writing bagit.txt: {}",ex);
        }
        
    }
    
    @Override
    public void create() {
        if ( !initialized ) { 
            init();
        }

        if ( bagVersion == null ) {
            bagVersion = new TagMetaElement(bagVersionRE, currentBagVersion, false);
        }
        if ( tagEncoding == null ) {
            tagEncoding = new TagMetaElement(tagFileRE, tagFileEncoding, false);
        }

        // I'm a bum so let's overwrite it
        // Maybe I'll make an append which solely appends
        try (BufferedWriter writer = Files.newBufferedWriter(bagitPath, 
                                                             Charset.forName("UTF-8"), 
                                                             StandardOpenOption.CREATE)) {
            writer.write(bagVersion.toString());
            writer.newLine();
            writer.write(tagEncoding.toString());
        } catch (IOException ex) {
            log.error("Error writing bagit.txt: {}", ex);
        }
    }
    
    // By using a StringBuilder, we don't have to worry about null strings
    public String bagVersionAsString() {
        return bagVersion.toString();
    }
    
    public String tagFileEncodingToString() {
        return tagEncoding.toString();
    }

}
