/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.chronopolis.bagit.util.BagMetaElement;
import org.chronopolis.bagit.util.PayloadOxum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Fill out bag-info fields
 *
 * @author shake
 */
public class BagInfoProcessor implements BagElementProcessor {
    // As defined by the bagit spec
    private final String bagInfoRE = "bag-info.txt";
    private final String oxumRE = "Payload-Oxum";
    private final Path bagInfoPath;
    private PayloadOxum payloadOxum;

    private final Logger log = LoggerFactory.getLogger(BagInfoProcessor.class);

    public BagInfoProcessor(Path bag) {
        this.bagInfoPath = bag.resolve(bagInfoRE);
        try {
            initBagInfo();
        } catch (IOException ex) {
            log.error("Could not read bag-info.txt {}", ex);
        }
    }

    private boolean exists() {
        return bagInfoPath.toFile().exists();
    }

    
    @Override
    public boolean valid() {
        String dataDir = "data";
        Boolean valid = true;
        PayloadOxum calculatedOxum = new PayloadOxum();
        Path bagPath = bagInfoPath.getParent();

        // Do we want to short circuit and return?
        // ....probably
        if ( !exists() ) {
            valid = false;
        }

        // Set up the oxums
        try {
            calculatedOxum.calculateOxum(bagPath.resolve(dataDir));
        } catch (IOException ex) {
            log.error("Could not read data directory to resolve payload\n{}", ex);
        }

        // And validate
        if (!payloadOxum.equals(calculatedOxum)) {
            valid = false;
        }


        return valid;
    }

    private void initBagInfo() throws IOException {
        BufferedReader reader = Files.newBufferedReader(bagInfoPath, Charset.forName("UTF-8"));

        String line;
        while ( (line = reader.readLine()) != null ) {
            if ( line.contains(oxumRE) ) {
                BagMetaElement payload = BagMetaElement.ParseBagMetaElement(line);
                switch (payload.getKey()) {
                    case "PayloadOxum":
                        payloadOxum.setFromString(payload.getValue());
                        break;
                    case "External-Identifier":
                        System.out.println("this is just here to be here");
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void create() {
        if ( !exists() ) {
            // full creation
        } else {
            // only what we need
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
