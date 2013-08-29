/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
//import org.easymock.EasyMock;
import org.junit.Test;
import org.junit.Assert;


/**
 * TODO: Test creation
 *
 * @author shake
 */
public class BagitTest extends TestUtil {
    Path bagitPath;

    @Test
    public void testValid() throws URISyntaxException {
        URL bag = getClass().getResource("/individual/bagit-valid.txt");
        bagitPath = Paths.get(bag.toURI());
        BagitProcessor bagitProcessor = new BagitProcessor(bagitPath);
        bagitProcessor.setBagitPath(bagitPath);
        Assert.assertTrue(bagitProcessor.valid());
    }

    @Test
    public void testInvalid() throws URISyntaxException {
        URL bag = getClass().getResource("/individual/bagit-invalid.txt");
        bagitPath = Paths.get(bag.toURI());
        BagitProcessor bagitProcessor = new BagitProcessor(bagitPath);
        bagitProcessor.setBagitPath(bagitPath);
        Assert.assertFalse(bagitProcessor.valid());
    }

    @Test
    public void testMissingTagEncoding() throws URISyntaxException {
        URL bag = getClass().getResource("/individual/bagit-noencoding.txt");
        bagitPath = Paths.get(bag.toURI());
        BagitProcessor bagitProcessor = new BagitProcessor(bagitPath);
        bagitProcessor.setBagitPath(bagitPath);
        Assert.assertFalse(bagitProcessor.valid());
    }

    @Test
    public void testMissingVersion() throws URISyntaxException {
        URL bag = getClass().getResource("/individual/bagit-noversion.txt");
        bagitPath = Paths.get(bag.toURI());
        BagitProcessor bagitProcessor = new BagitProcessor(bagitPath);
        bagitProcessor.setBagitPath(bagitPath);
        Assert.assertFalse(bagitProcessor.valid());
    }

    @Test
    public void testEmptyValues() throws URISyntaxException {
        URL bag = getClass().getResource("/individual/bagit-emptyvals.txt");
        bagitPath = Paths.get(bag.toURI());
        BagitProcessor bagitProcessor = new BagitProcessor(bagitPath);
        bagitProcessor.setBagitPath(bagitPath);
        Assert.assertFalse(bagitProcessor.valid());
    }

    
}
