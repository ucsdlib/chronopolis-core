package org.chronopolis.ingest;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.chronopolis.rest.models.BagDistribution.BagDistributionStatus.DISTRIBUTE;

/**
 * Class to initialize bags... or something
 *
 * Created by shake on 8/6/15.
 */
@Component
public class BagInitializer {
    private static final Logger log = LoggerFactory.getLogger(BagInitializer.class);
    private static final String TAR_TYPE = "application/x-tar";

    private static NodeRepository repository;
    private static IngestSettings settings;

    @Autowired
    public BagInitializer(NodeRepository repository, IngestSettings settings) {
        BagInitializer.settings = settings;
        BagInitializer.repository = repository;
    }

    /**
     * Iterate through a list of node usernames and add them to the BagDistribution table
     * TODO: Should we make distribution records for all nodes if the request is empty/null
     * TODO: List<String> -> List<Node> for replicating nodes
     * TODO: Use replicatingNodes size as number of required replications? (implicit association)
     *
     * @param bag
     * @param replicatingNodes
     */
    private static void createBagDistributions(Bag bag, List<String> replicatingNodes) {
        int numDistributions = 0;
        if (replicatingNodes == null) {
            replicatingNodes = new ArrayList<>();
        }

        for (String nodeName : replicatingNodes) {
            Node node = repository.findByUsername(nodeName);
            if (node != null) {
                log.debug("Creating dist record for {}", nodeName);
                bag.addDistribution(node, DISTRIBUTE);
                numDistributions++;
            }
        }

        if (numDistributions < bag.getRequiredReplications()) {
            for (Node node : repository.findAll()) {
                log.debug("Creating dist record for {}", node.getUsername());
                bag.addDistribution(node, DISTRIBUTE);
                numDistributions++;
            }
        }
    }


    /**
     * Set the location, fixity value, size, and total number of files for the bag
     *
     * @param bag
     * @param request
     */
    public static void initializeBag(Bag bag, IngestRequest request) throws IOException {
        String fileName = request.getLocation();
        Path stage = Paths.get(settings.getBagStage());
        Path bagPath = stage.resolve(fileName);

        // check if we should untar the bag
        String mimeType = Files.probeContentType(bagPath);
        if (mimeType != null && mimeType.equals(TAR_TYPE)) {
            bagPath = untar(bagPath, bag.getDepositor());
        }

        // TODO: Get these passed in the ingest request
        final long[] bagSize = {0};
        final long[] fileCount = {0};
        Files.walkFileTree(bagPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                fileCount[0]++;
                bagSize[0] += attrs.size();
                return FileVisitResult.CONTINUE;
            }
        });

        Path relBag = stage.relativize(bagPath);
        bag.setLocation(relBag.toString());
        bag.setSize(bagSize[0]);
        bag.setTotalFiles(fileCount[0]);
        bag.setFixityAlgorithm("SHA-256");

        // could put this in the bag object...
        if (request.getRequiredReplications() > 0) {
            bag.setRequiredReplications(request.getRequiredReplications());
        }

        createBagDistributions(bag, request.getReplicatingNodes());
    }

    /**
     * Explode a tarball for a given transfer
     *
     * @param tarball
     * @param depositor
     */
    private static Path untar(Path tarball, String depositor) throws IOException {
        String stage = settings.getBagStage();

        // Set up our tar stream and channel
        TarArchiveInputStream tais = new TarArchiveInputStream(java.nio.file.Files.newInputStream(tarball));
        TarArchiveEntry entry = tais.getNextTarEntry();
        ReadableByteChannel inChannel = Channels.newChannel(tais);

        // Get our root path (just the staging area), and create an updated bag path
        Path root = Paths.get(stage, depositor);
        Path bag = root.resolve(entry.getName());

        while (entry != null) {
            Path entryPath = root.resolve(entry.getName());

            if (entry.isDirectory()) {
                log.trace("Creating directory {}", entry.getName());
                java.nio.file.Files.createDirectories(entryPath);
            } else {
                log.trace("Creating file {}", entry.getName());

                entryPath.getParent().toFile().mkdirs();

                // In case files are greater than 2^32 bytes, we need to use a
                // RandomAccessFile and FileChannel to write them
                RandomAccessFile file = new RandomAccessFile(entryPath.toFile(), "rw");
                FileChannel out = file.getChannel();

                // The TarArchiveInputStream automatically updates its offset as
                // it is read, so we don't need to worry about it
                out.transferFrom(inChannel, 0, entry.getSize());
                out.close();
            }

            entry = tais.getNextTarEntry();
        }

        // basically, get the directory name of the bag
        //        0       1           2
        // root = stage + depositor
        // bag  = stage + depositor + bag-name
        // root.namecount = 2
        return root.resolve(bag.getName(root.getNameCount()));
    }


}
