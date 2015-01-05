package org.chronopolis.ingest.api;

import org.chronopolis.ingest.exception.ConflictException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RestoreRepository;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.models.Restoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Collection;

/**
 * Created by shake on 12/10/14.
 */
@RestController
@RequestMapping("/api/restorations")
public class RestoreController {
    private final Logger log = LoggerFactory.getLogger(RestoreController.class);

    @Autowired
    private RestoreRepository restoreRepository;

    @Autowired
    private NodeRepository nodeRepository;


    @RequestMapping(method = RequestMethod.GET)
    public Collection<Restoration> getRestorations(Principal principal) {
        return restoreRepository.findByStatus(ReplicationStatus.PENDING);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public Restoration putRestoration(Principal principal,
                                      @RequestBody IngestRequest request) {
        String name = request.getName();
        String depositor = request.getDepositor();
        Restoration restoration =
                restoreRepository.findByNameAndDepositor(name, depositor);

        if (restoration == null) {
            restoration = new Restoration(depositor, name, request.getLocation());
        }

        return restoration;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Restoration getRestoration(Principal principal,
                                      @PathVariable("id") Long id) {
        return restoreRepository.findOne(id);
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Restoration acceptRestoration(Principal principal,
                                         @PathVariable("id") Long id) {
        Restoration restoration = restoreRepository.findOne(id);
        Node requestNode = nodeRepository.findByUsername(principal.getName());

        if (restoration == null) {
            // 404
            log.info("Restoration {} accepted from node {}, but was not found",
                    id,
                    principal.getName());
            throw new NotFoundException(restoration.resourceID());
        }

        Node restoringNode = restoration.getNode();
        if (restoringNode != null &&
                !restoringNode.getUsername().equals(principal.getName())) {
            log.info("Restoration {} already has accepted node; " +
                            "ignoring request from {}",
                    id,
                    principal.getName());
            // 409
            throw new ConflictException();
        }

        log.info("Setting {} as the owner for replication {}",
                principal.getName(),
                id);
        restoration.setNode(requestNode);
        restoreRepository.save(restoration);
        return restoration;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public Restoration updateRestoration(Principal principal,
                                         @PathVariable("id") Long id,
                                         @RequestBody Restoration restoration) {
        return restoration;
    }

}
