package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.BagSearchCriteria;
import org.chronopolis.ingest.repository.BagService;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.RepairSearchCriteria;
import org.chronopolis.ingest.repository.RepairService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.entities.RepairFile;
import org.chronopolis.rest.models.repair.RepairRequest;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 *
 * Created by shake on 1/24/17.
 */
@RestController
@RequestMapping("/api/repair")
public class RepairController {
    private final Logger log = LoggerFactory.getLogger(RepairController.class);

    private final BagService bService;
    private final RepairService<Repair, Long, RepairRepository> rService;
    // SearchService<Fulfillment, Long, FulfillmentRepository> fService;

    @Autowired
    public RepairController(BagService bService, RepairService<Repair, Long, RepairRepository> rService) {
        this.bService = bService;
        this.rService = rService;
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

    @RequestMapping(value = "/requests", method = RequestMethod.POST)
    public Repair createRequest(Principal principal, @RequestBody RepairRequest request) {
        // Get the bag
        // TODO: Check for active repairs 
        // TODO: This can really be lazified since all we need is the bag id
        BagSearchCriteria criteria = new BagSearchCriteria()
                .withDepositor(request.getDepositor())
                .withName(request.getCollection());
        Bag b = bService.findBag(criteria);

        // Create the repair object
        Repair r = new Repair()
                .setBag(b)
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
    public void fulfillRequest() {
    }

}
