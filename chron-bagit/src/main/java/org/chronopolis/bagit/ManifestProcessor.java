/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.security.DigestInputStream;
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
 *  TODO: If manifest is made from MD5 digests, convert them to SHA-256
 *
 * @author shake
 */
public class ManifestProcessor implements Callable<Boolean>, TagProcessor {
    private final Logger log = LoggerFactory.getLogger(ManifestProcessor.class);
    private final String manifestRE = "*manifest-*.txt";
    private final Path bag;
    
    // Hm... do we really want/need both?
    private HashMap<Path, String> registeredDigests = new HashMap<>();
    private HashMap<Path, String> validDigests = new HashMap<>();
    private HashSet<ManifestError> corruptedFiles = new HashSet<>();
    private HashSet<Path> manifests = new HashSet<>();
    private MessageDigest md;
    private boolean valid;
    
    public ManifestProcessor(Path bag) {
        try {
            this.md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            log.error("Error initializing default digest: {}", ex);
        }
        this.bag = bag;
        
    }
    
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
    private void populateDigests() throws IOException, NoSuchAlgorithmException {
        for ( Path toManifest : manifests) {
            String digestType = toManifest.getFileName().toString().split("-")[1];
            // There's still the .txt on the end so just match
            // Actually I could do starts with
            // or strip the .txt but that would create a new object
            // Also need to move these out
            if ( digestType.contains("sha256")) {
                md = MessageDigest.getInstance("SHA-256");
            }
            
            try (BufferedReader reader = Files.newBufferedReader(toManifest,
                    Charset.forName("UTF-8"))) {
                String line;
                while ( (line = reader.readLine()) != null) {
                    String[] split = line.split("\\s+", 2);
                    String digest = split[0];
                    String file = split[1];
                    log.debug("Registering digest for {} : {} ", file, digest);
                    registeredDigests.put(Paths.get(bag.toString(), file), digest);
                }
            }
        }
        
    }
    
    // May want to break this out so other classes can do digests easily if needed
    private byte[] doDigest(Path path) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(path.toFile());
        try (DigestInputStream dis = new DigestInputStream(fis, md)) {
            dis.on(true);
            int bufferSize = 1048576; // should move into some type of settings
            byte []buf = new byte[bufferSize];
            while ( dis.read(buf) != -1) {
                // spin
            }
        }
        return md.digest();
    }
    
    @Override
    public Boolean call() throws Exception {
        valid = true;
        findManifests();
        populateDigests();
        
        if ( md == null ) {
            System.out.println("Digest is null -- probably no match above");
            md = MessageDigest.getInstance("SHA-256");
        }
        
        // And check the digests
        for ( Map.Entry<Path, String> entry : registeredDigests.entrySet()) {
            Path file = entry.getKey();
            String registeredDigest = entry.getValue();
            byte[] calculatedDigest;
            
            md.reset();
            
            // catch all
            // some duplicated code but whatever
            if ( !md.getAlgorithm().equals("SHA-256") ) {
                calculatedDigest = DigestUtil.convertToSHA256(file, md, registeredDigest);
                if ( calculatedDigest == null ) {
                    corruptedFiles.add(new ManifestError(file, registeredDigest, null));
                    valid = false;
                } else {
                    String digest = DigestUtil.byteToHex(calculatedDigest);
                    validDigests.put(file, digest);
                }
            } else {
                calculatedDigest = DigestUtil.doDigest(file, md);
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
        DirectoryStream.Filter filter = new DirectoryStream.Filter<Path>() {
            @Override
            public boolean accept(Path t) throws IOException {
                return !registeredDigests.containsKey(t)
                        && !t.toFile().isDirectory();
            }
        };
        
        DirectoryStream<Path> dStream = Files.newDirectoryStream(bag, filter);
        for ( Path p : dStream ) {
            System.out.println(p);
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
    
    public HashSet<ManifestError> getCorruptedFiles() {
        return corruptedFiles;
    }
    
    @Override
    public boolean valid() {
        while ( Thread.currentThread().isAlive()) {
        }
        return valid;
    }
    
    @Override
    public void create() {
        // teeangemutantninjaturtles
        // hard codin like a maw fucka
        HashMap<Path, String> dataDigests; 
        HashMap<Path, String> tagDigests; 
        Path manifest = bag.resolve("manifest-sha256.txt");
        Path tagManifest = bag.resolve("tagmanifest-sha256.txt");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            log.error("Could not get sha-256 algorithm for digesting {}", ex);
        }
        log.info("Building digests for bag {}", bag);
        // First digest everything in the data dir
        dataDigests = digestDirectory(bag.resolve("data"), false);
        
        // Now write our manifest
        writeDigests(manifest, dataDigests);
        
        // Now we can do the tag files (yay)
        tagDigests = digestDirectory(bag, true);
        
        // And write the tagmanifest
        writeDigests(tagManifest, tagDigests);
    }

    private void writeDigests(Path manifest, HashMap<Path, String> digests) {
        try (BufferedWriter writer = Files.newBufferedWriter(manifest,
                Charset.forName("UTF-8"),
                StandardOpenOption.CREATE_NEW)) {
            for ( Map.Entry<Path, String> entry : digests.entrySet()) {
                writer.append(entry.getValue() + "  ");
                writer.append(bag.relativize(entry.getKey()).toString());
                writer.newLine();
            }
        } catch (IOException ex) {
            log.error("Error writing {}:\n{}", manifest, ex);
        }
    }

    private HashMap<Path, String> digestDirectory(Path dir, final boolean skipData) {
        final HashMap<Path, String> digests = new HashMap<>();
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
                    public FileVisitResult visitFile(Path p, BasicFileAttributes attrs) {
                        if ( attrs.isRegularFile() ) {
                            log.debug("visiting {}", p);
                            byte[] digest = DigestUtil.doDigest(p, md);
                            digests.put(p, DigestUtil.byteToHex(digest));
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        } catch (IOException ex) {
            log.error("Error walking file tree for {}:\n{}", dir, ex);
        }

        return digests;
    }
    
    public class ManifestError {
        Path p;
        String expected;
        String found;
        
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