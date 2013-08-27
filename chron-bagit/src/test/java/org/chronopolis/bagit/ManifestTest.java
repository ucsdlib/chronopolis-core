/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import org.chronopolis.bagit.ManifestProcessor.ManifestError;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * TODO: Create bags for various scenarios
 * TODO: Combine some of the sha/md5 tests to reduce code dup
 *
 * @author shake
 */
public class ManifestTest {
   
    ManifestProcessor processor;
    TagManifestProcessor tagProcessor;
    URL validSha;
    URL validMd5;
    URL shaMissingTag;
    URL md5MissingTag;
    URL invalidSha;
    URL invalidMd5;
    URL shaMissingAll;
    URL orphanBag;


    @Before
    public void setup() {
        validSha = getClass().getResource("/bags/validbag-256/");
        validMd5 = getClass().getResource("/bags/validbag-md5/");
        invalidSha = getClass().getResource("/bags/invalidbag-256/");
        invalidMd5 = getClass().getResource("/bags/invalidbag-md5/");
        shaMissingTag = getClass().getResource("/bags/bag-notagmanifest-256/");
        md5MissingTag = getClass().getResource("/bags/bag-notagmanifest-md5/");
        shaMissingAll = getClass().getResource("/bags/bag-nomanifest/");
        orphanBag = getClass().getResource("/bags/orphans-256");
    }


    @Test
    public void testValidShaManifest() throws Exception {
        Path bagPath = Paths.get(validSha.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertTrue(valid);
        valid = tagProcessor.call();
        Assert.assertTrue(valid);
    }

    @Test
    public void testValidMd5Manifest() throws Exception {
        Path bagPath = Paths.get(validMd5.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertTrue(valid);
        valid = tagProcessor.call();
        Assert.assertTrue(valid);
    }

    @Test
    public void testInvalidShaManifest() throws Exception {
        Path bagPath = Paths.get(invalidSha.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertFalse(valid);

        // Because the manifest is now corrupted, the digest in the tagmanifest
        // will not match
        valid = tagProcessor.call();
        Assert.assertFalse(valid);

        // We only need to test any error stuff here, doing so in the
        // md5 test would just be redundant
        HashSet<ManifestError> errors = processor.getCorruptedFiles();
        Assert.assertEquals(1, errors.size());
    }

    @Test
    public void testInvalidMd5Manifest() throws Exception {
        Path bagPath = Paths.get(invalidMd5.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertFalse(valid);
        valid = tagProcessor.call();
        Assert.assertFalse(valid);
    }

    @Test
    // TODO: Test creation
    public void testNoTagManifestSha() throws Exception {
        Path bagPath = Paths.get(shaMissingTag.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertTrue(valid);
        valid = tagProcessor.call();
        Assert.assertFalse(valid);
    }

    @Test
    // TODO: Test creation
    public void testNoTagManifestMd5() throws Exception {
        Path bagPath = Paths.get(md5MissingTag.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertTrue(valid);
        valid = tagProcessor.call();
        Assert.assertFalse(valid);
    }

    @Test
    // TODO: Test creation of manifests
    public void testNoManifests() throws Exception {
        Path bagPath = Paths.get(shaMissingAll.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertFalse(valid);
        valid = tagProcessor.call();
        Assert.assertFalse(valid);

    }

    @Test
    public void testOrphans() throws Exception {
        Path bagPath = Paths.get(orphanBag.toURI());
        processor = new ManifestProcessor(bagPath);
        tagProcessor = new TagManifestProcessor(bagPath);
        boolean valid = processor.call();
        Assert.assertTrue(valid);
        valid = tagProcessor.call();
        Assert.assertTrue(valid);

        HashSet<Path> orphanedFiles = processor.getOrphans();
        HashSet<Path> orphanedTags = tagProcessor.getOrphans();
        Assert.assertEquals(2, orphanedFiles.size());

        Assert.assertEquals(1, orphanedTags.size());
    }


}
