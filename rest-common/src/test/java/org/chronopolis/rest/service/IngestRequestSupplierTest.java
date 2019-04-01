package org.chronopolis.rest.service;

import org.chronopolis.rest.models.create.BagCreate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class IngestRequestSupplierTest {
    private static final String DEPOSITOR = "test-depositor";

    private Path root;
    private IngestRequestSupplier supplier;

    @Before
    public void setup() {
        URL bags = ClassLoader.getSystemClassLoader().getResource("bags");
        root = Paths.get(bags.getPath());
    }

    @Test
    public void getRequestForBag() {
        String name = "new-bag-1";
        Path bag = root.resolve(DEPOSITOR).resolve(name);

        // Instead of using magic vals let's resolve everything
        long expectedTotalFiles = 3L;
        File manifest = bag.resolve("manifest-sha256.txt").toFile();
        File tagmanifest = bag.resolve("tagmanifest-sha256.txt").toFile();
        File helloWorld = bag.resolve("data/hello_world").toFile();
        long expectedSize = manifest.length() + tagmanifest.length() + helloWorld.length();

        supplier = new IngestRequestSupplier(bag, root, DEPOSITOR, name);

        Optional<BagCreate> optional = supplier.get();
        Assert.assertTrue("BagCreate is not present", optional.isPresent());
        BagCreate request = optional.get();
        long requestSize = request.getSize();
        long totalFiles = request.getTotalFiles();
        Assert.assertEquals("BagCreate RequestSize does not match", expectedSize, requestSize);
        Assert.assertEquals("BagCreate TotalFiles does not match", expectedTotalFiles, totalFiles);
    }

    @Test
    public void getRequestForSerializedBag() {
        final long expectedTotalFiles = 3L;
        final String name = "new-bag-2";
        Path bag = root.resolve(DEPOSITOR).resolve(name + ".tar");
        supplier = new IngestRequestSupplier(bag, root, DEPOSITOR, name);
        Optional<BagCreate> optional = supplier.get();

        // make sure deserialization happened
        Path bagOut = root.resolve(DEPOSITOR).resolve(name);
        Assert.assertTrue("Bag was not deserialized", bagOut.toFile().exists());
        Assert.assertTrue("Bag was not deserialized", bagOut.toFile().isDirectory());

        // resolve our files + verify existence
        File manifest = bagOut.resolve("manifest-sha256.txt").toFile();
        File tagmanifest = bagOut.resolve("tagmanifest-sha256.txt").toFile();
        File helloWorld = bagOut.resolve("data/hello_world").toFile();
        Assert.assertTrue("Bag manifest-sha256.txt does not exist", manifest.exists());
        Assert.assertTrue("Bag data/hello_world does not exist", helloWorld.exists());
        Assert.assertTrue("Bag tagmanifest-sha256.txt does not exist", tagmanifest.exists());

        long expectedSize = manifest.length() + tagmanifest.length() + helloWorld.length();

        Assert.assertTrue("BagCreate request is not present", optional.isPresent());
        BagCreate request = optional.get();
        long requestSize = request.getSize();
        long totalFiles = request.getTotalFiles();
        Assert.assertEquals("BagCreate RequestSize does not match", expectedSize, requestSize);
        Assert.assertEquals("BagCreate TotalFiles does not match", expectedTotalFiles, totalFiles);
    }

    @Test
    public void getRequestForNonExistentBag() {
        String name = "DNE";
        Path bag = root.resolve(DEPOSITOR).resolve(name);
        supplier = new IngestRequestSupplier(bag, root, DEPOSITOR, name);

        Optional<BagCreate> optional = supplier.get();
        Assert.assertFalse(optional.isPresent());
    }

}