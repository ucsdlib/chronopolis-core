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
    private final Path bagitPath;
    
    // Why not just make these TagMetaElements?
    private TagMetaElement<String> bagVersion;
    private TagMetaElement<String> tagEncoding;
    
    public BagitProcessor(Path bag) {
        this.bagitPath = bag.resolve(bagitRE);
        init();
    }
    
    private boolean exists() {
        return bagitPath.toFile().exists();
    }
    
    private void init() {
        if (exists()) {
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
                            break;
                    }
                }
                
            }catch (IOException ex) {
                log.error("Error reading bagit.txt\n{}", ex);
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
            bagVersion = new TagMetaElement(bagVersionRE, currentBagVersion, false);
        }
        //elements.add(new TagMetaElement(bagVersionRE, bagVersion));
        //elements.add(new TagMetaElement(tagFileRE, tagFileEncoding));
        elements.add(bagVersion);
        elements.add(tagEncoding);
        BagFileWriter.write(bagitPath, elements, StandardOpenOption.CREATE);
    }
    
    private void partialCreate() {
        // Since UTF-8 is defined fr the Tag-File, this is the only thing we
        // need to check
        if ( bagVersion == null ){
            bagVersion = new TagMetaElement(bagVersionRE, currentBagVersion, false);
        }
        String version = bagVersionAsString();
        
        try {
            BufferedWriter writer = Files.newBufferedWriter(bagitPath,
                    Charset.forName(tagFileEncoding),
                    StandardOpenOption.WRITE);
            writer.write(version, 0, version.length());
        } catch (IOException ex) {
            log.error("Error writing bagit.txt\n{}",ex);
        }
        
    }
    
    @Override
    public void create() {
        if ( exists() ) {
            partialCreate();
        } else {
            fullCreate();
        }
    }
    
    // By using a StringBuilder, we don't have to worry about null strings
    public String bagVersionAsString() {
        TagMetaElement element = new TagMetaElement(bagVersionRE, bagVersion, true);
        return element.toString();
    }
    
    public String tagFileEncodingToString() {
        TagMetaElement element = new TagMetaElement(tagFileRE, tagFileEncoding, true);
        return element.toString();
    }

}
