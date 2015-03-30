package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.models.Bag;
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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Principal;
import java.util.Map;

import static org.chronopolis.ingest.api.Params.PAGE;
import static org.chronopolis.ingest.api.Params.PAGE_SIZE;

/**
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api")
public class StagingController {
    Logger log = LoggerFactory.getLogger(StagingController.class);

    @Autowired
    BagRepository bagRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    IngestSettings ingestSettings;

    @RequestMapping(value = "bags", method = RequestMethod.GET)
    public Iterable<Bag> getBags(Principal principal,
                                 @RequestParam Map<String, String> params) {
        Integer pageNum = params.containsKey(PAGE)
                ? Integer.parseInt(params.get(PAGE))
                : -1;
        Integer pageSize = params.containsKey(PAGE_SIZE)
                ? Integer.parseInt(params.get(PAGE_SIZE))
                : 20;

        if (pageNum != -1) {
            return bagRepository.findAll(new PageRequest(pageNum, pageSize));
        }

        return bagRepository.findAll();
    }

    @RequestMapping(value = "bags/{bag-id}", method = RequestMethod.GET)
    public Bag getBag(Principal principal, @PathVariable("bag-id") Long bagId) {
        Bag bag = bagRepository.findOne(bagId);
        if (bag == null) {
            throw new NotFoundException("bag/" + bagId);
        }
        return bag;
    }

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


    /*
    @RequestMapping(value = "bags", method = RequestMethod.PUT)
    public Bag stageBag(Principal principal, @RequestBody IngestRequest request) {
        String name = request.getName();
        String depositor = request.getDepositor();

        // First check if the bag exists
        Bag bag = bagRepository.findByNameAndDepositor(name, depositor);

        if (bag != null) {
            log.debug("Bag {} exists from depositor {}, skipping creation", name, depositor);
            return bag;
        }

        log.debug("Creating bag {} for depositor {}", name, depositor);
        // If not, create the bag + tokens, then save it
        ChronPackager packager = new ChronPackager(request.getName(),
                request.getLocation(),
                request.getDepositor(),
                ingestSettings);
        bag = packager.packageForChronopolis();
        bagRepository.save(bag);

        Path bagPath = Paths.get(ingestSettings.getBagStage(),
                                 bag.getLocation());
        Path tokenPath = Paths.get(ingestSettings.getTokenStage(),
                                   bag.getTokenLocation());

        // Set up where nodes will pull from
        String user = ingestSettings.getReplicationUser();
        String server = ingestSettings.getStorageServer();
        String tokenStore = new StringBuilder(user)
                .append("@").append(server)
                .append(":").append(tokenPath.toString())
                .toString();
        String bagLocation = new StringBuilder(user)
                .append("@").append(server)
                .append(":").append(bagPath.toString())
                .toString();


        for (Node node : nodeRepository.findAll()) {
            log.trace("Creating replication object for {}", node.getUsername());
            Replication replication = new Replication(node, bag, bagLocation, tokenStore);
            replication.setProtocol("rsync");
            replicationRepository.save(replication);
        }

        return bag;
    }
    */

}
