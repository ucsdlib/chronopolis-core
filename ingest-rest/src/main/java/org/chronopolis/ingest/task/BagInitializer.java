package org.chronopolis.ingest.task;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.TrackingThreadPoolExecutor;
import org.chronopolis.ingest.repository.BagSearchCriteria;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
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

/**
 * Class to initialize bags
 * <p/>
 * Created by shake on 8/6/15.
 */
@Component
@EnableScheduling
public class BagInitializer {
    private static final Logger log = LoggerFactory.getLogger(BagInitializer.class);
    private final Integer DEFAULT_PAGE = 0;
    private final Integer DEFAULT_SIZE = 10;
    private final String TAR_TYPE = "application/x-tar";

    private BagService service;
    private IngestSettings settings;
    private TrackingThreadPoolExecutor<Bag> executor;

    @Autowired
    public BagInitializer(BagService service, IngestSettings settings, TrackingThreadPoolExecutor<Bag> bagExecutor) {
        this.settings = settings;
        this.service = service;
        this.executor = bagExecutor;
    }

    /**
     * Search for bags to initialize
     * <p/>
     * Single threaded for now, could easily submit to a thread pool
     * (each job is backed by a bag, reject duplicate bags for long jobs)
     */
    @Scheduled(cron = "${ingest.cron.initialize:0 */1 * * * *}")
    public void initializeBags() {
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withStatus(BagStatus.DEPOSITED);
        Page<Bag> bags = service.findBags(criteria, new PageRequest(DEFAULT_PAGE, DEFAULT_SIZE));
        log.info("Initializing {} bags", bags.getContent().size());
        for (Bag bag : bags) {
            executor.submitIfAvailable(new Initializer(bag), bag);
        }
    }

    public class Initializer implements Runnable {

        final Bag bag;

        public Initializer(Bag bag) {
            this.bag = bag;
        }

        @Override
        public void run() {
            try {
                log.debug("Initializing {}", bag.getName());
                initializeBag(bag);
                log.debug("Finished initializing {}", bag.getName());
                bag.setStatus(BagStatus.INITIALIZED);
            } catch (IOException e) {
                log.error("Error initializing bag {}", bag.getName(), e);
                bag.setStatus(BagStatus.ERROR);
            }

            log.trace("Saving bag {}", bag.getName());
            service.saveBag(bag);
        }

        /**
         * Set the location, fixity value, size, and total number of files for the bag
         *
         * @param bag
         */
        public void initializeBag(Bag bag) throws IOException {
            Path stage = Paths.get(settings.getBagStage());
            Path bagPath = stage.resolve(bag.getLocation());

            // check if we should untar the bag
            log.trace("Probing mime type for {}", bag.getName());
            String mimeType = Files.probeContentType(bagPath);
            if (mimeType != null && mimeType.equals(TAR_TYPE)) {
                bagPath = untar(bagPath, bag.getDepositor());

                // update the path of the bag to the exploded directory
                Path relBag = stage.relativize(bagPath);
                bag.setLocation(relBag.toString());
            }

            // TODO: Get these passed in the ingest request
            log.trace("Counting files for {}", bag.getName());
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

            bag.setSize(bagSize[0]);
            bag.setTotalFiles(fileCount[0]);
        }

        /**
         * Explode a tarball for a given transfer
         * TODO: Determine whether or not to overwrite files
         *
         * @param tarball
         * @param depositor
         */
        private Path untar(Path tarball, String depositor) throws IOException {
            log.debug("Untarring {}", bag.getName());
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
                } else //noinspection Duplicates
                {
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

            // get the directory name of the bag
            //        0       1           2
            // root = stage + depositor
            // bag  = stage + depositor + bag-name
            // root.namecount = 2
            return root.resolve(bag.getName(root.getNameCount()));
        }

    }

}
