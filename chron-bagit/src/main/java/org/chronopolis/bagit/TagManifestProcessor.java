/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class TagManifestProcessor extends ManifestProcessor {
    private final Logger log = LoggerFactory.getLogger(TagManifestProcessor.class);
    
    public TagManifestProcessor(Path bag){
        super(bag, "tagmanifest-*.txt");
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
    
    @Override
    public HashSet<Path> getOrphans() {
        final HashSet<Path> orphans = new HashSet<>();
        System.out.println("Size of our registered digests: " + registeredDigests.size());
        try {
            Files.walkFileTree(bag, new SimpleFileVisitor<Path> () {
                @Override
                public FileVisitResult preVisitDirectory(Path p,
                                                         BasicFileAttributes attrs) {
                    if ( attrs.isDirectory() ) {
                        if (p.endsWith("data")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                // This is the same as the manifest processor... 
                // wonder if there is a good way to combine them
                public FileVisitResult visitFile(Path p, 
                                                 BasicFileAttributes attrs) {
                    if ( attrs.isRegularFile() ) { 
                        if (!registeredDigests.containsKey(p) && !manifests.contains(p)) {
                            log.debug("Adding orphan: {}", p);
                            orphans.add(p);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
                
            });
        } catch (IOException ex) {
            log.error("Error getting orphans for tag manifest {}", ex);
        }
        return orphans;
    }
}
