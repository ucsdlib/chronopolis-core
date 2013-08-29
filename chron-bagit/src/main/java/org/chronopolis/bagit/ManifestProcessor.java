/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import org.chronopolis.bagit.util.DigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: If manifest is made from MD5 digests, convert them to SHA-256
 *
 * @author shake
 */
public class ManifestProcessor implements Callable<Boolean> {
    private final Logger log = LoggerFactory.getLogger(ManifestProcessor.class);
    protected final String manifestRE;
    protected final Path bag;
    
    // Can contain any type of digest (will probably only be either md5 or sha256)
    protected HashMap<Path, String> registeredDigests = new HashMap<>();

    // Contains only sha256 digests
    private HashMap<Path, String> validDigests = new HashMap<>();
    private HashSet<ManifestError> corruptedFiles = new HashSet<>();
    protected HashSet<Path> manifests = new HashSet<>();
    private MessageDigest md;
    private boolean valid;
    
    public ManifestProcessor(Path bag) {
        try {
            this.md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            log.error("Error initializing default digest: {}", ex);
        }

        this.manifestRE = "manifest-*.txt";
        this.bag = bag;
    }

    public ManifestProcessor(Path bag, String manifestRE) {
        try {
            this.md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            log.error("Error initializing default digest: {}", ex);
        }

        this.bag = bag;
        this.manifestRE = manifestRE;
    }
    
    /**
     * TODO: This could be bad if we have manifests of differing digests....
     * 
     * @throws IOException 
     */
    private void findManifests() throws IOException {
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(bag,
                                                                        manifestRE)) {
            for ( Path p : directory) {
                manifests.add(p);
            }
        }
    }
    
    // TODO: Add the digests of all missed files
    //       Something like for ( Path p : toBag if p not in validDigests )
    private MessageDigest populateDigests() throws IOException, NoSuchAlgorithmException {
        MessageDigest manifestDigest = null;
        for ( Path toManifest : manifests) {
            String digestType = toManifest.getFileName().toString().split("-")[1];
            // There's still the .txt on the end so just match
            // Actually I could do starts with
            // or strip the .txt but that would create a new object
            if ( digestType.contains("sha256")) {
                manifestDigest = MessageDigest.getInstance("SHA-256");
            } else if ( digestType.contains("md5")) {
                manifestDigest = MessageDigest.getInstance("MD5");
            } else if ( digestType.contains("sha1")) {
                manifestDigest = MessageDigest.getInstance("SHA-1");
            }
            
            try (BufferedReader reader = Files.newBufferedReader(toManifest,
                    Charset.forName("UTF-8"))) {
                String line;
                while ( (line = reader.readLine()) != null) {
                    String[] split = line.split("\\s+", 2);
                    String digest = split[0];
                    String file = split[1];
                    log.debug("Registering digest for {} : {} ", file, digest);
                    // This is where things could get bad for differing digests
                    // because when we validate it will go boom
                    registeredDigests.put(Paths.get(bag.toString(), file), digest);
                }
            }
        }

        return manifestDigest;
    }
    
    @Override
    public Boolean call() throws Exception {
        valid = true;
        findManifests();
        // registered digest
        MessageDigest rd = populateDigests();
        
        if ( manifests.isEmpty() ) { 
            valid = false;
        }
        
        if ( rd == null ) {
            rd = MessageDigest.getInstance("SHA-256");
        }
        
        // And check the digests
        for ( Map.Entry<Path, String> entry : registeredDigests.entrySet()) {
            Path file = entry.getKey();
            String registeredDigest = entry.getValue();
            byte[] calculatedDigest;
            
            rd.reset();
            
            // catch all
            // some duplicated code but whatever
            if ( !rd.getAlgorithm().equals("SHA-256") ) {
                calculatedDigest = DigestUtil.convertToSHA256(file, rd, registeredDigest);
                if ( calculatedDigest == null ) {
                    corruptedFiles.add(new ManifestError(file, registeredDigest, null));
                    valid = false;
                } else {
                    String digest = DigestUtil.byteToHex(calculatedDigest);
                    validDigests.put(file, digest);
                }
            } else {
                calculatedDigest = DigestUtil.doDigest(file, rd);
                String digest = DigestUtil.byteToHex(calculatedDigest);
                if ( !registeredDigest.equals(digest)) {
                    corruptedFiles.add(new ManifestError(file, registeredDigest, digest));
                    valid = false;
                } else {
                    validDigests.put(file, entry.getValue());
                }
            }
        }
        
        // I guess we're just assuming that the digests are valid...
        // Actually if the manifest-alg.txt is invalid the tagmanifest will catch it
        // Also we probably only want to do this if it is valid
        // Also we probably don't need this anymore
        DirectoryStream.Filter filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path t) throws IOException {
                return !registeredDigests.containsKey(t)
                        && !t.toFile().isDirectory();
            }
        };
        
        DirectoryStream<Path> dStream = Files.newDirectoryStream(bag, filter);
        for ( Path p : dStream ) {
            md.reset();
            byte[] manifestDigest = DigestUtil.doDigest(p, md);
            String digest = DigestUtil.byteToHex(manifestDigest);
            validDigests.put(p, digest);
        }
        
        return valid;
    }
    
    /**
     * @return the validDigests
     */
    public HashMap<Path, String> getValidDigests() {
        return validDigests;
    }
    
    /**
     * 
     * @return All errors found when checking against the manifest
     */
    public HashSet<ManifestError> getCorruptedFiles() {
        return corruptedFiles;
    }
    
    /**
     * Build and write the manifest-sha256.txt.
     * Will fail if the files exist, and blah blah blah
     * 
     */
    public void create() {
        // teeangemutantninjaturtles
        // hard codin like a maw fucka
        HashMap<Path, String> dataDigests; 
        Path manifest = bag.resolve("manifest-sha256.txt");
        if ( manifest.toFile().exists() ) {
            log.error("Manifest already exists, if you'd like to create yet SOL");
            throw new RuntimeException("Manifest already exists");
        }
        log.info("Building digests for bag {}", bag);
        // First digest everything in the data dir
        dataDigests = digestDirectory(bag.resolve("data"), false);
        
        // Now write our manifest
        writeDigests(manifest, dataDigests);
    }

    /**
     * This method will fail if the file already exists, and makes no attempt
     * to append missing files to the manifest
     * 
     * 
     * @param manifest the path of the manifest file to create
     * @param digests the digests to write to the file 
     */
    protected void writeDigests(Path manifest, HashMap<Path, String> digests) {
        try (BufferedWriter writer = Files.newBufferedWriter(manifest,
                Charset.forName("UTF-8"),
                StandardOpenOption.CREATE_NEW)) {
            for ( Map.Entry<Path, String> entry : digests.entrySet()) {
                writer.append(entry.getValue() + "  ");
                writer.append(bag.relativize(entry.getKey()).toString());
                writer.newLine();
            }
        } catch (IOException ex) {
            log.error("Error writing {}: {}", manifest, ex);
        }
    }

    /**
     *  Digest an entire directory, returning a map of the files and their 
     *  respective digests
     * 
     * @param dir The path which points to the root of the bag
     * @param skipData boolean to skip the data directory found in bags 
     * @return Map of digests
     */
    protected HashMap<Path, String> digestDirectory(Path dir, final boolean skipData) {
        final HashMap<Path, String> digests = new HashMap<>();
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            log.error("Could not initialized intance for SHA-256 digest: {}", ex);
        }
        try {
            Files.walkFileTree(bag, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path p,
                                                         BasicFileAttributes attrs) {
                    if ( skipData && p.endsWith("data") ) {
                        log.info("Skipping data directory");
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                        return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path p, 
                                                 BasicFileAttributes attrs) {
                    if ( attrs.isRegularFile() ) {
                        log.debug("visiting {}", p);
                        byte[] digest = DigestUtil.doDigest(p, md);
                        digests.put(p, DigestUtil.byteToHex(digest));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.error("Error walking file tree for {}: {}", dir, ex);
        }

        return digests;
    }

    /**
     * Get all the files which did not have registered digests
     * 
     * @return Set of files which are orphans
     */
    public HashSet<Path> getOrphans() {
        final HashSet<Path> orphans = new HashSet<>();
        try {
            Files.walkFileTree(bag.resolve("data"), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path p, BasicFileAttributes a) {
                    if ( a.isRegularFile() && !registeredDigests.containsKey(p) ) {
                        log.debug("Adding orphan: {}", p);
                        orphans.add(p);
                    } 
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.error("Could not build orphans, error walking file tree {}", ex);
        }
        
        return orphans;
    }
    
    /**
     * Class to encapsulate errors found in a manifest
     * 
     */
    public class ManifestError {
        Path p;
        String expected;
        String found;
        
        /**
         * Create a new ManifestError object 
         * 
         * @param p The path of the file
         * @param expected The expected digest from the manifest
         * @param found The digest found when checking the file
         */
        public ManifestError(Path p, String expected, String found) {
            this.p = p;
            this.expected = expected;
            this.found = found;
        }
        
        public Path getPath() {
            return p;
        }
        
        public String getExpected() {
            return expected;
            
        }
        
        public String getFound() {
            return found;
        }
    }
}