package org.chronopolis.rest.service;

import org.chronopolis.rest.kot.models.create.BagCreate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
        supplier = new IngestRequestSupplier(bag, root, DEPOSITOR, name);

        Optional<BagCreate> optional = supplier.get();
        Assert.assertTrue(optional.isPresent());
        BagCreate request = optional.get();
        Assert.assertEquals(181L, request.getSize());
        Assert.assertEquals(3L, request.getTotalFiles());
    }

    @Test
    public void getRequestForSerializedBag() {
        String name = "new-bag-2";
        Path bag = root.resolve(DEPOSITOR).resolve(name + ".tar");
        supplier = new IngestRequestSupplier(bag, root, DEPOSITOR, name);

        Optional<BagCreate> optional = supplier.get();
        Assert.assertTrue(optional.isPresent());
        BagCreate request = optional.get();
        Assert.assertEquals(181L, request.getSize());
        Assert.assertEquals(3L, request.getTotalFiles());
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