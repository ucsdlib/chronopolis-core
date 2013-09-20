/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Immutable variables call me Immutable Variable man
 * TODO: Make list of Validators for easy iteration
 *
 * @author shake
 */
public class Bag {
    public final Logger log = LoggerFactory.getLogger(Bag.class);
    
    // Files to check for in the bag
    public final String bagInfo = "bag-info.txt";
    public final String manifest = "manifest-*.txt";
    public final String tagManifest = "tagmanifest-*.txt";
    public final String charset = "UTF-8";
    public final String bagit = "bagit.txt";
    
    private final ExecutorService manifestRunner = Executors.newCachedThreadPool();
    private final Path bag;
    //private TokenWriterCallback callback = null;
    //private TokenRequestBatch batch = null;
    // Only SHA-256 digests in here
    // Could probably wrap this in a class and force it to check the size
    private Map<Path, String> validDigests;

    // All our various validators
    private BagInfoProcessor bagInfoValidator;
    private BagitProcessor bagitValidator;
    private ManifestProcessor manifestValidator;
    private Future<Boolean> validManifest;
    
    
    public Bag(Path toBag) {
        this.bag = toBag;
        //callback = new TokenWriterCallback(bag.getFileName().toString());

        bagInfoValidator = new BagInfoProcessor(toBag);
        bagitValidator = new BagitProcessor(toBag);
        manifestValidator = new ManifestProcessor(toBag);
        validManifest = manifestRunner.submit(manifestValidator);
        validDigests = manifestValidator.getValidDigests();
    }
    
    public static boolean ValidateBagFormat(Path toBag) {
        return true;
    }

    public Future<Boolean> getValidManifest() {
        return validManifest;
    }

    
    // Need to figure out exactly how we want to do these...
    public void checkBagitFiles() {
        getBagInfoValidator().valid();
        getBagitValidator().valid();
    }

    public void setValidDigests(Map<Path, String> validDigests) {
        this.validDigests = validDigests;
    }
    
    public Map<Path, String> getValidDigests(){
        return validDigests;
    }
    
    // Maybe we should push this into something else as well
    // Since it doesn't have to do much with validation, only runs after
    /*
    public Path getAceManifest(Path stage) throws InterruptedException, 
                                               IOException, 
                                               ExecutionException {
        if ( !validManifest.isDone() ) {
            throw new RuntimeException("Not finished validating manifest for bag");
        }
        if ( stage == null ) { 
            throw new RuntimeException("Stage cannot be null");
        }

        createIMSConnection();
        callback.setStage(stage);
        Future<Path> manifestPath = manifestRunner.submit(callback);
        
        log.info("Have {} entries", validDigests.entrySet().size());
        for ( Map.Entry<Path, String> entry : validDigests.entrySet()) {
            TokenRequest req = new TokenRequest();
            // We want the relative path for ACE so let's get it
            Path full = entry.getKey();
            Path relative = full.subpath(bag.getNameCount(), full.getNameCount());
             
            req.setName(relative.toString());
            req.setHashValue(entry.getValue());
            batch.add(req);
        }

        return manifestPath.get();
    }
    
    private void createIMSConnection() {
        IMSService ims;
        // TODO: Unhardcode
        ims = IMSService.connect("ims.umiacs.umd.edu", 443, true);
        batch = ims.createImmediateTokenRequestBatch("SHA-256",
                callback,
                1000,
                5000);
    }
    */

    /**
     * @return the bagInfoValidator
     */
    public BagInfoProcessor getBagInfoValidator() {
        return bagInfoValidator;
    }

    /**
     * @return the bagitValidator
     */
    public BagitProcessor getBagitValidator() {
        return bagitValidator;
    }

    /**
     * @return the manifestValidator
     */
    public ManifestProcessor getManifestValidator() {
        return manifestValidator;
    }
    
}
