package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.exception.UnauthorizedException;
import org.chronopolis.ingest.repository.BagSearchCriteria;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.ingest.repository.FulfillmentRepository;
import org.chronopolis.ingest.repository.FulfillmentSearchCriteria;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.RepairSearchCriteria;
import org.chronopolis.ingest.repository.RepairService;
import org.chronopolis.ingest.repository.SearchService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Fulfillment;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.entities.fulfillment.Strategy;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.FulfillmentStrategy;
import org.chronopolis.rest.models.repair.RepairRequest;
import org.chronopolis.rest.models.repair.RepairStatus;
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
import java.util.Map;

import static org.chronopolis.ingest.IngestController.createPageRequest;
import static org.chronopolis.ingest.IngestController.hasRoleAdmin;

/**
 *
 * Created by shake on 1/24/17.
 */
@RestController
@RequestMapping("/api/repair")
public class RepairController {
    private final Logger log = LoggerFactory.getLogger(RepairController.class);

    private final BagService bService;
    private final NodeRepository nodes;
    private final RepairService<Repair, Long, RepairRepository> rService;
    private final SearchService<Fulfillment, Long, FulfillmentRepository> fService;

    @Autowired
    public RepairController(BagService bService,
                            NodeRepository nodes,
                            RepairService<Repair, Long, RepairRepository> rService,
                            SearchService<Fulfillment, Long, FulfillmentRepository> fService) {
        this.bService = bService;
        this.nodes = nodes;
        this.rService = rService;
        this.fService = fService;
    }

    /**
     * Return all Repairs
     *
     * @param params a map of query parameters to search on
     * @return all the Repairs requested
     */
    @RequestMapping(value = "/requests", method = RequestMethod.GET)
    public Page<Repair> getRequests(@RequestParam Map<String, String> params) {
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withTo(params.getOrDefault(Params.TO, null))
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
    @RequestMapping(value = "/requests/{id}", method = RequestMethod.GET)
    public Repair getRequest(@PathVariable("id") Long id) {
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
     *
     * todo: a way to validate that the files exist
     * todo: Check for active repairs
     *
     * @param principal the security principal of the user
     * @param request the repair request to process
     * @return the newly created repair
     * @throws BadRequestException if the requested bag does not exist
     * @throws UnauthorizedException if the user is not part of the node requesting the repair
     */
    @SuppressWarnings("ConstantConditions")
    @RequestMapping(value = "/requests", method = RequestMethod.POST)
    public Repair createRequest(Principal principal, @RequestBody RepairRequest request) {
        boolean ignore = true;
        boolean admin = hasRoleAdmin();
        boolean sameNode = request.getTo()
                .map(to -> to.equals(principal.getName()))
                .orElse(ignore);

        if (!admin && !sameNode) {
            throw new UnauthorizedException("User is not authorized");
        }

        // Get the node using the request or the principal (fallback)
        Node node = nodes.findByUsername(request.getTo().orElse(principal.getName()));
        check(node, "To node must exist");

        // Get the bag
        // This can really be lazified since all we need is the bag id
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withDepositor(request.getDepositor())
                .withName(request.getCollection());
        Bag b = bService.findBag(criteria);
        check(b, "Bag must exist");

        // Create the repair object
        Repair r = new Repair()
                .setBag(b)
                .setTo(node)
                .setRequester(principal.getName())
                .setStatus(RepairStatus.REQUESTED)
                .setFilesFromRequest(request.getFiles());
        rService.save(r);

        return r;
    }

    /**
     * Offer to fulfill a repair request
     *
     * todo: create lock for creation of fulfillment on a repair?
     *       maybe an atomic reference would be better... guava might have something actually
     *
     * @param principal the security principal of the fulfilling user
     * @param id the id of the repair request
     * @return the newly general fulfillment
     * @throws BadRequestException if the request does not exist or if a user tries to fulfill their own request
     * @throws UnauthorizedException if the principal is not associated with a node
     */
    @RequestMapping(value = "/requests/{id}/fulfill", method = RequestMethod.POST)
    public Fulfillment fulfillRequest(Principal principal, @PathVariable("id") Long id) {
        Node n = nodes.findByUsername(principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withId(id);
        Repair repair = rService.find(criteria);
        repair.setStatus(RepairStatus.FULFILLING);

        // Constraints
        check(repair, "Repair request must exist");
        check(n, "Admin users are not supported for fulfilling at this time");
        if (repair.getTo().getUsername().equals(principal.getName())) {
            throw new BadRequestException("Cannot fulfill your own repair request");
        }

        // Create the fulfillment
        Fulfillment f = new Fulfillment();
        f.setRepair(repair)
         .setStatus(FulfillmentStatus.STAGING)
         .setFrom(n);
        repair.setFulfillment(f);

        log.info("Fulfilling request for {}", repair.getTo().getUsername());
        rService.save(repair);
        return f;
    }

    /**
     * Return all fulfillments
     *
     * @param params the query parameters
     * @return the list of fulfillments
     */
    @RequestMapping(value = "/fulfillments", method = RequestMethod.GET)
    public Page<Fulfillment> getFulfillments(@RequestParam Map<String, String> params) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria();
        return fService.findAll(criteria, createPageRequest(params, ImmutableMap.of()));
    }

    /**
     * Return a single fulfillment
     *
     * @param id the id of the fulfillment to return
     * @return the fulfillment identified by id
     * @throws NotFoundException if the fulfillment does not exist
     */
    @RequestMapping(value = "/fulfillments/{id}", method = RequestMethod.GET)
    public Fulfillment getFulfillment(@PathVariable("id") Long id) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria()
                .withId(id);
        Fulfillment fulfillment = fService.find(criteria);
        if (fulfillment == null) {
            throw new NotFoundException("Fulfillment " + id + " does not exist");
        }
        return fulfillment;
    }

    /**
     * Update a fulfillment with the information necessary to be completed
     *
     * @param principal the security principal of the user
     * @param strategy the fulfillment strategy to be used
     * @param id the id of the fulfillment to update
     * @return the updated fulfillment
     * @throws BadRequestException if the fulfillment does not exist
     * @throws UnauthorizedException if the user is not authorized to ready the fulfillment
     */
    @RequestMapping(value = "/fulfillments/{id}/ready", method = RequestMethod.PUT)
    public Fulfillment readyFulfillment(Principal principal, @RequestBody FulfillmentStrategy strategy, @PathVariable("id") Long id) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria()
                .withId(id);
        Fulfillment fulfillment = fService.find(criteria);

        // Move constraint logic somewhere else?
        check(fulfillment, "Fulfillment does not exist");

        // Validate access
        // Do we want to do the below or nest ifs?
        // boolean authorized = !hasRoleAdmin() && principal.getName().equals(fulfillment.getFrom().getUsername());
        if (!hasRoleAdmin()) {
            boolean authorized = principal.getName().equals(fulfillment.getFrom().getUsername());
            if (!authorized) {
                throw new UnauthorizedException(principal.getName() + " is not the fulfilling node");
            }
        }

        Strategy entity = strategy.createEntity(fulfillment);
        fulfillment.setType(strategy.getType());
        fulfillment.setStrategy(entity);
        fulfillment.setStatus(FulfillmentStatus.READY);
        fService.save(fulfillment);
        return fulfillment;
    }

    /**
     * Mark that a fulfillment has been completed
     *
     * @param principal the security principal of the user
     * @param id the id of the fulfillment
     * @return the updated fulfillment
     * @throws BadRequestException if the fulfillment does not exist
     * @throws UnauthorizedException if the user is not authorized to complete the fulfillment
     */
    @RequestMapping(value = "/fulfillments/{id}/complete", method = RequestMethod.PUT)
    public Fulfillment completeFulfillment(Principal principal, @PathVariable("id") Long id) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria()
                .withId(id);
        Fulfillment fulfillment = fService.find(criteria);
        check(fulfillment, "Fulfillment does not exist");
        check(fulfillment.getStrategy(), "Fulfillment must have a repair strategy before being completed!");

        Repair repair = fulfillment.getRepair();

        if (!hasRoleAdmin()) {
            boolean authorized = principal.getName().equals(repair.getTo().getUsername());
            if (!authorized) {
                throw new UnauthorizedException(principal.getName() + " is not the requesting node");
            }
        }

        fulfillment.setStatus(FulfillmentStatus.COMPLETE);
        repair.setStatus(RepairStatus.REPAIRED);
        fService.save(fulfillment);
        rService.save(repair);
        return fulfillment;
    }

    /**
     * Check a variable t to ensure it is not null, and if so throw a
     * BadRequestException
     *
     * @param t the t to listen
     * @param message the message to include with the message
     * @param <T> the type
     */
    private <T> void check(T t, String message) {
        if (t == null) {
            throw new BadRequestException(message);
        }
    }

}
