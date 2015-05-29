package org.chronopolis.ingest.api;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.BagSearchCriteria;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
import java.security.Principal;
import java.util.Map;

import static org.chronopolis.ingest.api.Params.DEPOSITOR;
import static org.chronopolis.ingest.api.Params.NAME;
import static org.chronopolis.ingest.api.Params.PAGE;
import static org.chronopolis.ingest.api.Params.PAGE_SIZE;
import static org.chronopolis.ingest.api.Params.STATUS;

/**
 * REST Controller for controlling actions associated with bags
 *
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api")
public class StagingController {
    private static final String TAR_TYPE = "application/x-tar";

    private final Logger log = LoggerFactory.getLogger(StagingController.class);

    @Autowired
    BagRepository bagRepository;

    @Autowired
    BagService bagService;

    @Autowired
    IngestSettings ingestSettings;

    /**
     * Retrieve all the bags we know about
     *
     * @param principal - authentication information
     * @param params - Query parameters used for searching
     * @return
     */
    @RequestMapping(value = "bags", method = RequestMethod.GET)
    public Iterable<Bag> getBags(Principal principal,
                                 @RequestParam Map<String, String> params) {
        Integer pageNum = params.containsKey(PAGE)
                ? Integer.parseInt(params.get(PAGE))
                : 0;
        Integer pageSize = params.containsKey(PAGE_SIZE)
                ? Integer.parseInt(params.get(PAGE_SIZE))
                : 20;

        BagSearchCriteria criteria = new BagSearchCriteria()
                .withDepositor(params.containsKey(DEPOSITOR) ? params.get(DEPOSITOR) : null)
                .withName(params.containsKey(NAME) ? params.get(NAME) : null)
                .withStatus(params.containsKey(STATUS) ? BagStatus.valueOf(params.get(STATUS)) : null);

        return bagService.findBags(criteria, new PageRequest(pageNum, pageSize));
        /*
        if (pageNum != -1) {
            return bagRepository.findAll(new PageRequest(pageNum, pageSize));
        }

        return bagRepository.findAll();
        */
    }

    /**
     * Retrieve information about a single bag
     *
     * @param principal - authentication information
     * @param bagId - the bag id to retrieve
     * @return
     */
    @RequestMapping(value = "bags/{bag-id}", method = RequestMethod.GET)
    public Bag getBag(Principal principal, @PathVariable("bag-id") Long bagId) {
        Bag bag = bagRepository.findOne(bagId);
        if (bag == null) {
            throw new NotFoundException("bag/" + bagId);
        }
        return bag;
    }

    /**
     * Notification that a bag exists and is ready to be ingested into Chronopolis
     *
     * @param principal - authentication information
     * @param request - the request containing the bag name, depositor, and location of the bag
     * @return
     */
    @RequestMapping(value = "bags", method = RequestMethod.POST)
    public Bag stageBag(Principal principal, @RequestBody IngestRequest request)  {
        String name = request.getName();
        String depositor = request.getDepositor();

        Bag bag = bagRepository.findByNameAndDepositor(name, depositor);
        if (bag != null) {
            log.debug("Bag {} exists from depositor {}, skipping creation", name, depositor);
            return bag;
        }

        bag = new Bag(name, depositor);
        try {
            initializeBag(bag, request.getLocation());
        } catch (IOException e) {
            log.error("Error initializing bag {}:{}", depositor, name);
            return null;
        }

        bagRepository.save(bag);

        return bag;
    }

    /**
     * Set the location, fixity value, size, and total number of files for the bag
     *
     * @param bag
     * @param fileName
     */
    private void initializeBag(Bag bag, String fileName) throws IOException {
        Path stage = Paths.get(ingestSettings.getBagStage());
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
    }


    /**
     * Explode a tarball for a given transfer
     *  @param tarball
     * @param depositor
     */
    private Path untar(Path tarball, String depositor) throws IOException {
        String stage = ingestSettings.getBagStage();

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
