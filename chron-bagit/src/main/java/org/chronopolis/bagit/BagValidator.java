/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import edu.umiacs.ace.ims.api.IMSService;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.OpenOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.api.TokenRequestBatch;
import edu.umiacs.ace.ims.ws.TokenRequest;

/**
 * Immutable variables call me Immutable Variable man
 *
 * @author shake
 */
public class BagValidator {
    
    public final String bagInfo = "bag-info.txt";
    public final String manifest = "manifest-*.txt";
    public final String charset = "UTF-8";
    public final String bagit = "bagit.txt";
    private final ExecutorService manifestRunner = Executors.newCachedThreadPool();
    private ManifestValidator validator;
    private final Path toBag;
    private Future<Boolean> f;
    private TokenWriterCallback callback = null;
    
    
    public BagValidator(Path toBag) {
        this.toBag = toBag;
        validator = new ManifestValidator();
        callback = new TokenWriterCallback();
        f = manifestRunner.submit(validator);
    }
    
    public static boolean ValidateBagFormat(Path toBag) {
        return true;
    }
    
    public Future<Boolean> getFuture() {
        return f;
    }
    
    private String byteToHex(byte[] bytes) {
        StringBuilder str = new StringBuilder(new BigInteger(1, bytes).toString(16));
        if ( str.length() < bytes.length*2) {
            for ( int i=0; i < bytes.length*2-str.length(); i++) {
                str.insert(0, "0");
            }
        }
        return str.toString();
    }
    
    // This goes all over the 80 character limit... trying to decide what to do
    // about that
    private class ManifestValidator implements Callable<Boolean> {
        HashMap<Path, String> registeredDigests = new HashMap<>();
        HashSet<Path> manifests = new HashSet<>();
        MessageDigest md;
        TokenRequestBatch batch = null;
        
        private void findManifests() throws IOException {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(toBag, manifest)) {
                for ( Path p : directory) {
                    manifests.add(p);
                }
            }
        }
        
        private void populateDigests() throws IOException, NoSuchAlgorithmException {
            for ( Path toManifest : manifests) {
                String digestType = toManifest.getFileName().toString().split("-")[1];
                // There's still the .txt on the end so just match
                if ( digestType.matches("sha256") ) {
                    System.out.println("MATCH (dot calm)");
                    md = MessageDigest.getInstance("SHA-256");
                }
                
                try (BufferedReader reader = Files.newBufferedReader(toManifest, Charset.forName("UTF-8"))) {
                    String line = null;
                    while ( (line = reader.readLine()) != null) {
                        String[] split = line.split("\\s+", 2);
                        String digest = split[0];
                        String file = split[1];
                        registeredDigests.put(Paths.get(toBag.toString(), file), digest);
                    }
                }
            }
        }
        
        @Override
        public Boolean call() throws Exception {
            boolean valid = true;
            findManifests();
            populateDigests();

            if ( md == null ) {
                System.out.println("Digest is null -- probably no match above");
                md = MessageDigest.getInstance("SHA-256");
            }

            createIMSConnection();

            // And check the digests
            // There really has to be a better way to do this... default block
            // size for DigestInputStream is 8 so I'm using 1024 instead.
            for ( Map.Entry<Path, String> entry : registeredDigests.entrySet()) {
                Path toFile = entry.getKey();
                String registeredDigest = entry.getValue();
                
                FileInputStream fis = new FileInputStream(toFile.toFile());
                DigestInputStream dis = new DigestInputStream(fis, md);
                dis.on(true);
                int off = 0;
                int toread = (1024 <= dis.available() ? 1024 : dis.available());
                byte []buf = new byte[dis.available()];
                while ( dis.read(buf, off, toread) > 0) {
                    toread = (1024 <= dis.available() ? 1024 : dis.available());
                }
                byte[] calculatedDigest = md.digest();
                String digest = byteToHex(calculatedDigest);
                valid = registeredDigest.equals(digest);
            }
            
            if ( valid ) {
                for ( Map.Entry<Path, String> entry : registeredDigests.entrySet()) {
                    TokenRequest req = new TokenRequest();
                    req.setName(entry.getKey().getFileName().toString());
                    req.setHashValue(entry.getValue());
                    batch.add(req);
                }
            }

            return valid;
        }

        private void createIMSConnection() {
            IMSService ims;
            ims = IMSService.connect("ims.umiacs.umd.edu", 443, true);
            batch = ims.createImmediateTokenRequestBatch(md.getAlgorithm(), 
                                                         callback, 
                                                         1000, 
                                                         5000);
        }
        
    }
    
    public void ValidateManifestAndGetTokens() throws IOException {
        //Filter f = new Filter();
        HashSet<Path> manifests = new HashSet<>();
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(toBag, manifest)) {
            for ( Path p : directory) {
                manifests.add(p);
            }
        }
        
        for ( Path toManifest : manifests ) {
            BufferedReader br = Files.newBufferedReader(toBag, Charset.forName(charset));
            String line = null;
            while ( (line = br.readLine()) != null) {
                String[] split = line.split(" ");
                String digest = split[0];
                String file = split[1];
            }
        }
        
    }
    
}
