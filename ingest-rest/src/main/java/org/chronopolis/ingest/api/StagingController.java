package org.chronopolis.ingest.api;

import org.chronopolis.ingest.ChronPackager;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * Created by shake on 11/5/14.
 */
@RestController
@RequestMapping("/api/staging")
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
    public Iterable<Bag> getBags(Principal principal) {
        return bagRepository.findByStatus(BagStatus.STAGED);
    }

    @RequestMapping(value = "bags/{bag-id}", method = RequestMethod.GET)
    public Bag getBag(Principal principal, @PathVariable("bag-id") Long bagId) {
        return bagRepository.findOne(bagId);
    }

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
                request.getFileName(),
                request.getDepositor(),
                ingestSettings);
        bag = packager.packageForChronopolis();
        bagRepository.save(bag);

        for (Node node : nodeRepository.findAll()) {
            Replication replication = new Replication(node, bag.getId());
            replicationRepository.save(replication);
        }

        return bag;
    }

}
