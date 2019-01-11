package org.chronopolis.rest.service;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.chronopolis.rest.csv.BagFileHeaders;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Generate a csv consisting of the files for a given Bag
 * <p>
 * At the moment this does not rely on our Bagging library as it is only for creating Bags, but
 * eventually should be moved toward that when it can be used to represent a Bag on disk.
 * <p>
 * The csv we want to generate looks like
 * filename, size (file size in bytes), fixity_algorithm, fixity_value
 * <p>
 * This takes a very naive approach to creating the csv as it will scan the manifests for a Bag
 * and resolve the paths to get the file metadata. We might want to offer something which can write
 * to an output stream as we validate each Bag but for now that's not terribly important.
 *
 * Also something to keep in mind is that we are going to be limited in how large of csv files we
 * can upload, meaning we should optionally split files here while writing.
 *
 * @author shake
 */
public class BagFileCsvGenerator implements Callable<BagFileCsvResult> {

    private final Logger log = LoggerFactory.getLogger(BagFileCsvGenerator.class);
    private static final String MANIFEST_NAME = "manifest-";
    private static final String TAGMANIFEST_NAME = "tagmanifest-";

    private Path out;
    private Path root;
    private FixityAlgorithm algorithm;

    public BagFileCsvGenerator(Path out, Path root, FixityAlgorithm algorithm) {
        this.out = out;
        this.root = root;
        this.algorithm = algorithm;
    }

    @Override
    public BagFileCsvResult call() {
        Path parent = root.getParent();
        String bagName = parent.relativize(root).toString();

        String prefix = derivePrefix(algorithm);

        Path csv = out.resolve(bagName + ".csv");
        BagFileCsvResult result = new BagFileCsvResult(csv);
        try (CSVPrinter print = CSVFormat.RFC4180.withHeader()
                .withHeader(BagFileHeaders.class)
                .print(csv, Charset.defaultCharset())) {
            Path manifest = root.resolve(MANIFEST_NAME + prefix);
            Path tagmanifest = root.resolve(TAGMANIFEST_NAME + prefix);
            writeToCsv(print, manifest);
            writeToCsv(print, tagmanifest);

            // now we just need to write the values for the tagmanifest as well
            writeTagmanifest(print, tagmanifest);
        } catch (IOException e) {
            log.error("Error writing csv for bag files", e);
            result = new BagFileCsvResult(e);
        }

        return result;
    }

    private void writeTagmanifest(CSVPrinter print, Path tagmanifest) throws IOException {
        String filename = "/" + TAGMANIFEST_NAME + derivePrefix(algorithm);
        File tagFile = tagmanifest.toFile();
        HashFunction hashFunction = Hashing.sha256();
        HashCode hash = com.google.common.io.Files
                .asByteSource(tagFile)
                .hash(hashFunction);

        print.printRecord(filename, tagFile.length(), algorithm.getCanonical(), hash.toString());
    }

    private void writeToCsv(CSVPrinter printer, Path manifest) throws IOException {
        String fixityAlgorithm = algorithm.getCanonical();
        try (BufferedReader reader = Files.newBufferedReader(manifest)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // money folder
                String[] split = line.split("\\s", 2);
                if (split.length != 2) {
                    throw new IOException("Invalid manifest format");
                }

                String fixity = split[0].trim();
                String relativePath = split[1].trim();
                Path file = root.resolve(relativePath);

                Long length = file.toFile().length();
                String normalizedPath = relativePath.startsWith("/")
                        ? relativePath
                        : "/" + relativePath;

                printer.printRecord(normalizedPath, length, fixityAlgorithm, fixity);
            }
        }
    }

    private String derivePrefix(FixityAlgorithm algorithm) {
        switch (algorithm) {
            case SHA_256:
                return "sha256.txt";
            case UNSUPPORTED:
            default:
                return ".txt";
        }
    }

}
