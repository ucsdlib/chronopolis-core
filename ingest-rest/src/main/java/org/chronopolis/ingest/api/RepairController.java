package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.ConflictException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.exception.UnauthorizedException;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.RepairSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.repair.Repair;
import org.chronopolis.rest.entities.repair.Strategy;
import org.chronopolis.rest.entities.serializers.ExtensionsKt;
import org.chronopolis.rest.models.FulfillmentStrategy;
import org.chronopolis.rest.models.create.RepairCreate;
import org.chronopolis.rest.models.enums.AuditStatus;
import org.chronopolis.rest.models.enums.RepairStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;

import static org.chronopolis.ingest.IngestController.createPageRequest;
import static org.chronopolis.ingest.IngestController.hasRoleAdmin;

/**
 * RestController for our repair/fulfillment api
 * <p>
 * todo: might be able to have a method which queries for a fulfillment/repair
 * and throws exceptions as needed (NotFound // Unauthorized)
 * <p>
 * Created by shake on 1/24/17.
 */
@RestController
@RequestMapping("/api/repairs")
public class RepairController {
    private final Logger log = LoggerFactory.getLogger(RepairController.class);
    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);

    private final NodeRepository nodes;
    private final BagService bService;
    private final SearchService<Repair, Long, RepairRepository> rService;

    @Autowired
    public RepairController(BagService bagService,
                            NodeRepository nodes,
                            SearchService<Repair, Long, RepairRepository> repairService) {
        this.bService = bagService;
        this.nodes = nodes;
        this.rService = repairService;
    }

    /**
     * Return all Repairs
     *
     * @param params a map of query parameters to search on
     * @return all the Repairs requested
     */
    @RequestMapping(method = RequestMethod.GET)
    public Page<Repair> getRequests(@RequestParam Map<String, String> params) {
        access.info("[GET /api/repairs]");
        RepairStatus status = params.containsKey(Params.STATUS)
                ? RepairStatus.valueOf(params.get(Params.STATUS))
                : null;

        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withStatus(status)
                .withTo(params.getOrDefault(Params.TO, null))
                .withCleaned(params.getOrDefault(Params.CLEANED, null))
                .withReplaced(params.getOrDefault(Params.REPLACED, null))
                .withValidated(params.getOrDefault(Params.VALIDATED, null))
                .withRequester(params.getOrDefault(Params.REQUESTER, null));
        return rService.findAll(criteria, createPageRequest(params, ImmutableMap.of()));
    }

    /**
     * Get a single Repair
     *
     * @param id the id of the repair
     * @return the Repair identified by id
     * @throws NotFoundException if the Repair does not exist
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public Repair getRequest(@PathVariable("id") Long id) {
        access.info("[GET /api/repairs/{}]", id);
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withId(id);
        Repair repair = rService.find(criteria);
        if (repair == null) {
            throw new NotFoundException("Repair " + id + " does not exist");
        }
        return repair;
    }

    /**
     * Create a repair request for a given thing
     * <p>
     * todo: a way to validate that the files exist
     * todo: Check for active repairs
     *
     * @param principal the security principal of the user
     * @param request   the repair request to process
     * @return the newly created repair
     * @throws BadRequestException   if the requested bag does not exist
     * @throws UnauthorizedException if the user is not part of the node requesting the repair
     */
    @SuppressWarnings("ConstantConditions")
    @RequestMapping(method = RequestMethod.POST)
    public Repair createRequest(Principal principal, @RequestBody RepairCreate request) {
        access.info("[POST /api/repairs/] - ", principal.getName());
        access.info("POST parameters - {};{}", request.getCollection(), request.getDepositor());
        boolean ignore = true;
        boolean admin = hasRoleAdmin();
        boolean sameNode = request.getTo() == null
                ? ignore
                : request.getTo().equalsIgnoreCase(principal.getName());

        if (!admin && !sameNode) {
            throw new UnauthorizedException("User is not authorized");
        }

        // todo: clean up a bit
        // Get the node using the request or the principal (fallback)
        Node node = nodes.findByUsername(request.getTo() == null
                ? principal.getName()
                : request.getTo());
        check(node, "To node must exist");

        // Get the bag
        // This can really be lazified since all we need is the bag id
        // todo: validate that depositor and collection exist in the request
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withDepositor(request.getDepositor())
                .withName(request.getCollection());
        Bag b = bService.find(criteria);
        check(b, "Bag must exist");
        log.info("Creating repair request from user {} for bag {}", principal.getName(), b.getName());

        // Create the repair object
        Repair r = new Repair(b, node, null, // from_node -> null at first
                RepairStatus.REQUESTED, AuditStatus.PRE,
                null, null,  // vars set by from_node
                principal.getName(),
                false, false, false);
        r.setFiles(new HashSet<>());
        r.addFilesFromRequest(request.getFiles());
        rService.save(r);

        return r;
    }

    /**
     * Offer to fulfill a repair request
     * <p>
     * todo: create lock for creation of fulfillment on a repair?
     * maybe an atomic reference would be better... guava might have something actually
     *
     * @param principal the security principal of the fulfilling user
     * @param id        the id of the repair request
     * @return the newly general fulfillment
     * @throws BadRequestException   if the request does not exist or if a user tries to fulfill their own request
     * @throws UnauthorizedException if the principal is not associated with a node
     */
    @RequestMapping(value = "/{id}/fulfill", method = RequestMethod.POST)
    public Repair fulfillRequest(Principal principal, @PathVariable("id") Long id) {
        access.info("[POST /api/repairs/{}/fulfill] - {}", id, principal.getName());
        Node from = nodes.findByUsername(principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withId(id);
        Repair repair = rService.find(criteria);

        // Constraints
        check(repair, "Repair request must exist");
        check(from, "Admin users are not supported for fulfilling at this time");
        if (repair.getTo().getUsername().equals(principal.getName())) {
            throw new BadRequestException("Cannot fulfill your own repair request");
        }
        if (repair.getFrom() != null) {
            throw new ConflictException("Request is already being fulfilled");
        }

        // Create the fulfillment
        log.info("{} is fulfilling the repair request {}", from.getUsername(), repair.getId());
        repair.setStatus(RepairStatus.STAGING);
        repair.setFrom(from);
        rService.save(repair);
        return repair;
    }

    /**
     * Update a repair with the information for downloading files provided by
     * the fulfilling node
     *
     * @param principal the security principal of the user
     * @param strategy  the fulfillment strategy to be used
     * @param id        the id of the fulfillment to update
     * @return the updated fulfillment
     * @throws BadRequestException   if the fulfillment does not exist
     * @throws UnauthorizedException if the user is not authorized to ready the fulfillment
     */
    @RequestMapping(value = "/{id}/ready", method = RequestMethod.PUT)
    public Repair readyFulfillment(Principal principal,
                                   @RequestBody FulfillmentStrategy strategy,
                                   @PathVariable("id") Long id) {
        access.info("[PUT /api/repairs/{}/ready] - {}", id, principal.getName());
        access.info("PUT parameters - {}", strategy.getType());
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withId(id);
        Repair repair = rService.find(criteria);

        // Move constraint logic somewhere else?
        check(repair, "Repair does not exist");

        // Validate access
        // Do we want to do the below or nest ifs?
        // boolean authorized = !hasRoleAdmin() && principal.getName().equals(fulfillment.getFrom().getUsername());
        if (!hasRoleAdmin()) {
            boolean authorized = principal.getName().equals(repair.getFrom().getUsername());
            if (!authorized) {
                throw new UnauthorizedException(principal.getName() + " is not the fulfilling node");
            }
        }


        Strategy entity = ExtensionsKt.toEntity(strategy);
        entity.setRepair(repair);
        log.info("Adding strategy of type {} to repair {}", strategy.getType(), repair.getId());
        repair.setType(strategy.getType());
        repair.setStrategy(entity);
        repair.setStatus(RepairStatus.READY);
        rService.save(repair);
        return repair;
    }

    /**
     * Mark that a repair has been completed
     *
     * @param principal the security principal of the user
     * @param id        the id of the fulfillment
     * @return the updated fulfillment
     * @throws BadRequestException   if the fulfillment does not exist
     * @throws UnauthorizedException if the user is not authorized to complete the fulfillment
     */
    @RequestMapping(value = "/{id}/complete", method = RequestMethod.PUT)
    public Repair completeFulfillment(Principal principal, @PathVariable("id") Long id) {
        access.info("[PUT /api/repairs/{}/complete] - {}", id, principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withId(id);
        Repair repair = rService.find(criteria);
        check(repair, "Repair does not exist");
        check(repair.getStrategy(), "Repair must have a strategy before being completed!");

        if (!hasRoleAdmin()) {
            boolean authorized = principal.getName().equals(repair.getTo().getUsername());
            if (!authorized) {
                throw new UnauthorizedException(principal.getName() + " is not the requesting node");
            }
        }

        // TODO: Constraint satisfaction: the fulfillment should be validated before being set to complete
        log.info("Completing repair {} for node {}", repair.getId(), principal.getName());
        repair.setStatus(RepairStatus.REPAIRED);
        rService.save(repair);
        return repair;
    }

    /**
     * This is a placeholder atm, might need to update the request body
     * <p>
     * TODO: Should this trigger the completion of a fulfillment + repair if the audit succeeded?
     *
     * @param principal the principal of the authenticated user
     * @param id        the id of the repair
     * @param status    the status to update to
     * @return the updated repair
     */
    @RequestMapping(path = "/{id}/audit", method = RequestMethod.PUT)
    public Repair repairAuditing(Principal principal,
                                 @PathVariable("id") Long id,
                                 @RequestBody AuditStatus status) {
        access.info("[PUT /api/repairs/{}/audit] - {}", id, principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria().withId(id);
        Repair repair = rService.find(criteria);
        checkNotFound(repair, "Repair does not exist!");

        if (!hasRoleAdmin() && !principal.getName().equals(repair.getTo().getUsername())) {
            throw new UnauthorizedException(principal.getName() + " is not the requesting node");
        }

        repair.setAudit(status);
        rService.save(repair);
        return repair;
    }

    /**
     * Note that a repair backup has been cleaned
     *
     * @param principal the principal of the authenticated user
     * @param id        the id of the repair
     * @return the updated repair
     */
    @RequestMapping(path = "/{id}/cleaned", method = RequestMethod.PUT)
    public Repair repairCleaned(Principal principal, @PathVariable("id") Long id) {
        access.info("[PUT /api/repairs/{}/cleaned] - {}", id, principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria().withId(id);
        Repair repair = rService.find(criteria);
        checkNotFound(repair, "Repair does not exist");

        if (!hasRoleAdmin() && !principal.getName().equals(repair.getTo().getUsername())) {
            throw new UnauthorizedException(principal.getName() + " is not the requesting node");
        }

        repair.setCleaned(true);
        rService.save(repair);
        return repair;
    }

    /**
     * Note that a repair has been backed up
     *
     * @param principal the principal of the authenticated user
     * @param id        the id of the repair
     * @return the updated repair
     */
    @RequestMapping(path = "/{id}/replaced", method = RequestMethod.PUT)
    public Repair repairReplaced(Principal principal, @PathVariable("id") Long id) {
        access.info("[PUT /api/repairs/{}/replaced] - {}", id, principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria().withId(id);
        Repair repair = rService.find(criteria);
        checkNotFound(repair, "Repair does not exist");

        if (!hasRoleAdmin() && !principal.getName().equals(repair.getTo().getUsername())) {
            throw new UnauthorizedException(principal.getName() + " is not the requesting node");
        }

        repair.setReplaced(true);
        rService.save(repair);
        return repair;
    }

    /*
     * Note that a fulfillment has been cleaned from its staging area
     * TODO: Find a way to denote cleaning of the staged content
     *
     * @param principal the principal of the authenticated user
     * @param id the id of the fulfillment
     * @return the updated fulfillment
    @RequestMapping(path = "/{id}/cleaned", method = RequestMethod.PUT)
    public Fulfillment fulfillmentCleaned(Principal principal, @PathVariable("id") Long id) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria().withId(id);
        Fulfillment fulfillment = fService.find(criteria);
        checkNotFound(fulfillment, "Fulfillment does not exist");

        if (!hasRoleAdmin() && !principal.getName().equals(fulfillment.getFrom().getUsername())) {
            throw new UnauthorizedException(principal.getName() + " is not the fulfilling node");
        }

        fulfillment.setCleaned(true);
        fService.save(fulfillment);
        return fulfillment;
    }
     */

    /**
     * Note that a fulfillment has been cleaned from its staging area
     * TODO: Not sure about this...
     *
     * @param principal the principal of the authenticated user
     * @param id        the id of the fulfillment
     * @return the updated fulfillment
     */
    @RequestMapping(path = "/{id}/status", method = RequestMethod.PUT)
    public Repair fulfillmentUpdated(Principal principal,
                                     @PathVariable("id") Long id,
                                     @RequestBody RepairStatus status) {
        access.info("[PUT /api/repairs/{}/status] - {}", id, principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria().withId(id);
        Repair repair = rService.find(criteria);
        checkNotFound(repair, "Repair does not exist");

        if (!hasRoleAdmin()) {
            Node from = repair.getFrom();
            Node to = repair.getTo();
            if (status == RepairStatus.TRANSFERRED && !principal.getName().equals(to.getUsername())) {
                throw new UnauthorizedException(principal.getName() + " is not the repairing node");
            } else if (status != RepairStatus.TRANSFERRED && !principal.getName().equals(from.getUsername())) {
                throw new UnauthorizedException(principal.getName() + " is not the fulfilling node");
            }
        }

        repair.setStatus(status);
        rService.save(repair);
        return repair;
    }

    /**
     * Mark a fulfillment as validated according to the repairing node
     *
     * @param principal the security principal of the node validating
     * @param id        the id of the fulfillment
     * @return the updated fulfillment
     */
    @RequestMapping(path = "/{id}/validated", method = RequestMethod.PUT)
    public Repair fulfillmentValidated(Principal principal, @PathVariable("id") Long id) {
        access.info("[PUT /api/repairs/{}/validated] - {}", id, principal.getName());

        RepairSearchCriteria criteria = new RepairSearchCriteria().withId(id);
        Repair repair = rService.find(criteria);
        checkNotFound(repair, "Repair does not exist");
        Node to = repair.getTo();

        if (!hasRoleAdmin() && !principal.getName().equals(to.getUsername())) {
            throw new UnauthorizedException(principal.getName() + " is not the repairing node");
        }

        repair.setValidated(true);
        rService.save(repair);
        return repair;
    }


    /**
     * Check a variable t to ensure it is not null, and if so throw a
     * BadRequestException
     *
     * @param t       the t to check
     * @param message the message to include with the message
     * @param <T>     the type
     */
    private <T> void check(T t, String message) {
        if (t == null) {
            throw new BadRequestException(message);
        }
    }


    /**
     * Check a variable t to ensure it is not null, and if so throw a
     * NotFoundException
     *
     * @param t       the t to check
     * @param message the message to include
     * @param <T>     the type
     */
    private <T> void checkNotFound(T t, String message) {
        if (t == null) {
            throw new NotFoundException(message);
        }
    }

}
