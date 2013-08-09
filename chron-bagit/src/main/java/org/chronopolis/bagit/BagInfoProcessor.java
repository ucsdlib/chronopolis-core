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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.chronopolis.bagit.util.BagMetaElement;
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
public class BagInfoProcessor implements BagElementProcessor {
    // As defined by the bagit spec
    private final String bagInfoRE = "bag-info.txt";
    private final String bagSizeRE = "Bag-Size";
    private final String baggingDateRE = "Bagging-Date";
    private final String oxumRE = "Payload-Oxum";
    private final Path bagInfoPath;
    private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
    private PayloadOxum payloadOxum;
    private BagMetaElement bagSize;
    private BagMetaElement baggingDate;

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
                BagMetaElement<String> payload = BagMetaElement.ParseBagMetaElement(line);
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

    // Can probably break this out
    private void write(List<BagMetaElement> elements, OpenOption opt) {
        try {
            BufferedWriter writer = Files.newBufferedWriter(bagInfoPath, 
                                                            Charset.forName("UTF-8"),
                                                            opt);
            int index = 0;
            for ( BagMetaElement element : elements) {
                writer.write(element.toString());
                if ( ++index < elements.size()) {
                    writer.newLine();
                }
            }
        } catch (IOException ex) {
            log.error("Error writing to bag-info.txt {}", ex);
        }

    }

    private void fullCreate() {

        List<BagMetaElement> metaElements = new ArrayList<>();
        Path payloadPath = bagInfoPath.getParent().resolve("data");
        bagSize = new BagMetaElement(bagSizeRE, 0);
        baggingDate = new BagMetaElement(baggingDateRE, 
                                         dateFormat.format(new Date()));
        try {
            payloadOxum.calculateOxum(payloadPath);
            metaElements.add(payloadOxum.toBagMetaElement());
            metaElements.add(bagSize);
            metaElements.add(baggingDate);
            write(metaElements, StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            log.error("IOERROR OH GOD WHHY {}", ex);
        }
    }

    private void partialCreate() {
        List<BagMetaElement> metaElements = new ArrayList<>();
        if ( payloadOxum.getNumFiles() == 0 || payloadOxum.getOctetCount() == 0) {
            Path payloadPath = bagInfoPath.getParent().resolve("data");
            try {
                payloadOxum.calculateOxum(payloadPath);
            } catch (IOException ex) {
                log.error("Error calculating payload Oxum {} ", ex);
            }

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
    
}
