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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private Path bagInfoPath;
    private final DateFormat dateFormat = new SimpleDateFormat("YYYY-MM-DD");
    private PayloadOxum payloadOxum;
    private HashMap<String, TagMetaElement> elements;
    
    private boolean initialized;

    private final Logger log = LoggerFactory.getLogger(BagInfoProcessor.class);

    /**
     * Initialize a BagInfoProcessor 
     * 
     * @param bag The path to the root of a bag
     */
    public BagInfoProcessor(Path bag) {
        this.payloadOxum = new PayloadOxum();
        this.bagInfoPath = bag.resolve(bagInfoName);
        this.initialized = false;
        this.elements = new HashMap<>();
        
        // Hm...
        /*
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
                                    */
        this.fullRegex = Pattern.compile("^[A-Za-z\\-]*:");
        init();
    }

    /**
     * Set the path for the processor. Reinitializes the processor.
     * @param bagInfoPath
     */
    public void setBagInfoPath(Path bagInfoPath) {
        this.bagInfoPath = bagInfoPath;
        elements.clear();
        initialized = false;
        init();
    }

    private boolean exists() {
        return bagInfoPath.toFile().exists();
    }

    
    /*
     * The only thing we really need to validate against is the Payload Oxum
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
            valid = false;
        } else {
            // Set up the oxums
            try {
                calculatedOxum.calculateOxum(bagPath.resolve(dataDir));
            } catch (IOException ex) {
                log.error("Could not read data directory to resolve payload: {}", ex);
                valid = false;
            }

            // And validate
            if (!payloadOxum.equals(calculatedOxum)) {
                log.error("Payload in bag does not equal calculated payload:\nexpected {}\nfound {}",
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
            log.info("No bag info file found: {}", ex);
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
                    // Or maybe we should just match on [A-Za-z\-]*:
                    Matcher m = fullRegex.matcher(extra);
                    if ( m.find()) {
                        break;
                    } else {
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
            log.error("Error reading from bag-info.txt: {}", ex);
        }

        // also init our payload properly
        TagMetaElement<String> oxum = elements.get(oxumRE);    
        if ( oxum != null ) {
            payloadOxum.setFromString(oxum.getValue());
        }
        
        initialized = true;
    }

    @Override
    public void create() {
        if ( !initialized ) {
            init();
        }

        // I don't really feel like trying to figure out when to append/etc
        // so let's just write the entire thing
        // Maybe we'll get lucky and have it be sorted by how it comes in but
        // probably not
        try (BufferedWriter writer = Files.newBufferedWriter(bagInfoPath, 
                                                             Charset.forName("UTF-8"), 
                                                             StandardOpenOption.CREATE)) {
            for (Map.Entry<String, TagMetaElement> element : elements.entrySet()) {
                writer.write(element.getValue().toString());
                writer.newLine();
            }
        } catch (IOException ex) {
            log.error("Error writing bag-info.txt: {}", ex);
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
