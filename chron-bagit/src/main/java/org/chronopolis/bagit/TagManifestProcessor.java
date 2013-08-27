/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.nio.file.Path;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class TagManifestProcessor extends ManifestProcessor {
    private final Logger log = LoggerFactory.getLogger(TagManifestProcessor.class);
    private final String manifestRE;
    
    public TagManifestProcessor(Path bag){
        super(bag);
        this.manifestRE = "tagmanifest-*.txt";
    }
    
    @Override
    public void create() {
        if (!bag.resolve("manifest-sha256.txt").toFile().exists()) {
            throw new RuntimeException("Must create manifest-sha256 before" +
                    "creating the tagmanifest");
        }
        log.info("Building tagmanifest for {}", bag);
        Path tagManifest = bag.resolve("tagmanifest-sha256.txt");
        HashMap<Path, String> tagDigests;
        
        // create the digests and write them
        tagDigests = digestDirectory(bag, true);
        writeDigests(tagManifest, tagDigests);
    }
}
