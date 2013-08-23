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
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    //
    private static final String OR = "|";
    // As defined by the bagit spec
    private final String sourceRE           = "Source-Organization";
    private final String addressRE          = "Organization-Address";
    private final String contactNameRE      = "Contact-Name";
    private final String contactPhoneRE     = "Contact-Phone";
    private final String contactMailRE      = "Contact-Email";
    private final String externalDescRE     = "External-Description";
    private final String baggingDateRE      = "Bagging-Date";
    private final String externalIdRE       = "External-Identifier";
    private final String bagSizeRE          = "Bag-Size";
    private final String oxumRE             = "Payload-Oxum";
    private final String bagGroupRE         = "Bag-Group";
    private final String bagCountRE         = "Bag-Count";
    private final String internalSenderIdRE = "Internal-Sender-Identification";
    private final String internalSenderDesc = "Internal-Sender-Description";

    // And now the other stuff
    private final String bagInfoName = "bag-info.txt";
    private Pattern fullRegex;
    private final Path bagInfoPath;
    private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
    private PayloadOxum payloadOxum;
    private HashMap<String, TagMetaElement> elements;
    
    private boolean initialized;

    private final Logger log = LoggerFactory.getLogger(BagInfoProcessor.class);

    public BagInfoProcessor(Path bag) {
        payloadOxum = new PayloadOxum();
        this.bagInfoPath = bag.resolve(bagInfoName);
        initialized = false;
        elements = new HashMap<>();
        
        // Hm...
        fullRegex = Pattern.compile(sourceRE + OR +
                                    addressRE + OR +
                                    contactNameRE + OR +
                                    contactPhoneRE + OR +
                                    contactMailRE + OR + 
                                    externalDescRE + OR +
                                    baggingDateRE + OR +
                                    externalIdRE + OR + 
                                    bagSizeRE + OR + 
                                    oxumRE + OR + 
                                    bagGroupRE + OR +
                                    bagCountRE + OR + 
                                    internalSenderIdRE + OR +
                                    internalSenderDesc);
        init();
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

        // Do we want to short circuit and return?
        // ....probably
        if ( !exists() ) {
            valid = false;
        }

        if (payloadOxum == null || payloadOxum.getNumFiles() == 0 || 
            payloadOxum.getOctetCount() == 0) {
            System.out.println("Bunch of null stuff");
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
                log.error("Payload in bag does not equal calculated payload: {} -> {}",
                           payloadOxum.toString(), calculatedOxum.toString());
                valid = false;
            }
        }

        return valid;
    }

    private void init() {
        BufferedReader reader;
        try {
            reader = Files.newBufferedReader(bagInfoPath,
                                             Charset.forName("UTF-8"));
        } catch (IOException ex) {
            log.info("No bag info file found");
            initialized = true;
            return;
        }

        String line;
        try {
            while ((line = reader.readLine())!=null) {
                // Set where we are in the file
                reader.mark(80);
                StringBuilder fullElement = new StringBuilder(line);
                String extra; 
                while ( (extra = reader.readLine()) != null) {
                    // Do we need this? 
                    // We can take arbitrary values anyways so maybe the best 
                    // thing would be to check for spaces
                    // Maybe we should just match on [A-Za-z\-]*:
                    Matcher m = fullRegex.matcher(extra);
                    if ( m.find()) {
                        break;
                    } else {
                        System.out.println("No match on the line, appending " + extra);
                        fullElement.append(" ");
                        fullElement.append(extra.trim());
                        // Update the mark
                        reader.mark(80);
                    }
                }
                TagMetaElement<String> payload = TagMetaElement.ParseBagMetaElement(fullElement.toString());
                if ( elements.get(payload.getKey()) != null ) {
                    throw new RuntimeException("Multiple values of " + payload.getKey()
                            + " found in bag-info. Exiting.");
                }
                elements.put(payload.getKey(), payload);

                // Make sure we start at the next element
                reader.reset();
            }
        } catch (IOException ex) {
            log.error("Error reading from bag-info.txt");
        }

        // also init our payload properly
        TagMetaElement<String> oxum = elements.get(oxumRE);    
        if ( oxum != null ) {
            payloadOxum.setFromString(oxum.getValue());
        }
        
        initialized = true;
    }

    private void fullCreate() {

        List<TagMetaElement> metaElements = new ArrayList<>();
        Path payloadPath = bagInfoPath.getParent().resolve("data");
        setBagSize((TagMetaElement<String>) new TagMetaElement(bagSizeRE, 0, false));
        setBaggingDate((TagMetaElement<String>) new TagMetaElement(baggingDateRE, 
                                                                   dateFormat.format(new Date()),
                                                                   false));
        try {
            getPayloadOxum().calculateOxum(payloadPath);
        } catch (IOException ex) {
            log.error("Error creating payloadOxum for path {} \n {}",
                      payloadPath,
                      ex);
        }
        metaElements.add(getPayloadOxum().toBagMetaElement());
        metaElements.add(getBagSize());
        metaElements.add(getBaggingDate());
        BagFileWriter.write(bagInfoPath, 
                            metaElements, 
                            StandardOpenOption.CREATE_NEW);
    }

    // Do we want to rewrite the whole file or figure out where in the file we 
    // need to add elements
    private void partialCreate() {
        List<TagMetaElement> metaElements = new ArrayList<>();
        if ( getPayloadOxum().getNumFiles() == 0 || getPayloadOxum().getOctetCount() == 0) {
            Path payloadPath = bagInfoPath.getParent().resolve("data");
            try {
                getPayloadOxum().calculateOxum(payloadPath);
            } catch (IOException ex) {
                log.error("Error calculating payload Oxum {} ", ex);
            }
            metaElements.add(getPayloadOxum().toBagMetaElement());
        }
    }

    @Override
    public void create() {
        if ( !initialized ) {
            init();
        }
        if ( exists() ) {
            partialCreate();
        } else {
            fullCreate();
        }
    }

    /**
     * @return the payloadOxum
     */
    public PayloadOxum getPayloadOxum() {
        return payloadOxum;
    }

    /**
     * @return the bagSize
     */
    public TagMetaElement getBagSize() {
        return elements.get(bagSizeRE);
    }

    /**
     * @return the baggingDate
     */
    public TagMetaElement getBaggingDate() {
        return elements.get(baggingDateRE);
    }

    /**
     * @param payloadOxum the payloadOxum to set
     */
    public void setPayloadOxum(PayloadOxum payloadOxum) {
        this.payloadOxum = payloadOxum;
    }

    /**
     * @param bagSize the bagSize to set
     */
    public void setBagSize(TagMetaElement<String> bagSize) {
        elements.put(bagSize.getKey(), bagSize);
    }

    /**
     * @param baggingDate the baggingDate to set
     */
    public void setBaggingDate(TagMetaElement<String> baggingDate) {
        elements.put(baggingDate.getKey(), baggingDate);
    }

    public void setCustomValue(String key, String value) {
        if ( key == null || key.isEmpty()) {
            log.error("Cannot have null key");
        }
        if ( value == null || value.isEmpty()) {
            log.error("Cannot have null value");
        }

        elements.put(key, new TagMetaElement<>(key, value, false));
    }
    
}
