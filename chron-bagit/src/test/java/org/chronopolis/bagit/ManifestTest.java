/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO: Create bags for various scenarios
 *
 * @author shake
 */
public class ManifestTest {
   
    ManifestProcessor processor;
    URL validSha;
    URL validMd5;
    URL shaMissingTag;
    URL md5MissingTag;
    URL invalidSha;
    URL invalidMd5;
    URL shaMissingAll;


    @Before
    public void setup() {
    }


    @Test
    public void testValidShaManifest() throws Exception {
        URL bag = getClass().getResource("/bags/validbag-256/");
        Path bagPath = Paths.get(bag.toURI());
        processor = new ManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertTrue(valid);
    }


}
