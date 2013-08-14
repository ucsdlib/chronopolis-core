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
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.chronopolis.bagit.util.BagFileWriter;
import org.chronopolis.bagit.util.TagMetaElement;
import org.chronopolis.bagit.util.PayloadOxum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validate and create bag-info.txt
 * 
 * TODO: Fill out bag-info fields
 *
 * @author shake
 */
public class BagInfoProcessor implements TagProcessor {
    // As defined by the bagit spec
    private final String bagInfoRE = "bag-info.txt";
    private final String bagSizeRE = "Bag-Size";
    private final String baggingDateRE = "Bagging-Date";
    private final String oxumRE = "Payload-Oxum";
    private final Path bagInfoPath;
    private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
    private PayloadOxum payloadOxum;
    private TagMetaElement bagSize;
    private TagMetaElement baggingDate;
    
    private boolean initialized;

    private final Logger log = LoggerFactory.getLogger(BagInfoProcessor.class);

    public BagInfoProcessor(Path bag) {
        payloadOxum = new PayloadOxum();
        this.bagInfoPath = bag.resolve(bagInfoRE);
        initialized = false;
        initBagInfo();
    }

    private boolean exists() {
        return bagInfoPath.toFile().exists();
    }

    
    /* The only thing we really need to validate against is the Payload Oxum
     * So we walk the file tree and make sure it's there 
     * 
     */
    @Override
    public boolean valid() {
        String dataDir = "data";
        Boolean valid = true;
        PayloadOxum calculatedOxum = new PayloadOxum();
        Path bagPath = bagInfoPath.getParent();
        System.out.println("SADFJASDFJ");

        // Do we want to short circuit and return?
        // ....probably
        if ( !exists() ) {
            valid = false;
        }

        if (payloadOxum == null || payloadOxum.getNumFiles() == 0 || 
            payloadOxum.getOctetCount() == 0) {
            valid = false;
        } else {
            // Set up the oxums
            try {
                System.out.println("Setting up our payload");
                calculatedOxum.calculateOxum(bagPath.resolve(dataDir));
            } catch (IOException ex) {
                log.error("Could not read data directory to resolve payload\n{}", ex);
                valid = false;
            }

            // And validate
            if (!payloadOxum.equals(calculatedOxum)) {
                valid = false;
            }
        }

        return valid;
    }

    private void initBagInfo() {
        BufferedReader reader = null;
        try {
            reader = Files.newBufferedReader(bagInfoPath, Charset.forName("UTF-8"));
        } catch (IOException ex) {
            log.info("No bag-info.txt found \n {}", ex);
            initialized = true;
            return;
        }

        String line;
        try {
            while ( (line = reader.readLine()) != null ) {
                TagMetaElement<String> payload = TagMetaElement.ParseBagMetaElement(line);
                switch (payload.getKey()) {
                    case oxumRE:
                        payloadOxum.setFromString(payload.getValue());
                        break;
                    case bagSizeRE:
                        bagSize = TagMetaElement.ParseBagMetaElement(line);
                        break;
                    case baggingDateRE:
                        baggingDate = TagMetaElement.ParseBagMetaElement(line);
                        break;
                    case "External-Identifier":
                        System.out.println("this is just here to be here");
                        break;
                    default:
                        log.error("Unexpected value in bag-info.txt");
                        break;
                }
            }
        } catch (IOException ex) {
            log.error("Error reading bag-info.txt \n {}", ex);
        }
        initialized = true;
    }

    private void fullCreate() {

        List<TagMetaElement> metaElements = new ArrayList<>();
        Path payloadPath = bagInfoPath.getParent().resolve("data");
        bagSize = new TagMetaElement(bagSizeRE, 0);
        baggingDate = new TagMetaElement(baggingDateRE, 
                                         dateFormat.format(new Date()));
        try {
            payloadOxum.calculateOxum(payloadPath);
        } catch (IOException ex) {
            log.error("Error creating payloadOxum for path {} \n {}",
                      payloadPath,
                      ex);
        }
        metaElements.add(payloadOxum.toBagMetaElement());
        metaElements.add(bagSize);
        metaElements.add(baggingDate);
        BagFileWriter.write(bagInfoPath, 
                            metaElements, 
                            StandardOpenOption.CREATE_NEW);
    }

    // Do we want to rewrite the whole file or figure out where in the file we 
    // need to add elements
    private void partialCreate() {
        List<TagMetaElement> metaElements = new ArrayList<>();
        if ( payloadOxum.getNumFiles() == 0 || payloadOxum.getOctetCount() == 0) {
            Path payloadPath = bagInfoPath.getParent().resolve("data");
            try {
                payloadOxum.calculateOxum(payloadPath);
            } catch (IOException ex) {
                log.error("Error calculating payload Oxum {} ", ex);
            }
            metaElements.add(payloadOxum.toBagMetaElement());

        }

    }

    @Override
    public void create() {
        if ( !initialized ) {
            initBagInfo();
        }
        if ( exists() ) {
            partialCreate();
        } else {
            fullCreate();
        }
    }
    
}
