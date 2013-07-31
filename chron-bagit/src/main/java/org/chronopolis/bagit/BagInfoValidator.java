/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.nio.file.Path;
import org.chronopolis.bagit.util.PayloadOxum;

/**
 * TODO: Fill out bag-info fields
 *
 * @author shake
 */
public class BagInfoValidator implements Validator {
    // As defined by the bagit spec
    private final String bagInfoRE = "bag-info.txt";
    private final String oxumRE = "Payload-Oxum";
    private final Path bagInfoPath;
    private PayloadOxum payloadOxum;

    public BagInfoValidator(Path bag) {
        this.bagInfoPath = bag.resolve(bagInfoRE);
    }

    private boolean exists() {
        return bagInfoPath.toFile().exists();
    }

    
    @Override
    public boolean isValid() {
        String dataDir = "data";
        Boolean valid = true;
        // Do we want to short circuit and return?
        if ( !exists() ) {
            valid = false;
        }
        Path bagPath = bagInfoPath.getParent();


        return valid;
    }
    
}
