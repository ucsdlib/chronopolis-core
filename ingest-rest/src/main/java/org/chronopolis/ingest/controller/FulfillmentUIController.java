package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.PageWrapper;
import org.chronopolis.ingest.models.FulfillmentRequest;
import org.chronopolis.ingest.repository.FulfillmentRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.criteria.FulfillmentSearchCriteria;
import org.chronopolis.ingest.repository.criteria.RepairSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Fulfillment;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handle Fulfillment UI ops
 *
 * Created by shake on 4/25/17.
 */
@Controller
public class FulfillmentUIController extends IngestController {
    private final Logger log = LoggerFactory.getLogger(RepairUIController.class);

    private final NodeRepository nodes;
    private final SearchService<Repair, Long, RepairRepository> repairs;
    private final SearchService<Fulfillment, Long, FulfillmentRepository> fulfillments;

    @Autowired
    public FulfillmentUIController(NodeRepository nodes,
                                   SearchService<Repair, Long, RepairRepository> repairs,
                                   SearchService<Fulfillment, Long, FulfillmentRepository> fulfillments) {
        this.repairs = repairs;
        this.nodes = nodes;
        this.fulfillments = fulfillments;
    }

    @RequestMapping(path = "/fulfillments", method = RequestMethod.POST)
    public String createFulfillment(Model model, Principal principal, FulfillmentRequest request) {
        log.info("{} offering to fulfill {}", request.getFrom(), request.getRepair());
        Repair repair = repairs.find(new RepairSearchCriteria().withId(request.getRepair()));

        // todo ensure we can repair
        Fulfillment fulfillment = new Fulfillment();
        fulfillment.setRepair(repair);
        fulfillment.setFrom(nodes.findByUsername(request.getFrom()));
        fulfillment.setStatus(FulfillmentStatus.STAGING);

        repair.setFulfillment(fulfillment);
        repairs.save(repair);

        // The repair holds the updated fulfillment??
        return "redirect:/fulfillments/" + repair.getFulfillment().getId();
    }

    // todo: query params
    @RequestMapping(path = "/fulfillments", method = RequestMethod.GET)
    public String getFulfillments(Model model) {
        Page<Fulfillment> all = fulfillments.findAll(new FulfillmentSearchCriteria(),
                createPageRequest(new HashMap<>(), new HashMap<>()));

        PageWrapper<Fulfillment> pages = new PageWrapper<>(all, "/fulfillments");
        model.addAttribute("pages", pages);
        model.addAttribute("fulfillments", all);
        return "fulfillment/list";
    }

    @RequestMapping(path = "/fulfillments/{id}", method = RequestMethod.GET)
    public String getFulfillment(Model model, @PathVariable("id") Long id) {
        Fulfillment fulfillment = fulfillments.find(new FulfillmentSearchCriteria().withId(id));
        model.addAttribute("fulfillment", fulfillment);
        return "fulfillment/fulfillment";
    }

    @RequestMapping(path = "/fulfillments/add", method = RequestMethod.GET)
    public String fulfillRepair(Model model, Principal principal) {
        // todo: either to withFromNot principal.getName
        //       or disable all radios w/ the selected from in the page
        Page<Repair> availableRepairs = repairs.findAll(new RepairSearchCriteria()
                        .withStatus(RepairStatus.REQUESTED),
                createPageRequest(new HashMap<>(), new HashMap<>()));
        List<Node> from;
        if (hasRoleAdmin()) {
            from = nodes.findAll();
        } else {
            from = new ArrayList<>();
            from.add(nodes.findByUsername(principal.getName()));
        }

        model.addAttribute("repairs", availableRepairs);
        model.addAttribute("from", from);
        return "fulfillment/add";
    }
}
