package org.chronopolis.ingest.api;

import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.ConflictException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.exception.UnauthorizedException;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RestoreRepository;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.models.Restoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

import static org.chronopolis.ingest.api.Params.PAGE;
import static org.chronopolis.ingest.api.Params.PAGE_SIZE;

/**
 * TODO: Add status param for accepted restores
 *
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
    public Iterable<Restoration> getRestorations(Principal principal,
                                                 @RequestParam Map<String, String> params) {
        Integer page = params.containsKey(PAGE) ? Integer.parseInt(params.get(PAGE)) : -1;
        Integer pageSize = params.containsKey(PAGE_SIZE) ? Integer.parseInt(params.get(PAGE_SIZE)) : 20;

        Iterable<Restoration> restorations;
        if (page == -1) {
            restorations = restoreRepository.findByStatus(ReplicationStatus.PENDING);
        } else {
            Pageable pageable = new PageRequest(page, pageSize);
            restorations = restoreRepository.findByStatus(ReplicationStatus.PENDING, pageable);
        }

        return restorations;
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
        Restoration restoration = restoreRepository.findOne(id);
        // check if it exists
        if (restoration == null) {
            throw new NotFoundException("restore/" + id);
        }

        Node node = restoration.getNode();
        // check if the user is authorized to access the resource
        if (node != null && !node.getUsername().equals(principal.getName())) {
            throw new UnauthorizedException("restore/" + id);
        }

        return restoration;
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
            throw new NotFoundException("restore/" + id);
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
                                         @RequestBody Restoration updated) {
        Restoration restoration = restoreRepository.findOne(id);

        // check to make sure someone has accepted the restoration
        if (restoration.getNode() == null) {
            throw new BadRequestException("Restoration has no associated node and cannot be updated");
        }

        // check to make sure the correct node is updating the restoration
        if (!restoration.getNode().getUsername().equals(principal.getName())) {
            throw new UnauthorizedException(principal.getName());
        }

        ReplicationStatus status = updated.getStatus();

        // We want to control success/failure status here, so in the event of these two update to 'transferred'
        if (status.equals(ReplicationStatus.SUCCESS) || status.equals(ReplicationStatus.TRANSFERRED)) {
            restoration.setStatus(ReplicationStatus.TRANSFERRED);
        } else if (status.equals(ReplicationStatus.STARTED)) {
            restoration.setStatus(ReplicationStatus.STARTED);
        } else {
            // client side failure... tbd what to do
            restoration.setStatus(ReplicationStatus.FAILURE);
        }

        restoreRepository.save(restoration);
        return restoration;
    }

}
