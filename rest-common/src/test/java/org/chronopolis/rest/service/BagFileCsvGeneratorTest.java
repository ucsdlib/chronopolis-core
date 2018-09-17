package org.chronopolis.rest.service;

import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class BagFileCsvGeneratorTest {
    private static final String BAG = "new-bag-1";
    private static final String DEPOSITOR = "test-depositor";

    @Test
    public void call() {
        URL bags = ClassLoader.getSystemClassLoader().getResource("bags");

        // So for the CsvGenerator we need
        // out:       Path            | The location where our csv is generated
        // root:      Path            | The root of the Bag
        // algorithm: FixityAlgorithm | The FixityAlgorithm used for validating the bag
        Path out = Paths.get(bags.getPath());
        Path root = out.resolve(DEPOSITOR).resolve(BAG);
        FixityAlgorithm algorithm = FixityAlgorithm.SHA_256;

        BagFileCsvGenerator generator = new BagFileCsvGenerator(out, root, algorithm);

        BagFileCsvResult call = generator.call();
        Assert.assertTrue(call.isSuccess());

        // normally we'd map but since this is a test just do a get
        Path path = call.getCsv().get();
        Assert.assertNotNull(path);

        try (Stream<String> lines = Files.lines(path)) {
            long count = lines.count();
            Assert.assertEquals(3, count);
        } catch (IOException e) {
            Assert.fail();
        }

    }
}