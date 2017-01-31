package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.ingest.exception.BadRequestException;
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
import org.chronopolis.rest.entities.RepairFile;
import org.chronopolis.rest.entities.fulfillment.Strategy;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.FulfillmentStrategy;
import org.chronopolis.rest.models.repair.RepairRequest;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @RequestMapping(value = "/requests", method = RequestMethod.GET)
    public Iterable<Repair> getRequests() {
        RepairSearchCriteria criteria = new RepairSearchCriteria();
        return rService.findAll(criteria, new PageRequest(0, 20));
    }

    @RequestMapping(value = "/requests/{id}", method = RequestMethod.GET)
    public Repair getRequest(@PathVariable("id") Long id) {
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withId(id);
        return rService.find(criteria);
    }

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
        // TODO: Check for active repairs
        // TODO: This can really be lazified since all we need is the bag id
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

    private RepairFile toRepairFile(String path, Repair repair) {
        log.info("Creating new repair file {}", path);
        RepairFile r = new RepairFile();
        r.setPath(path);
        r.setRepair(repair);
        return r;
    }

    @RequestMapping(value = "/requests/{id}/fulfill", method = RequestMethod.POST)
    public Fulfillment fulfillRequest(Principal principal, @PathVariable("id") Long id) {
        // todo: contraints (node exists, repair exists)
        // todo: create lock for creation of fulfillment on a repair?
        Node n = nodes.findByUsername(principal.getName());
        RepairSearchCriteria criteria = new RepairSearchCriteria()
                .withId(id);
        Repair repair = rService.find(criteria);
        repair.setStatus(RepairStatus.FULFILLING);

        Fulfillment f = new Fulfillment();
        f.setRepair(repair)
         .setStatus(FulfillmentStatus.STAGING)
         .setFrom(n);
        repair.setFulfillment(f);

        log.info("Fulfilling request for {}", repair.getTo().getUsername());
        fService.save(f);
        return f;
    }

    @RequestMapping(value = "/fulfillments", method = RequestMethod.GET)
    public Page<Fulfillment> getFulfillments(@RequestParam Map<String, String> params) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria();
        return fService.findAll(criteria, createPageRequest(params, ImmutableMap.of()));
    }

    @RequestMapping(value = "/fulfillments/{id}", method = RequestMethod.GET)
    public Fulfillment getFulfillment(@PathVariable("id") Long id) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria()
                .withId(id);
        return fService.find(criteria);
    }

    @RequestMapping(value = "/fulfillments/{id}/ready", method = RequestMethod.PUT)
    public Fulfillment readyFulfillment(Principal principal, @RequestBody FulfillmentStrategy strategy, @PathVariable("id") Long id) {
        // todo: constraints
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria()
                .withId(id);
        Fulfillment fulfillment = fService.find(criteria);

        Strategy entity = strategy.createEntity(fulfillment);
        fulfillment.setStrategy(entity);
        fulfillment.setStatus(FulfillmentStatus.READY);
        fService.save(fulfillment);
        return fulfillment;
    }

    @RequestMapping(value = "/fulfillments/{id}/complete", method = RequestMethod.PUT)
    public Fulfillment completeFulfillment(Principal principal, @PathVariable("id") Long id) {
        FulfillmentSearchCriteria criteria = new FulfillmentSearchCriteria()
                .withId(id);
        Fulfillment fulfillment = fService.find(criteria);
        Repair repair = fulfillment.getRepair();

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
