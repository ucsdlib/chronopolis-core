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
import java.util.logging.Logger;
import org.chronopolis.bagit.util.BagFileWriter;
import org.chronopolis.bagit.util.TagMetaElement;

/**
 * Validate and create bagit.txt files
 *
 * @author shake
 */
public class BagitProcessor implements TagProcessor {
    // As defined in the bagit spec
    private final String bagitRE = "bagit.txt";
    private final String bagVersionRE = "BagIt-Version";
    private final String tagFileRE = "Tag-File-Character-Encoding";
    private final String currentBagVersion = "0.97";
    private final Path bagitPath;

    // Why not just make these BagMetaElements?
    private final String tagFileEncoding = "UTF-8";
    private String bagVersion;

    public BagitProcessor(Path bag) {
        this.bagitPath = bag.resolve(bagitRE);
    }

    private boolean exists() {
        return bagitPath.toFile().exists();
    }

    @Override
    public boolean valid() {
        boolean valid = exists();
        try {
            try (BufferedReader reader = Files.newBufferedReader(bagitPath, Charset.forName("UTF-8"))) {
                String line;
                while ( (line = reader.readLine()) != null ) {
                    TagMetaElement<String> element = TagMetaElement.ParseBagMetaElement(line);

                    // TODO: Maybe make it it's own method so I can just say
                    // valid = parseLine(tmp)
                    switch (element.getKey()) {
                        case bagVersionRE:
                            bagVersion = element.getValue();
                            break;
                        case tagFileRE:
                            // Make sure we're using UTF-8
                            if ( !element.getValue().equals(tagFileEncoding)) {
                                valid = false;
                            }
                            break;
                        default:
                            valid = false;
                            break;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(BagitProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        return valid;
    }

    /**
     * @return the bagVersion
     */
    public String getBagVersion() {
        return bagVersion;
    }

    /**
     * @return the tagFileEncoding
     */
    public String getTagFileEncoding() {
        return tagFileEncoding;
    }

    private void fullCreate() {
        List<TagMetaElement> elements = new ArrayList<>();
        if (null == bagVersion || bagVersion.isEmpty()) {
            bagVersion = currentBagVersion;
        }
        elements.add(new TagMetaElement(bagVersionRE, bagVersion));
        elements.add(new TagMetaElement(tagFileRE, tagFileEncoding));
        BagFileWriter.write(bagitPath, elements, StandardOpenOption.CREATE);
    }

    private void partialCreate() {
        // Since UTF-8 is defined fr the Tag-File, this is the only thing we 
        // need to check
        if ( bagVersion == null ){ 
            bagVersion = currentBagVersion;
        }
        String version = bagVersionAsString();

        try {
            BufferedWriter writer = Files.newBufferedWriter(bagitPath, 
                                          Charset.forName(tagFileEncoding), 
                                          StandardOpenOption.WRITE);
            writer.write(version, 0, version.length());
        } catch (IOException ex) {
            Logger.getLogger(BagitProcessor.class.getName()).log(Level.SEVERE, null, ex);
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
        TagMetaElement element = new TagMetaElement(bagVersionRE, bagVersion);
        return element.toString();
    }

    public String tagFileEncodingToString() {
        TagMetaElement element = new TagMetaElement(tagFileRE, tagFileEncoding);
        return element.toString();
    }

}
