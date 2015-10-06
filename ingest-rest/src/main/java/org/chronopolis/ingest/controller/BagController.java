package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.models.BagUpdate;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;

import static org.chronopolis.ingest.BagInitializer.initializeBag;

/**
 * Controller for handling bag/replication related requests
 *
 * Created by shake on 4/17/15.
 */
@Controller
public class BagController extends IngestController {
    private final Logger log = LoggerFactory.getLogger(BagController.class);

    @Autowired
    BagRepository bagRepository;

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    IngestSettings settings;

    /**
     * Retrieve information about all bags
     *
     * TODO: Pagination so we don't return a massive list of bags all at once
     *
     * @param model - the view model
     * @param principal - authentication information
     * @return
     */
    @RequestMapping(value= "/bags", method = RequestMethod.GET)
    public String getBags(Model model, Principal principal) {
        log.info("Getting bags for user {}", principal.getName());

        Collection<Bag> bags = bagRepository.findAll();
        model.addAttribute("bags", bags);

        return "bags";
    }

    /**
     * Get information about a single bag
     *
     * @param model - the view model
     * @param id - the id of the bag
     * @return
     */
    @RequestMapping(value = "/bags/{id}", method = RequestMethod.GET)
    public String getBag(Model model, @PathVariable("id") Long id) {
        log.info("Getting bag {}", id);

        // TODO: Could probably use model.addAllAttributes and use that for
        // common pages
        model.addAttribute("bags", bagRepository.findOne(id));
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));
        model.addAttribute("tokens", tokenRepository.countByBagId(id));

        return "bag";
    }

    /**
     * Handler for updating a bag
     *
     * @param model - the viewmodel
     * @param id - id of the bag to update
     * @param update - the updated information
     * @return
     */
    @RequestMapping(value = "/bags/{id}", method = RequestMethod.POST)
    public String updateBag(Model model, @PathVariable("id") Long id, BagUpdate update) {
        log.info("Updating bag {}: status = {}", id, update.getStatus());

        Bag bag = bagRepository.findOne(id);
        bag.setStatus(update.getStatus());
        bagRepository.save(bag);

        model.addAttribute("bags", bag);
        model.addAttribute("statuses", Arrays.asList(BagStatus.values()));
        model.addAttribute("tokens", tokenRepository.countByBagId(id));

        return "bag";
    }

    /**
     * Retrieve the page for adding bags
     *
     * @param model - the view model
     * @return
     */
    @RequestMapping(value = "/bags/add", method = RequestMethod.GET)
    public String addBag(Model model) {
        model.addAttribute("nodes", nodeRepository.findAll());
        return "addbag";
    }

    /**
     * Handler for adding bags
     *
     * @param model - the view model
     * @param request - the request containing the bag name, depositor, and location
     * @return
     * @throws IOException
     */
    @RequestMapping(value = "/bags/add", method = RequestMethod.POST)
    public String addBag(Model model, IngestRequest request) throws IOException {
        log.info("Adding new bag");
        String name = request.getName();
        String depositor = request.getDepositor();
        Bag bag = bagRepository.findByNameAndDepositor(name, depositor);
        request.setRequiredReplications(request.getReplicatingNodes().size());

        // only add new bags
        if (bag == null) {
            bag = new Bag(name, depositor);
            try {
                initializeBag(bag, request);
            } catch (IOException e) {
                log.error("Error creating bag", e);
                throw e;
            }
        }
        bagRepository.save(bag);

        // TODO: Redirect to /bags/{id}?
        return "redirect:/bags";
    }

    /**
     * Get all replications
     * If admin, return a list of all replications
     * else return a list for the given user
     *
     * @param model - the viewmodel
     * @param principal - authentication information
     * @return
     */
    @RequestMapping(value = "/replications", method = RequestMethod.GET)
    public String getReplications(Model model, Principal principal) {
        log.info("Getting replications for user {}", principal.getName());
        Collection<Replication> replications;
        if (hasRoleAdmin()) {
            replications = replicationRepository.findAll();
        } else {
            replications = replicationRepository.findByNodeUsername(principal.getName());
        }

        model.addAttribute("replications", replications);
        return "replications";
    }

}
