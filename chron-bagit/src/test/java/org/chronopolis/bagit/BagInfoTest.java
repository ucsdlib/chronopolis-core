/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

import org.chronopolis.bagit.util.PayloadOxum;
import org.junit.Assert;

/**
 * TODO: Test creation
 *
 * @author shake
 */
public class BagInfoTest {
    BagInfoProcessor bagInfoProcessor;
    private final String bagSizeRE = "Bag-Size";
    private final String baggingDateRE = "Bagging-Date";

    @Test
    public void testInit() throws IOException, URISyntaxException {
        URL bag = getClass().getResource("/individual/bag-info-valid.txt");
        Path bagPath = Paths.get(bag.toURI());
        bagInfoProcessor = new BagInfoProcessor(bagPath);
        bagInfoProcessor.setBagInfoPath(bagPath);

        Assert.assertEquals(1, bagInfoProcessor.getPayloadOxum().getNumFiles());
        Assert.assertEquals(18, 
                            bagInfoProcessor.getPayloadOxum().getOctetCount());
        Assert.assertEquals(bagSizeRE, bagInfoProcessor.getBagSize().getKey());
        Assert.assertEquals("18 K", bagInfoProcessor.getBagSize().getValue());
        Assert.assertEquals(baggingDateRE, 
                            bagInfoProcessor.getBaggingDate().getKey());
        Assert.assertEquals("2013-08-26", bagInfoProcessor.getBaggingDate().getValue());
    }

    

    @Test
    public void testValid() throws IOException, Exception {
        URL bag = getClass().getResource("/individual/bag-info-valid.txt");
        URL oxum = getClass().getResource("/bags/validbag-256/data");
        Path bagPath = Paths.get(bag.toURI());
        bagInfoProcessor = new BagInfoProcessor(bagPath);
        bagInfoProcessor.setBagInfoPath(bagPath);

        PayloadOxum actualOxum = new PayloadOxum();
        actualOxum.calculateOxum(Paths.get(oxum.toURI()));
        bagInfoProcessor.setPayloadOxum(actualOxum);
        // Our validator will try and calculate the actual payload, so I made
        // a data directory in the individual folder for such purpose
        boolean valid = bagInfoProcessor.valid();
         
        Assert.assertTrue(valid);
    }

    @Test
    public void testInvalidFile() throws IOException, Exception {
        URL bag = getClass().getResource("/individual/bag-info-invalid.txt");
        URL oxum = getClass().getResource("/bags/validbag-256/data");
        Path bagPath = Paths.get(bag.toURI());
        bagInfoProcessor = new BagInfoProcessor(bagPath);

        PayloadOxum actualOxum = new PayloadOxum();
        actualOxum.calculateOxum(Paths.get(oxum.toURI()));
        bagInfoProcessor.setPayloadOxum(actualOxum);

        Assert.assertFalse(bagInfoProcessor.valid());
    }
    
}
